export type Role = 'user' | 'assistant' | 'system';

export type StepType =
  | 'analysis'
  | 'execution'
  | 'supervision'
  | 'summary'
  | 'error'
  | 'complete'
  | 'unknown';

export interface StepEvent {
  id: string;
  type: StepType;
  subType?: string;
  step?: number | null;
  content: string;
  completed?: boolean;
  timestamp?: number | null;
  sessionId?: string | null;
}

export interface Message {
  id: string;
  role: Role;
  content: string;
  createdAt: number;
  steps?: StepEvent[];
}

export interface Conversation {
  id: string;
  title: string;
  createdAt: number;
  updatedAt: number;
  agentId: string;
  maxStep: number;
  messages: Message[];
  stepEvents: StepEvent[];
}

export interface AgentProfile {
  id: string;
  label: string;
  maxStep: number;
}

export interface AutoAgentRequest {
  aiAgentId: string;
  message: string;
  sessionId: string;
  maxStep?: number;
}

export interface AutoAgentEventPayload {
  type?: string;
  subType?: string;
  step?: number | null;
  content?: string;
  completed?: boolean;
  timestamp?: number | null;
  sessionId?: string | null;
}
