export interface AutoAgentRequestDTO {
    aiAgentId: string;
    message: string;
    sessionId: string;
    maxStep?: number;
}

export type AgentEventType = 'analysis' | 'execution' | 'supervision' | 'summary' | 'error' | 'complete';

export interface AutoAgentExecuteResultEntity {
    type: AgentEventType;
    subType?: string;
    step?: number;
    content: string;
    completed: boolean;
    timestamp: number;
    sessionId: string;
}

export interface ChatSession {
    id: string;
    title: string;
    createdAt: number;
    messages: ChatMessage[];
    processSteps: Record<string, ProcessStep>; // stepId -> ProcessStep
}

export interface ChatMessage {
    id: string;
    role: 'user' | 'assistant';
    content: string;
    timestamp: number;
    isThinking?: boolean;
}

export interface ProcessStep {
    step: number;
    events: AutoAgentExecuteResultEntity[];
}
