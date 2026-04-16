package com.tkck.domain.audit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditStepMetricEntity {

    private Long id;
    private String traceId;
    private String taskId;
    private String sessionId;
    private Integer stepNo;
    private String stepName;
    private String stepNameLabel;
    private String stage;
    private String clientId;
    private String modelCode;
    private String status;
    private Long durationMs;
    private Integer retryCount;
    private Boolean timeoutFlag;
    private Boolean degradedFlag;
    private Integer llmCallTotal;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private String errorCode;
    private String errorMessage;
    private String location;
    private String createTime;
}
