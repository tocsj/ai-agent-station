package com.tkck.domain.audit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditEventEntity {

    private Long id;
    private String eventId;
    private String eventType;
    private String eventTypeName;
    private String bizType;
    private String bizId;
    private String sessionId;
    private String executionMode;
    private String operatorId;
    private String operatorName;
    private String requestUri;
    private String requestMethod;
    private String status;
    private String errorCode;
    private String errorMessage;
    private String location;
    private String metadataJson;
    private String createTime;
}
