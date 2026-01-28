import type { AutoAgentEventPayload, AutoAgentRequest, StepEvent } from './types';
import { createId, extractSseData, parseSseChunks, safeJsonParse, toStepEvent } from './utils';

export interface StreamHandlers {
  onStep: (step: StepEvent) => void;
  onSummary: (content: string, step: StepEvent) => void;
  onError: (message: string) => void;
  onComplete: () => void;
  onRawChunk?: (text: string) => void;
}

export interface StreamController {
  abort: () => void;
  id: string;
}

export async function streamAutoAgent(
  request: AutoAgentRequest,
  handlers: StreamHandlers,
  baseUrl = ''
): Promise<StreamController> {
  const controller = new AbortController();
  const streamId = createId('stream');

  const doFetch = async () => {
    const response = await fetch(`${baseUrl}/api/v1/agent/auto_agent`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(request),
      signal: controller.signal
    });

    if (!response.ok) {
      const text = await response.text();
      throw new Error(text || `HTTP ${response.status}`);
    }

    if (!response.body) {
      throw new Error('Empty response body');
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      const chunk = decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n');
      handlers.onRawChunk?.(chunk);
      buffer += chunk;

      const { events, rest } = parseSseChunks(buffer);
      buffer = rest;

      for (const eventBlock of events) {
        const dataLines = extractSseData(eventBlock);
        if (dataLines.length === 0) {
          const raw = eventBlock.trim();
          if (raw) {
            const fallbackStep = toStepEvent({}, raw);
            handlers.onStep(fallbackStep);
          }
          continue;
        }

        for (const dataLine of dataLines) {
          const payload = safeJsonParse(dataLine);
          if (payload) {
            const step = toStepEvent(payload);
            handlers.onStep(step);
            if (step.type === 'summary' && step.content) {
              handlers.onSummary(step.content, step);
            }
            if (step.type === 'error') {
              handlers.onError(step.content || 'Server error');
            }
            if (step.type === 'complete') {
              handlers.onComplete();
            }
          } else if (dataLine.trim()) {
            const fallbackStep = toStepEvent({}, dataLine.trim());
            handlers.onStep(fallbackStep);
          }
        }
      }
    }
  };

  let reconnects = 0;
  const maxReconnects = 1;

  const run = async () => {
    try {
      await doFetch();
      handlers.onComplete();
    } catch (error) {
      if (controller.signal.aborted) return;
      if (reconnects < maxReconnects) {
        reconnects += 1;
        const reconnectStep: StepEvent = {
          id: createId('step'),
          type: 'analysis',
          content: `Stream disconnected. Reconnecting (${reconnects}/${maxReconnects})...`
        };
        handlers.onStep(reconnectStep);
        await run();
        return;
      }
      const message = error instanceof Error ? error.message : 'Network error';
      handlers.onError(message);
      handlers.onComplete();
    }
  };

  run();

  return {
    abort: () => controller.abort(),
    id: streamId
  };
}
