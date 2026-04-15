package com.tkck.domain.agent.service.runtime.resilience;

public class ExecutionStageResult<T> {

    private final boolean success;
    private final boolean degraded;
    private final T payload;
    private final ExecutionErrorCode errorCode;
    private final int attempts;

    private ExecutionStageResult(boolean success, boolean degraded, T payload, ExecutionErrorCode errorCode, int attempts) {
        this.success = success;
        this.degraded = degraded;
        this.payload = payload;
        this.errorCode = errorCode;
        this.attempts = attempts;
    }

    public static <T> ExecutionStageResult<T> success(T payload, int attempts) {
        return new ExecutionStageResult<>(true, false, payload, null, attempts);
    }

    public static <T> ExecutionStageResult<T> degraded(T payload, ExecutionErrorCode errorCode, int attempts) {
        return new ExecutionStageResult<>(true, true, payload, errorCode, attempts);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public T getPayload() {
        return payload;
    }

    public ExecutionErrorCode getErrorCode() {
        return errorCode;
    }

    public int getAttempts() {
        return attempts;
    }
}
