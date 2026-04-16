package com.tkck.domain.workbench.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkbenchDashboardEntity {

    private String range;
    private WorkbenchPlatformStatusEntity platform;
    private List<WorkbenchAgentCardEntity> agentCards;
    private WorkbenchOverviewEntity overview;
    private List<WorkbenchRecentRunEntity> recentRuns;
}
