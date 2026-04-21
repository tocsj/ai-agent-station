package com.tkck.domain.agent.model.valobj;

public enum ExecutionMode {

    SINGLE_SHOT,
    STRUCTURED_PLAN_EXECUTE,
    OPEN_PLAN_EXECUTE;

    public static ExecutionMode defaultMode() {
        return STRUCTURED_PLAN_EXECUTE;
    }
}
