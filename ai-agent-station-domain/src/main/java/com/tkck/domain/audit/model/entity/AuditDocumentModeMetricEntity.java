package com.tkck.domain.audit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditDocumentModeMetricEntity {

    private String mode;
    private String modeName;
    private Integer taskTotal;
    private Integer successTotal;
    private Integer failedTotal;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
}
