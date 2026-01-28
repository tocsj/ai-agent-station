import React, { createContext, useContext, useState, useEffect } from 'react';
import type { ChatSession, ChatMessage, ProcessStep, AutoAgentExecuteResultEntity } from '../types/api';
import { streamAgentResponse } from '../services/api'; // Import implementation

interface ChatContextType {
    sessions: ChatSession[];
    currentSessionId: string | null;
    createSession: () => void;
    selectSession: (id: string) => void;
    deleteSession: (id: string) => void;
    sendMessage: (text: string) => Promise<void>;
    isGenerating: boolean;
    currentProcessSteps: ProcessStep[];
    currentMessages: ChatMessage[];
}

const STORAGE_KEY = 'chat_sessions';

export const ChatContext = createContext<ChatContextType | undefined>(undefined);

export const ChatProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [sessions, setSessions] = useState<ChatSession[]>(() => {
        const stored = localStorage.getItem(STORAGE_KEY);
        return stored ? JSON.parse(stored) : [];
    });
    const [currentSessionId, setCurrentSessionId] = useState<string | null>(null);
    const [isGenerating, setIsGenerating] = useState(false);

    // Sync to local storage
    useEffect(() => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions));
    }, [sessions]);

    const currentSession = sessions.find(s => s.id === currentSessionId);
    const currentMessages = currentSession?.messages || [];

    // Transform session process steps map to array for UI
    const currentProcessSteps = React.useMemo(() => {
        if (!currentSession?.processSteps) return [];
        // Combine steps from map values and sort by step number
        return Object.values(currentSession.processSteps).sort((a, b) => a.step - b.step);
    }, [currentSession]);


    const createSession = () => {
        const newSession: ChatSession = {
            id: crypto.randomUUID(),
            title: '新对话',
            createdAt: Date.now(),
            messages: [],
            processSteps: {}
        };
        setSessions(prev => [newSession, ...prev]);
        setCurrentSessionId(newSession.id);
    };

    const selectSession = (id: string) => setCurrentSessionId(id);

    const deleteSession = (id: string) => {
        setSessions(prev => prev.filter(s => s.id !== id));
        if (currentSessionId === id) setCurrentSessionId(null);
    }

    const sendMessage = async (text: string) => {
        if (!currentSessionId || isGenerating) return;

        setIsGenerating(true);

        // Add user message
        const userMsg: ChatMessage = { id: crypto.randomUUID(), role: 'user', content: text, timestamp: Date.now() };

        // Add placeholder assistant message
        const assistantMsgId = crypto.randomUUID();
        const assistantMsg: ChatMessage = { id: assistantMsgId, role: 'assistant', content: '', timestamp: Date.now(), isThinking: true };

        setSessions(prev => prev.map(s => {
            if (s.id === currentSessionId) {
                return { ...s, messages: [...s.messages, userMsg, assistantMsg] };
            }
            return s;
        }));

        try {
            await streamAgentResponse(
                {
                    aiAgentId: '3101', // Default Agent ID 
                    message: text,
                    sessionId: currentSessionId,
                    maxStep: 5
                },
                (event) => handleAgentEvent(currentSessionId, assistantMsgId, event),
                (error) => console.error(error)
            );
        } finally {
            setIsGenerating(false);
            // Remove thinking state
            setSessions(prev => prev.map(s => {
                if (s.id === currentSessionId) {
                    return {
                        ...s,
                        messages: s.messages.map(m => m.id === assistantMsgId ? { ...m, isThinking: false } : m)
                    }
                }
                return s;
            }));
        }
    };

    const handleAgentEvent = (sessionId: string, msgId: string, event: AutoAgentExecuteResultEntity) => {
        setSessions(prev => prev.map(session => {
            if (session.id !== sessionId) return session;

            let updatedMessages = [...session.messages];
            let updatedProcessSteps = { ...session.processSteps };

            if (event.type === 'summary' || event.type === 'complete') {
                // Append to main message content if it's a summary or result
                updatedMessages = updatedMessages.map(m => {
                    if (m.id === msgId) {
                        return { ...m, content: m.content + (event.content || '') };
                    }
                    return m;
                });
            } else {
                // It's a process step (analysis, execution, supervision)
                const stepNum = event.step || 0;
                const stepKey = `step_${stepNum}`;

                if (!updatedProcessSteps[stepKey]) {
                    updatedProcessSteps[stepKey] = { step: stepNum, events: [] };
                }

                const existingEvents = updatedProcessSteps[stepKey].events;
                // Avoid duplicates if needed, or just append. Backend sends discrete events.
                updatedProcessSteps[stepKey] = {
                    ...updatedProcessSteps[stepKey],
                    events: [...existingEvents, event]
                };
            }

            return { ...session, messages: updatedMessages, processSteps: updatedProcessSteps };
        }));
    };

    return (
        <ChatContext.Provider value={{ sessions, currentSessionId, createSession, selectSession, deleteSession, sendMessage, isGenerating, currentProcessSteps, currentMessages }}>
            {children}
        </ChatContext.Provider>
    );
};

export const useChatStore = () => {
    const context = useContext(ChatContext);
    if (!context) throw new Error('useChatStore must be used within a ChatProvider');
    return context;
};
