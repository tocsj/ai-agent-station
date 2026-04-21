package com.tkck.domain.workbench.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkbenchAgentCardEntity {

    private String taskType;
    private String taskTypeName;
    private String description;
    private String routePath;
    private Integer taskTotal;
    private Double successRate;
    private String lastRunTime;
}
