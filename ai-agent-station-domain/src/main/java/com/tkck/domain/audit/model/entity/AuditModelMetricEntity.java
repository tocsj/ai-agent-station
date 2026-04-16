package com.tkck.domain.audit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditModelMetricEntity {

    private String clientId;
    private String modelCode;
    private Integer callTotal;
    private Integer successTotal;
    private Integer failedTotal;
    private Long avgDurationMs;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private Long avgTotalTokens;
}
