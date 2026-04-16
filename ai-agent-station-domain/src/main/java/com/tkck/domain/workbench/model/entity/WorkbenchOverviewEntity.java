package com.tkck.domain.workbench.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkbenchOverviewEntity {

    private Integer taskTotal;
    private Integer runningTotal;
    private Long avgDurationMs;
    private Integer modelCalls;
    private Double successRate;
    private Long totalTokens;
}
