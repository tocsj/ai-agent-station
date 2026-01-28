import { useEffect, useMemo, useRef, useState } from 'react';
import DOMPurify from 'dompurify';
import { marked } from 'marked';
import hljs from 'highlight.js';
import 'highlight.js/styles/atom-one-dark.css';
import { StreamController, streamAutoAgent } from './api';
import {
  AgentProfile,
  Conversation,
  Message
} from './types';
import { createId, groupLabel, humanTime } from './utils';
import {
  loadAgents,
  loadConversations,
  loadActiveConversationId,
  saveActiveConversationId,
  saveAgents,
  saveConversations
} from './storage';

const DEFAULT_AGENT: AgentProfile = {
  id: '1',
  label: 'Agent 1',
  maxStep: 3
};

const DEFAULT_MODEL_LABEL = 'Auto-Agent';

marked.setOptions({
  breaks: true
});

function MarkdownView({ content }: { content: string }) {
  const ref = useRef<HTMLDivElement | null>(null);
  const html = useMemo(() => {
    const raw = marked.parse(content || '');
    return DOMPurify.sanitize(raw as string);
  }, [content]);

  useEffect(() => {
    const container = ref.current;
    if (!container) return;
    const codeBlocks = container.querySelectorAll('pre code');
    codeBlocks.forEach((block) => {
      hljs.highlightElement(block as HTMLElement);
      const pre = block.parentElement;
      if (!pre) return;
      if (pre.querySelector('.copy-button')) return;
      const button = document.createElement('button');
      button.className = 'copy-button';
      button.type = 'button';
      button.textContent = 'Copy';
      button.addEventListener('click', () => {
        const text = (block as HTMLElement).innerText;
        navigator.clipboard.writeText(text).catch(() => undefined);
        button.textContent = 'Copied';
        setTimeout(() => {
          button.textContent = 'Copy';
        }, 1200);
      });
      pre.appendChild(button);
    });
  }, [html]);

  return (
    <div
      ref={ref}
      className="markdown"
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}

export default function App() {
  const apiBase = import.meta.env.VITE_API_BASE || '';
  const [conversations, setConversations] = useState<Conversation[]>(() => {
    const stored = loadConversations();
    if (stored.length > 0) return stored;
    const conversation: Conversation = {
      id: createId('session'),
      title: 'New chat',
      createdAt: Date.now(),
      updatedAt: Date.now(),
      agentId: DEFAULT_AGENT.id,
      maxStep: DEFAULT_AGENT.maxStep,
      messages: [],
      stepEvents: []
    };
    return [conversation];
  });
  const [activeId, setActiveId] = useState<string>(() => loadActiveConversationId() || '');
  const [agents, setAgents] = useState<AgentProfile[]>(() => {
    const stored = loadAgents();
    return stored.length ? stored : [DEFAULT_AGENT];
  });
  const [draft, setDraft] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);
  const [error, setError] = useState('');
  const [showProcess, setShowProcess] = useState(true);
  const [renamingId, setRenamingId] = useState<string | null>(null);
  const [editingAgentId, setEditingAgentId] = useState<string | null>(null);
  const streamRef = useRef<StreamController | null>(null);
  const endRef = useRef<HTMLDivElement | null>(null);

  const activeConversation = useMemo(() => {
    return conversations.find((item) => item.id === activeId) || conversations[0];
  }, [activeId, conversations]);

  useEffect(() => {
    if (!activeId && conversations.length) {
      setActiveId(conversations[0].id);
    }
  }, [activeId, conversations]);

  useEffect(() => {
    saveConversations(conversations);
  }, [conversations]);

  useEffect(() => {
    saveAgents(agents);
  }, [agents]);

  useEffect(() => {
    saveActiveConversationId(activeId || null);
  }, [activeId]);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [activeConversation?.messages.length, activeConversation?.stepEvents.length]);

  const updateConversation = (id: string, updater: (conv: Conversation) => Conversation) => {
    setConversations((prev) => prev.map((conv) => (conv.id === id ? updater(conv) : conv)));
  };

  const handleNewConversation = () => {
    const agent = agents[0] || DEFAULT_AGENT;
    const conversation: Conversation = {
      id: createId('session'),
      title: 'New chat',
      createdAt: Date.now(),
      updatedAt: Date.now(),
      agentId: agent.id,
      maxStep: agent.maxStep,
      messages: [],
      stepEvents: []
    };
    setConversations((prev) => [conversation, ...prev]);
    setActiveId(conversation.id);
  };

  const handleDeleteConversation = (id: string) => {
    setConversations((prev) => prev.filter((conv) => conv.id !== id));
    if (activeId === id) {
      const next = conversations.find((conv) => conv.id !== id);
      setActiveId(next?.id || '');
    }
  };

  const handleRenameConversation = (id: string, title: string) => {
    updateConversation(id, (conv) => ({ ...conv, title }));
  };

  const handleAgentChange = (agentId: string) => {
    const agent = agents.find((item) => item.id === agentId);
    if (!agent || !activeConversation) return;
    updateConversation(activeConversation.id, (conv) => ({
      ...conv,
      agentId: agent.id,
      maxStep: agent.maxStep,
      updatedAt: Date.now()
    }));
  };

  const handleMaxStepChange = (value: number) => {
    if (!activeConversation) return;
    updateConversation(activeConversation.id, (conv) => ({
      ...conv,
      maxStep: value,
      updatedAt: Date.now()
    }));
  };

  const handleSend = async (content?: string) => {
    const message = (content ?? draft).trim();
    if (!message || !activeConversation || isStreaming) return;
    setError('');
    setDraft('');

    const userMessage: Message = {
      id: createId('msg'),
      role: 'user',
      content: message,
      createdAt: Date.now()
    };
    const assistantMessage: Message = {
      id: createId('msg'),
      role: 'assistant',
      content: '',
      createdAt: Date.now(),
      steps: []
    };

    updateConversation(activeConversation.id, (conv) => ({
      ...conv,
      messages: [...conv.messages, userMessage, assistantMessage],
      updatedAt: Date.now()
    }));

    setIsStreaming(true);

    const controller = await streamAutoAgent(
      {
        aiAgentId: activeConversation.agentId,
        message,
        sessionId: activeConversation.id,
        maxStep: activeConversation.maxStep
      },
      {
        onStep: (step) => {
          updateConversation(activeConversation.id, (conv) => {
            const updatedMessages = conv.messages.map((msg) => {
              if (msg.id !== assistantMessage.id) return msg;
              const steps = msg.steps ? [...msg.steps, step] : [step];
              return { ...msg, steps };
            });
            const stepEvents = [...conv.stepEvents, step];
            return { ...conv, messages: updatedMessages, stepEvents, updatedAt: Date.now() };
          });
        },
        onSummary: (summary) => {
          updateConversation(activeConversation.id, (conv) => {
            const updatedMessages = conv.messages.map((msg) =>
              msg.id === assistantMessage.id ? { ...msg, content: summary } : msg
            );
            return { ...conv, messages: updatedMessages, updatedAt: Date.now() };
          });
        },
        onError: (message) => {
          setError(message);
          updateConversation(activeConversation.id, (conv) => {
            const systemMsg: Message = {
              id: createId('msg'),
              role: 'system',
              content: `Error: ${message}`,
              createdAt: Date.now()
            };
            return { ...conv, messages: [...conv.messages, systemMsg], updatedAt: Date.now() };
          });
        },
        onComplete: () => {
          setIsStreaming(false);
          streamRef.current = null;
        }
      },
      apiBase
    );

    streamRef.current = controller;
  };

  const handleStop = () => {
    if (!streamRef.current) return;
    streamRef.current.abort();
    setIsStreaming(false);
    updateConversation(activeConversation.id, (conv) => ({
      ...conv,
      messages: [
        ...conv.messages,
        {
          id: createId('msg'),
          role: 'system',
          content: 'Generation stopped by user.',
          createdAt: Date.now()
        }
      ],
      updatedAt: Date.now()
    }));
  };

  const handleRegenerate = () => {
    if (!activeConversation || isStreaming) return;
    const lastUser = [...activeConversation.messages].reverse().find((msg) => msg.role === 'user');
    if (!lastUser) return;
    handleSend(lastUser.content);
  };

  const handleAddAgent = () => {
    const newAgent: AgentProfile = {
      id: createId('agent').slice(-6),
      label: `Agent ${agents.length + 1}`,
      maxStep: 3
    };
    setAgents((prev) => [...prev, newAgent]);
    setEditingAgentId(newAgent.id);
  };

  const updateAgent = (id: string, patch: Partial<AgentProfile>) => {
    setAgents((prev) => prev.map((agent) => (agent.id === id ? { ...agent, ...patch } : agent)));
  };

  const removeAgent = (id: string) => {
    setAgents((prev) => {
      const next = prev.filter((agent) => agent.id !== id);
      if (activeConversation?.agentId === id) {
        const fallback = next[0] || DEFAULT_AGENT;
        updateConversation(activeConversation.id, (conv) => ({
          ...conv,
          agentId: fallback.id,
          maxStep: fallback.maxStep,
          updatedAt: Date.now()
        }));
      }
      return next.length ? next : [DEFAULT_AGENT];
    });
  };

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-title">{DEFAULT_MODEL_LABEL}</div>
          <div className="brand-sub">Chat-style Auto-Agent UI</div>
        </div>
        <button className="primary" onClick={handleNewConversation}>
          New chat
        </button>
        <div className="section-title">Conversations</div>
        <div className="conversation-list">
          {conversations.map((conv) => (
            <div
              key={conv.id}
              className={`conversation-item ${conv.id === activeId ? 'active' : ''}`}
            >
              <div className="conversation-main" onClick={() => setActiveId(conv.id)}>
                {renamingId === conv.id ? (
                  <input
                    className="rename-input"
                    value={conv.title}
                    onChange={(event) => handleRenameConversation(conv.id, event.target.value)}
                    onBlur={() => setRenamingId(null)}
                    autoFocus
                  />
                ) : (
                  <div className="conversation-title">{conv.title}</div>
                )}
                <div className="conversation-meta">
                  {conv.messages.length} messages
                </div>
              </div>
              <div className="conversation-actions">
                <button onClick={() => setRenamingId(conv.id)}>Rename</button>
                <button onClick={() => handleDeleteConversation(conv.id)}>Delete</button>
              </div>
            </div>
          ))}
        </div>
        <div className="section-title">Agent profiles</div>
        <div className="agent-list">
          {agents.map((agent) => (
            <div key={agent.id} className="agent-card">
              {editingAgentId === agent.id ? (
                <div className="agent-edit">
                  <input
                    value={agent.label}
                    onChange={(event) => updateAgent(agent.id, { label: event.target.value })}
                  />
                  <input
                    value={agent.id}
                    onChange={(event) => updateAgent(agent.id, { id: event.target.value })}
                  />
                  <input
                    type="number"
                    min={1}
                    max={10}
                    value={agent.maxStep}
                    onChange={(event) => updateAgent(agent.id, { maxStep: Number(event.target.value) })}
                  />
                  <div className="row">
                    <button onClick={() => setEditingAgentId(null)}>Done</button>
                  </div>
                </div>
              ) : (
                <>
                  <div className="agent-label">{agent.label}</div>
                  <div className="agent-meta">ID: {agent.id}</div>
                  <div className="agent-meta">Max step: {agent.maxStep}</div>
                  <div className="row">
                    <button onClick={() => setEditingAgentId(agent.id)}>Edit</button>
                    <button onClick={() => removeAgent(agent.id)}>Remove</button>
                  </div>
                </>
              )}
            </div>
          ))}
          <button className="ghost" onClick={handleAddAgent}>
            Add agent profile
          </button>
        </div>
      </aside>

      <main className="main">
        <header className="topbar">
          <div className="agent-selector">
            <label>Agent</label>
            <select
              value={activeConversation?.agentId || ''}
              onChange={(event) => handleAgentChange(event.target.value)}
            >
              {agents.map((agent) => (
                <option key={agent.id} value={agent.id}>
                  {agent.label} ({agent.id})
                </option>
              ))}
            </select>
          </div>
          <div className="agent-selector">
            <label>Max step</label>
            <input
              type="number"
              min={1}
              max={10}
              value={activeConversation?.maxStep || 3}
              onChange={(event) => handleMaxStepChange(Number(event.target.value))}
            />
          </div>
          <button className="ghost" onClick={() => setShowProcess((prev) => !prev)}>
            {showProcess ? 'Hide' : 'Show'} process
          </button>
        </header>

        {error && <div className="error-banner">{error}</div>}

        <div className="chat-layout">
          <section className="chat-window">
            {activeConversation?.messages.map((message) => (
              <div key={message.id} className={`message ${message.role}`}>
                <div className="message-meta">
                  <span>{message.role === 'assistant' ? 'Agent' : message.role}</span>
                  <span>{humanTime(message.createdAt)}</span>
                </div>
                <div className="message-bubble">
                  {message.role === 'assistant' ? (
                    <MarkdownView content={message.content || '...'} />
                  ) : (
                    <div className="plain-text">{message.content}</div>
                  )}
                </div>
                {message.steps && message.steps.length > 0 && (
                  <div className="message-steps">
                    <div className="steps-title">Process snapshot</div>
                    {message.steps.slice(-4).map((step) => (
                      <div key={step.id} className={`step-chip ${step.type}`}>
                        <span>{groupLabel(step.type, step.subType)}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
            <div ref={endRef} />
          </section>

          {showProcess && (
            <aside className="process-panel">
              <div className="process-header">
                <div>Live process</div>
                <div className="process-sub">{activeConversation?.stepEvents.length || 0} events</div>
              </div>
              <div className="process-list">
                {activeConversation?.stepEvents.map((step) => (
                  <div key={step.id} className={`process-item ${step.type}`}>
                    <div className="process-title">
                      <span>{groupLabel(step.type, step.subType)}</span>
                      {step.step != null && <span>Step {step.step}</span>}
                    </div>
                    <div className="process-content">{step.content}</div>
                    {step.timestamp && <div className="process-meta">{humanTime(step.timestamp)}</div>}
                  </div>
                ))}
              </div>
            </aside>
          )}
        </div>

        <footer className="composer">
          <div className="composer-input">
            <textarea
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              placeholder="Ask the auto-agent..."
              rows={3}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && !event.shiftKey) {
                  event.preventDefault();
                  handleSend();
                }
              }}
            />
          </div>
          <div className="composer-actions">
            <button className="ghost" onClick={handleRegenerate} disabled={isStreaming}>
              Regenerate
            </button>
            {isStreaming ? (
              <button className="danger" onClick={handleStop}>
                Stop
              </button>
            ) : (
              <button className="primary" onClick={() => handleSend()}>
                Send
              </button>
            )}
          </div>
        </footer>
      </main>
    </div>
  );
}
