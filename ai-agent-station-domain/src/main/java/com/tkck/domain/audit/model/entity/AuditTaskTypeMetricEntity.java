package com.tkck.domain.audit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditTaskTypeMetricEntity {

    private String taskType;
    private String taskTypeName;
    private Integer taskTotal;
    private Integer successTotal;
    private Integer failedTotal;
    private Double successRate;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
}
