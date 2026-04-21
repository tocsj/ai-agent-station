package com.tkck.domain.agent.service.runtime.resilience;

public class ExecutionRetryPolicy {

    private final int maxAttempts;
    private final long baseBackoffMillis;

    private ExecutionRetryPolicy(int maxAttempts, long baseBackoffMillis) {
        this.maxAttempts = maxAttempts;
        this.baseBackoffMillis = baseBackoffMillis;
    }

    public static ExecutionRetryPolicy defaults() {
        return new ExecutionRetryPolicy(3, 150L);
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public boolean shouldRetry(ExecutionErrorCode errorCode, int attempt) {
        return errorCode != null && errorCode.isRetryable() && attempt < maxAttempts;
    }

    public long nextBackoffMillis(int attempt) {
        long cappedAttempt = Math.max(1, attempt);
        return baseBackoffMillis * cappedAttempt;
    }
}
