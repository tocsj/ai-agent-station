import type { AutoAgentRequestDTO, AutoAgentExecuteResultEntity } from '../types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1/agent';

export const streamAgentResponse = async (
    request: AutoAgentRequestDTO,
    onEvent: (event: AutoAgentExecuteResultEntity) => void,
    onError: (error: string) => void
) => {
    try {
        const response = await fetch(`${API_BASE_URL}/auto_agent`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(request),
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const reader = response.body?.getReader();
        const decoder = new TextDecoder();

        if (!reader) {
            throw new Error('Response body is unavailable');
        }

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            const chunk = decoder.decode(value, { stream: true });
            const lines = chunk.split('\n');

            for (const line of lines) {
                if (line.startsWith('data: ')) {
                    try {
                        const data = JSON.parse(line.slice(6));
                        // Handle double newline which might result in empty data object or specific server heartbeat
                        if (data) {
                            onEvent(data as AutoAgentExecuteResultEntity);
                        }
                    } catch (e) {
                        console.error('Failed to parse SSE data:', line, e);
                    }
                }
            }
        }
    } catch (error: any) {
        onError(error.message || 'Network error');
    }
};
