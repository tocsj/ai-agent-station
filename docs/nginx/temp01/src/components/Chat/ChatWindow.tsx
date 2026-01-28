import React, { useRef, useEffect } from 'react';
import { useChatStore } from '../../store/chatStore';
import { ProcessPanel } from './ProcessPanel';
import { User } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import { clsx } from 'clsx';
import type { ChatMessage } from '../../types/api';

export const ChatWindow: React.FC = () => {
    const { currentMessages, currentProcessSteps } = useChatStore();
    const bottomRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [currentMessages, currentProcessSteps]);

    if (currentMessages.length === 0) {
        return (
            <div className="flex-1 flex flex-col items-center justify-center text-gray-400">
                <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
                    <span role="img" aria-label="sparkles" className="text-2xl">✨</span>
                </div>
                <p className="text-lg">有什么我可以帮您的吗？</p>
            </div>
        )
    }

    return (
        <div className="flex-1 overflow-y-auto p-4 md:p-8 space-y-6">
            {currentMessages.map((msg: ChatMessage, index: number) => {
                const isLast = index === currentMessages.length - 1;
                // Only show process steps under the LAST message if it is from assistant and we are generating or have steps
                const showProcess = isLast && msg.role === 'assistant' && currentProcessSteps.length > 0;

                return (
                    <div key={msg.id} className={clsx("flex gap-4 max-w-4xl mx-auto", msg.role === 'user' ? "flex-row-reverse" : "flex-row")}>
                        <div className={clsx(
                            "w-8 h-8 rounded-full flex items-center justify-center shrink-0 mt-1",
                            msg.role === 'user' ? "bg-gray-800 text-white" : "bg-green-600 text-white"
                        )}>
                            {msg.role === 'user' ? <User size={16} /> : <span className="font-bold text-xs">AI</span>}
                        </div>

                        <div className="flex flex-col max-w-[85%]">
                            <div className="mb-1 text-xs text-gray-400 px-1">
                                {msg.role === 'user' ? 'You' : 'AutoAgent'}
                            </div>

                            {/* Process Panel acts as a precursor to the final answer */}
                            {showProcess && <ProcessPanel steps={currentProcessSteps} />}

                            <div className={clsx(
                                "prose prose-sm px-4 py-3 rounded-2xl shadow-sm border",
                                msg.role === 'user'
                                    ? "bg-white text-gray-900 border-gray-200 rounded-tr-sm"
                                    : "bg-white/90 text-gray-900 border-gray-100 rounded-tl-sm backdrop-blur-sm"
                            )}>
                                {msg.role === 'assistant' && msg.content === '' && !msg.isThinking ?
                                    <span className="text-gray-400 italic">空响应</span> :
                                    <ReactMarkdown>{msg.content}</ReactMarkdown>
                                }
                                {msg.isThinking && (
                                    <span className="inline-flex gap-1 items-center ml-2">
                                        <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                                        <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                                        <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                                    </span>
                                )}
                            </div>
                        </div>
                    </div>
                );
            })}
            <div ref={bottomRef} />
        </div>
    );
};
