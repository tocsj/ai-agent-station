package com.tkck.domain.agent.service.runtime.resilience;

public class ExecutionFailureContext {

    private final String sessionId;
    private final String scene;
    private final String location;

    public ExecutionFailureContext(String sessionId, String scene) {
        this(sessionId, scene, "unknown");
    }

    public ExecutionFailureContext(String sessionId, String scene, String location) {
        this.sessionId = sessionId;
        this.scene = scene;
        this.location = location;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getScene() {
        return scene;
    }

    public String getLocation() {
        return location;
    }
}
