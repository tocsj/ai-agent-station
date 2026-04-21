package com.tkck.domain.agent.service.runtime.resilience;

import java.util.Arrays;

public enum ExecutionErrorCode {

    STAGE_TIMEOUT("AGENT_TIMEOUT_001", "阶段执行超时", true),
    MODEL_TRANSIENT_ERROR("AGENT_MODEL_TRANSIENT_002", "模型调用临时失败", true),
    MODEL_REQUEST_ERROR("AGENT_MODEL_REQUEST_003", "模型请求错误", false),
    RAG_RETRIEVE_ERROR("AGENT_RAG_004", "检索失败", true),
    SSE_DISCONNECTED("AGENT_SSE_005", "流连接已断开", false),
    INVALID_REQUEST("AGENT_REQUEST_006", "请求参数非法", false),
    DATA_NOT_FOUND("AGENT_DATA_007", "数据不存在", false),
    SYSTEM_ERROR("AGENT_SYSTEM_999", "系统内部错误", false);

    private final String code;
    private final String message;
    private final boolean retryable;

    ExecutionErrorCode(String code, String message, boolean retryable) {
        this.code = code;
        this.message = message;
        this.retryable = retryable;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public static ExecutionErrorCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            return SYSTEM_ERROR;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(SYSTEM_ERROR);
    }
}
