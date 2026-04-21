package com.tkck.domain.audit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLlmCallMetricEntity {

    private Long id;
    private String callId;
    private String traceId;
    private String taskType;
    private String taskSubType;
    private String taskId;
    private String sessionId;
    private String stepName;
    private String stage;
    private String clientId;
    private String modelCode;
    private String status;
    private Long durationMs;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private String errorCode;
    private String errorMessage;
    private String location;
    private String createTime;
}
