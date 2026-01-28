# Simple Agent Console

A lightweight static frontend located at docs/nginx/simple for exercising /api/v1/agent/auto_agent streams. No bundler is required; any static server or nginx can host the files.

## Features
- Collects iAgentId, sessionId, maxStep, and message in a single form
- Streams and renders step events plus the raw SSE buffer in real time
- Keeps a basic conversation log, copies the summary, clears logs, and regenerates session ids
- Persists the backend base URL, agent id, and max step inside localStorage

## Usage
1. Ensure the backend is reachable (for example http://127.0.0.1:8091).
2. Serve this folder, e.g. 
px serve docs/nginx/simple or mount it through nginx.
3. The page targets /api/v1/agent/auto_agent on the same origin by default. Fill the ¡°backend base¡± field with a prefix or absolute URL if you need a different host (e.g. /api or http://127.0.0.1:8091).
4. Enter the agent id and your task prompt, click ¡°send¡±, and monitor the timeline, summary, and raw SSE logs. Use ¡°stop¡± to abort the in-flight request.
