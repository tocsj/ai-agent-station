import React, { useState } from 'react';
import type { ProcessStep } from '../../types/api';
import { ChevronDown, ChevronRight, BrainCircuit } from 'lucide-react';
import { clsx } from 'clsx';
import ReactMarkdown from 'react-markdown';

export const ProcessPanel: React.FC<{ steps: ProcessStep[] }> = ({ steps }) => {
    const [collapsed, setCollapsed] = useState(false);

    if (steps.length === 0) return null;

    return (
        <div className="border border-gray-200 rounded-xl bg-gray-50 mb-4 overflow-hidden transition-all duration-300">
            <div
                className="flex items-center justify-between p-3 bg-gray-100 cursor-pointer hover:bg-gray-200 transition-colors"
                onClick={() => setCollapsed(!collapsed)}
            >
                <div className="flex items-center gap-2 text-sm font-medium text-gray-700">
                    <BrainCircuit size={16} className="text-blue-600" />
                    <span>思考过程 ({steps.length} 步)</span>
                </div>
                {collapsed ? <ChevronRight size={16} /> : <ChevronDown size={16} />}
            </div>

            {!collapsed && (
                <div className="p-4 space-y-4 max-h-96 overflow-y-auto bg-gray-50">
                    {steps.map((step) => (
                        <StepItem key={step.step} step={step} />
                    ))}
                </div>
            )}
        </div>
    );
};

const StepItem: React.FC<{ step: ProcessStep }> = ({ step }) => {
    const [isExpanded, setIsExpanded] = useState(true); // Default expand latest steps? Or all.

    return (
        <div className="flex flex-col gap-2">
            <button
                onClick={() => setIsExpanded(!isExpanded)}
                className="flex items-center gap-2 text-xs font-bold text-gray-500 hover:text-gray-800 uppercase tracking-wider text-left w-full"
            >
                {isExpanded ? <ChevronDown size={12} /> : <ChevronRight size={12} />}
                STEP {step.step}
            </button>

            {isExpanded && (
                <div className="pl-4 border-l-2 border-gray-200 space-y-3">
                    {step.events.map((event, idx) => (
                        <div key={idx} className="text-sm">
                            <div className="flex items-center gap-2 mb-1">
                                <Badge type={event.type} subType={event.subType} />
                                <span className="text-gray-400 text-xs">{new Date(event.timestamp).toLocaleTimeString()}</span>
                            </div>
                            <div className="prose prose-sm max-w-none text-gray-700 bg-white p-3 rounded border border-gray-100 shadow-sm">
                                <ReactMarkdown>{event.content}</ReactMarkdown>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

const Badge: React.FC<{ type: string, subType?: string }> = ({ type, subType }) => {
    let colorClass = "bg-gray-100 text-gray-600";
    let label = subType || type;

    switch (type) {
        case 'analysis':
            colorClass = "bg-blue-100 text-blue-700 border-blue-200";
            break;
        case 'execution':
            colorClass = "bg-amber-100 text-amber-700 border-amber-200";
            break;
        case 'supervision':
            colorClass = "bg-purple-100 text-purple-700 border-purple-200";
            break;
        case 'error':
            colorClass = "bg-red-100 text-red-700 border-red-200";
            break;
    }

    return (
        <span className={clsx("px-2 py-0.5 rounded-full text-xs font-medium border flex items-center gap-1 w-fit", colorClass)}>
            {label}
        </span>
    );
}
