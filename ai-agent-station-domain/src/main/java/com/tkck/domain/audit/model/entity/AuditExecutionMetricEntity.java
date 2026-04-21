package com.tkck.domain.audit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditExecutionMetricEntity {

    private String traceId;
    private String taskType;
    private String taskSubType;
    private String taskId;
    private String sessionId;
    private String executionMode;
    private String status;
    private Long totalDurationMs;
    private Integer stepCount;
    private Integer successStepCount;
    private Integer failedStepCount;
    private Integer timeoutCount;
    private Integer retryCount;
    private Integer degradedCount;
    private Integer modelCalls;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private String createTime;
    private String finishTime;
}
