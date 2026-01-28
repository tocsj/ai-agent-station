import React from 'react';
import { useChatStore } from '../../store/chatStore';
import { MessageSquare, Plus, Trash2 } from 'lucide-react';
import { clsx } from 'clsx';

export const Sidebar: React.FC<{ isOpen: boolean, setIsOpen: (v: boolean) => void }> = ({ isOpen }) => {
    const { sessions, currentSessionId, createSession, selectSession, deleteSession } = useChatStore();

    return (
        <div className={clsx(
            "fixed inset-y-0 left-0 z-50 w-64 bg-gray-900 text-white transform transition-transform duration-200 ease-in-out flex flex-col",
            isOpen ? "translate-x-0" : "-translate-x-full",
            "md:relative md:translate-x-0"
        )}>
            <div className="p-4 border-b border-gray-700 flex justify-between items-center">
                <h1 className="text-xl font-bold flex items-center gap-2">
                    <span role="img" aria-label="bot">🤖</span> AutoAgent
                </h1>
                <button onClick={createSession} className="p-2 hover:bg-gray-700 rounded-lg transition-colors" title="新对话">
                    <Plus size={20} />
                </button>
            </div>

            <div className="flex-1 overflow-y-auto p-2 space-y-1">
                {sessions.map((session: { id: string, title: string }) => (
                    <div
                        key={session.id}
                        onClick={() => selectSession(session.id)}
                        className={clsx(
                            "group flex items-center gap-3 p-3 rounded-lg cursor-pointer transition-colors text-sm",
                            session.id === currentSessionId ? "bg-gray-700 text-white" : "hover:bg-gray-800 text-gray-300"
                        )}
                    >
                        <MessageSquare size={16} />
                        <span className="truncate flex-1">{session.title}</span>
                        <button
                            onClick={(e) => { e.stopPropagation(); deleteSession(session.id); }}
                            className="opacity-0 group-hover:opacity-100 p-1 hover:text-red-400 transition-opacity"
                        >
                            <Trash2 size={14} />
                        </button>
                    </div>
                ))}
                {sessions.length === 0 && (
                    <div className="text-center text-gray-500 mt-10 text-sm">暂无历史对话</div>
                )}
            </div>

            <div className="p-4 border-t border-gray-700 text-xs text-gray-500 text-center">
                Powered by AutoClient
            </div>
        </div>
    );
};
