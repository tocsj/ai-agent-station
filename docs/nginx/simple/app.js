const form = document.getElementById('agentForm');
const sendBtn = document.getElementById('sendBtn');
const stopBtn = document.getElementById('stopBtn');
const statusBadge = document.getElementById('statusBadge');
const apiBaseInput = document.getElementById('apiBase');
const agentIdInput = document.getElementById('agentId');
const sessionInput = document.getElementById('sessionId');
const maxStepInput = document.getElementById('maxStep');
const messageInput = document.getElementById('message');
const regenerateBtn = document.getElementById('regenerateSession');
const eventsContainer = document.getElementById('events');
const conversationLog = document.getElementById('conversationLog');
const eventTemplate = document.getElementById('eventTemplate');
const summaryContent = document.getElementById('summaryContent');
const copySummaryBtn = document.getElementById('copySummary');
const rawLog = document.getElementById('rawLog');
const clearRawBtn = document.getElementById('clearRaw');
const clearConversationBtn = document.getElementById('clearConversation');
const eventCount = document.getElementById('eventCount');

const STORAGE_KEY = 'simple-agent-console';
let controller = null;
let streaming = false;
let received = 0;

function loadSettings() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (!saved) return;
    const data = JSON.parse(saved);
    if (data.apiBase) apiBaseInput.value = data.apiBase;
    if (data.agentId) agentIdInput.value = data.agentId;
    if (typeof data.maxStep === 'number') maxStepInput.value = data.maxStep;
  } catch {
    // ignore parse errors
  }
}

function saveSettings() {
  const payload = {
    apiBase: apiBaseInput.value.trim(),
    agentId: agentIdInput.value.trim(),
    maxStep: Number(maxStepInput.value) || 3
  };
  localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
}

function generateSessionId() {
  return `session-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`;
}

function setStatus(text, active = false) {
  statusBadge.textContent = text;
  statusBadge.classList.toggle('status--active', active);
}

function appendMessage(role, content) {
  const wrapper = document.createElement('div');
  wrapper.className = `message message--${role}`;
  const title = document.createElement('h4');
  title.textContent = role === 'user' ? '用户' : 'Agent';
  const body = document.createElement('p');
  body.innerText = content || '';
  wrapper.appendChild(title);
  wrapper.appendChild(body);
  conversationLog.appendChild(wrapper);
  conversationLog.scrollTop = conversationLog.scrollHeight;
}

function handleStepEvent(step) {
  addEventCard(step);
  if (step.type === 'summary' && step.content) {
    appendMessage('assistant', step.content);
    summaryContent.textContent = step.content;
  }
  if (step.type === 'error') {
    appendMessage('assistant', step.content || '服务器返回错误');
    setStatus('发生错误', false);
  }
  if (step.type === 'complete') {
    finishStream('服务端完成');
  }
}

function addEventCard(step) {
  const fragment = eventTemplate.content.cloneNode(true);
  const article = fragment.querySelector('.event');
  const typeEl = fragment.querySelector('.event__type');
  const metaEl = fragment.querySelector('.event__meta');
  const contentEl = fragment.querySelector('.event__content');
  const footerEl = fragment.querySelector('.event__footer');

  typeEl.textContent = [step.type, step.subType].filter(Boolean).join(' / ') || '事件';
  metaEl.textContent = step.step != null ? `第 ${step.step} 步` : '';
  contentEl.textContent = step.content || '(无内容)';
  const ts = step.timestamp ? new Date(step.timestamp).toLocaleString() : new Date().toLocaleString();
  footerEl.textContent = `${ts}${step.sessionId ? ` · ${step.sessionId}` : ''}`;

  eventsContainer.prepend(fragment);
  received += 1;
  eventCount.textContent = `${received} 条`;
}

function logRawChunk(chunk) {
  rawLog.textContent = `${rawLog.textContent}${chunk}`;
  rawLog.scrollTop = rawLog.scrollHeight;
}

function toStepEvent(dataLine) {
  try {
    const payload = JSON.parse(dataLine);
    return {
      id: payload.id || `step-${crypto.randomUUID?.() ?? Date.now()}`,
      type: payload.type || 'analysis',
      subType: payload.subType || '',
      step: payload.step,
      content: payload.content,
      timestamp: payload.timestamp,
      sessionId: payload.sessionId
    };
  } catch {
    return {
      id: `step-${Date.now()}`,
      type: 'info',
      subType: '',
      content: dataLine,
      timestamp: Date.now()
    };
  }
}

function parseSse(buffer) {
  const events = [];
  let idx;
  while ((idx = buffer.indexOf('\n\n')) !== -1) {
    events.push(buffer.slice(0, idx));
    buffer = buffer.slice(idx + 2);
  }
  return { events, rest: buffer };
}

async function startStream(payload, endpoint) {
  controller = new AbortController();
  setStatus('流式处理中', true);
  streaming = true;
  sendBtn.disabled = true;
  stopBtn.disabled = false;

  try {
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload),
      signal: controller.signal
    });

    if (!response.ok) {
      const text = await response.text();
      throw new Error(text || `HTTP ${response.status}`);
    }

    if (!response.body) {
      throw new Error('后端未返回流数据');
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      const chunk = decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n');
      logRawChunk(chunk);
      buffer += chunk;
      const { events, rest } = parseSse(buffer);
      buffer = rest;
      for (const eventBlock of events) {
        const lines = eventBlock.split('\n');
        const dataLines = lines
          .filter((line) => line.startsWith('data:'))
          .map((line) => line.slice(5).trim())
          .filter(Boolean);
        if (dataLines.length === 0) {
          const fallback = eventBlock.trim();
          if (fallback) {
            handleStepEvent(toStepEvent(fallback));
          }
          continue;
        }
        for (const dataLine of dataLines) {
          handleStepEvent(toStepEvent(dataLine));
        }
      }
    }

    finishStream('已完成');
  } catch (error) {
    if (controller?.signal.aborted) {
      finishStream('已手动停止');
      return;
    }
    const message = error instanceof Error ? error.message : '网络异常';
    appendMessage('assistant', `请求失败: ${message}`);
    setStatus('失败', false);
    finishStream('失败');
  }
}

function finishStream(finalText) {
  streaming = false;
  sendBtn.disabled = false;
  stopBtn.disabled = true;
  controller = null;
  if (finalText) {
    setStatus(finalText, false);
  }
}

function buildEndpoint(base) {
  if (!base) return '/api/v1/agent/auto_agent';
  const normalized = base.endsWith('/') ? base.slice(0, -1) : base;
  return `${normalized}/api/v1/agent/auto_agent`;
}

form.addEventListener('submit', (event) => {
  event.preventDefault();
  if (streaming) return;
  const agentId = agentIdInput.value.trim();
  const message = messageInput.value.trim();
  if (!agentId || !message) {
    setStatus('请填写必要字段');
    return;
  }

  const payload = {
    aiAgentId: agentId,
    message,
    sessionId: sessionInput.value.trim() || generateSessionId(),
    maxStep: Number(maxStepInput.value) || 3
  };

  saveSettings();
  appendMessage('user', payload.message);
  setStatus('准备发送...', true);
  startStream(payload, buildEndpoint(apiBaseInput.value.trim()));
});

stopBtn.addEventListener('click', () => {
  if (controller) {
    controller.abort();
  }
});

regenerateBtn.addEventListener('click', () => {
  sessionInput.value = generateSessionId();
});

copySummaryBtn.addEventListener('click', async () => {
  const text = summaryContent.textContent?.trim();
  if (!text) return;
  try {
    await navigator.clipboard.writeText(text);
    copySummaryBtn.textContent = '已复制';
    setTimeout(() => {
      copySummaryBtn.textContent = '复制';
    }, 1200);
  } catch {
    // ignore
  }
});

clearRawBtn.addEventListener('click', () => {
  rawLog.textContent = '';
});

clearConversationBtn.addEventListener('click', () => {
  conversationLog.innerHTML = '';
});

window.addEventListener('beforeunload', () => {
  if (controller) {
    controller.abort();
  }
});

function init() {
  loadSettings();
  if (!sessionInput.value) {
    sessionInput.value = generateSessionId();
  }
  setStatus('空闲');
}

init();
