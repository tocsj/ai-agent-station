package com.tkck.domain.audit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditStepMetricSummaryEntity {

    private String stepName;
    private String stepNameLabel;
    private String stage;
    private Integer executeTotal;
    private Integer successTotal;
    private Integer failedTotal;
    private Integer timeoutTotal;
    private Integer degradedTotal;
    private Long avgDurationMs;
    private Double successRate;
    private Integer llmCallTotal;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private Long avgTotalTokens;
}
