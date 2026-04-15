package com.tkck.domain.agent.service.runtime.resilience;

public class ExecutionFailureContext {

    private final String sessionId;
    private final String scene;

    public ExecutionFailureContext(String sessionId, String scene) {
        this.sessionId = sessionId;
        this.scene = scene;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getScene() {
        return scene;
    }
}
