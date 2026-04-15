package com.tkck.domain.agent.service.runtime.resilience;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

public class ExecutionTimeoutPolicy {

    private final Map<ExecutionStage, Duration> timeoutMap;

    private ExecutionTimeoutPolicy(Map<ExecutionStage, Duration> timeoutMap) {
        this.timeoutMap = timeoutMap;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ExecutionTimeoutPolicy defaults() {
        return builder()
                .timeout(ExecutionStage.ROOT, Duration.ofSeconds(45))
                .timeout(ExecutionStage.STEP1_ANALYZE, Duration.ofSeconds(12))
                .timeout(ExecutionStage.STEP2_EXECUTE, Duration.ofSeconds(25))
                .timeout(ExecutionStage.STEP3_VERIFY, Duration.ofSeconds(15))
                .timeout(ExecutionStage.STEP4_SUMMARIZE, Duration.ofSeconds(12))
                .timeout(ExecutionStage.SSE_PUSH, Duration.ofSeconds(1))
                .build();
    }

    public Duration getTimeout(ExecutionStage stage) {
        return timeoutMap.getOrDefault(stage, Duration.ofSeconds(15));
    }

    public static class Builder {
        private final Map<ExecutionStage, Duration> timeoutMap = new EnumMap<>(ExecutionStage.class);

        public Builder timeout(ExecutionStage stage, Duration duration) {
            timeoutMap.put(stage, duration);
            return this;
        }

        public ExecutionTimeoutPolicy build() {
            return new ExecutionTimeoutPolicy(new EnumMap<>(timeoutMap));
        }
    }
}
