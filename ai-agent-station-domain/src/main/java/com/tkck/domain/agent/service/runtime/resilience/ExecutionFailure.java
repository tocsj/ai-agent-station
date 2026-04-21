package com.tkck.domain.agent.service.runtime.resilience;

public class ExecutionFailure {

    private final ExecutionStage stage;
    private final ExecutionFailureContext context;
    private final ExecutionErrorCode errorCode;
    private final Throwable cause;
    private final int attempt;
    private final boolean retryable;

    public ExecutionFailure(ExecutionStage stage,
                            ExecutionFailureContext context,
                            ExecutionErrorCode errorCode,
                            Throwable cause,
                            int attempt,
                            boolean retryable) {
        this.stage = stage;
        this.context = context;
        this.errorCode = errorCode;
        this.cause = cause;
        this.attempt = attempt;
        this.retryable = retryable;
    }

    public ExecutionStage getStage() {
        return stage;
    }

    public ExecutionFailureContext getContext() {
        return context;
    }

    public ExecutionErrorCode getErrorCode() {
        return errorCode;
    }

    public Throwable getCause() {
        return cause;
    }

    public int getAttempt() {
        return attempt;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getMessage() {
        return cause == null ? errorCode.getMessage() : cause.getMessage();
    }
}
