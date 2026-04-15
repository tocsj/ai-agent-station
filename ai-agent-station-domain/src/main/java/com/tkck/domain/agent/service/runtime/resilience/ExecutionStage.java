package com.tkck.domain.agent.service.runtime.resilience;

public enum ExecutionStage {
    ROOT,
    STEP1_ANALYZE,
    STEP2_EXECUTE,
    STEP3_VERIFY,
    STEP4_SUMMARIZE,
    SSE_PUSH
}
