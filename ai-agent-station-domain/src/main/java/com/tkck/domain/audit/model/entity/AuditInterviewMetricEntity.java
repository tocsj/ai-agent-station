package com.tkck.domain.audit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditInterviewMetricEntity {

    private Integer sessionTotal;
    private Integer completedSessionTotal;
    private Integer abortedSessionTotal;
    private Integer roundTotal;
    private Double avgRoundsPerSession;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
}
