import type { AutoAgentEventPayload, StepEvent, StepType } from './types';

export function createId(prefix: string): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return `${prefix}-${crypto.randomUUID()}`;
  }
  return `${prefix}-${Math.random().toString(36).slice(2)}${Date.now().toString(36)}`;
}

export function humanTime(ts?: number | null): string {
  if (!ts) return '';
  const date = new Date(ts);
  return date.toLocaleTimeString();
}

export function normalizeStepType(value?: string): StepType {
  if (!value) return 'unknown';
  switch (value) {
    case 'analysis':
    case 'execution':
    case 'supervision':
    case 'summary':
    case 'error':
    case 'complete':
      return value;
    default:
      return 'unknown';
  }
}

export function toStepEvent(payload: AutoAgentEventPayload, fallbackContent?: string): StepEvent {
  return {
    id: createId('step'),
    type: normalizeStepType(payload.type),
    subType: payload.subType,
    step: payload.step ?? null,
    content: payload.content ?? fallbackContent ?? '',
    completed: payload.completed,
    timestamp: payload.timestamp ?? null,
    sessionId: payload.sessionId ?? null
  };
}

export function groupLabel(type: StepType, subType?: string): string {
  const main = type.toUpperCase();
  if (!subType) return main;
  return `${main} / ${subType}`;
}

export function safeJsonParse(text: string): AutoAgentEventPayload | null {
  try {
    return JSON.parse(text) as AutoAgentEventPayload;
  } catch {
    return null;
  }
}

export function parseSseChunks(buffer: string): { events: string[]; rest: string } {
  const events: string[] = [];
  const parts = buffer.split('\n\n');
  if (parts.length === 1) {
    return { events, rest: buffer };
  }
  for (let i = 0; i < parts.length - 1; i += 1) {
    events.push(parts[i]);
  }
  return { events, rest: parts[parts.length - 1] };
}

export function extractSseData(eventBlock: string): string[] {
  const lines = eventBlock.split('\n');
  const dataLines: string[] = [];
  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed.startsWith('data:')) {
      dataLines.push(trimmed.replace(/^data:\s*/, ''));
    }
  }
  return dataLines;
}
