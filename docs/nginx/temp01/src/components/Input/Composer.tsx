import React, { useState, type KeyboardEvent } from 'react';
import { useChatStore } from '../../store/chatStore';
import { Send, Square } from 'lucide-react';

export const Composer: React.FC = () => {
    const { sendMessage, isGenerating } = useChatStore();
    const [input, setInput] = useState('');

    const handleSend = async () => {
        if (!input.trim() || isGenerating) return;
        const text = input;
        setInput('');
        await sendMessage(text);
    };

    const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    return (
        <div className="p-4 border-t border-gray-200 bg-white/80 backdrop-blur-md">
            <div className="max-w-4xl mx-auto relative flex items-end gap-2 p-3 bg-white border border-gray-300 rounded-xl shadow-sm focus-within:ring-2 focus-within:ring-blue-500/20 focus-within:border-blue-500 transition-all">
                <textarea
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder="发送消息..."
                    className="flex-1 max-h-48 min-h-[44px] bg-transparent border-none outline-none resize-none py-2 px-1 text-sm leading-6"
                    disabled={isGenerating}
                />
                <button
                    onClick={handleSend}
                    disabled={!input.trim() || isGenerating}
                    className="p-2 bg-black text-white rounded-lg hover:bg-gray-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors mb-0.5"
                >
                    {isGenerating ? <Square size={16} className="animate-pulse" /> : <Send size={16} />}
                </button>
            </div>
            <div className="text-center mt-2 text-xs text-gray-400">
                AI Agent 可能会产生错误，请核对重要信息。
            </div>
        </div>
    );
};
