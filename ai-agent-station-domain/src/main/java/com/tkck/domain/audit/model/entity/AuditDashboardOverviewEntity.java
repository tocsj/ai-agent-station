package com.tkck.domain.audit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditDashboardOverviewEntity {

    private String range;
    private String taskType;
    private Integer taskTotal;
    private Integer successTotal;
    private Integer failedTotal;
    private Integer runningTotal;
    private Double successRate;
    private Long avgDurationMs;
    private Integer timeoutTotal;
    private Integer degradedTotal;
    private Integer modelCalls;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private Integer publishSuccessTotal;
    private Integer publishFailedTotal;
}
