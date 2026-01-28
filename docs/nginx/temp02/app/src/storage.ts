import { AgentProfile, Conversation } from './types';

const STORAGE_KEY = 'auto_agent_conversations_v1';
const AGENT_KEY = 'auto_agent_profiles_v1';
const ACTIVE_KEY = 'auto_agent_active_id_v1';

export function loadConversations(): Conversation[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const data = JSON.parse(raw) as Conversation[];
    return Array.isArray(data) ? data : [];
  } catch {
    return [];
  }
}

export function saveConversations(conversations: Conversation[]): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(conversations));
}

export function loadAgents(): AgentProfile[] {
  try {
    const raw = localStorage.getItem(AGENT_KEY);
    if (!raw) return [];
    const data = JSON.parse(raw) as AgentProfile[];
    return Array.isArray(data) ? data : [];
  } catch {
    return [];
  }
}

export function saveAgents(agents: AgentProfile[]): void {
  localStorage.setItem(AGENT_KEY, JSON.stringify(agents));
}

export function loadActiveConversationId(): string | null {
  return localStorage.getItem(ACTIVE_KEY);
}

export function saveActiveConversationId(id: string | null): void {
  if (id) {
    localStorage.setItem(ACTIVE_KEY, id);
  } else {
    localStorage.removeItem(ACTIVE_KEY);
  }
}
