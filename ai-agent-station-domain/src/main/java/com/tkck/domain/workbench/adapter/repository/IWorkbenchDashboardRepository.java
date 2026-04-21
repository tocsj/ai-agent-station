package com.tkck.domain.workbench.adapter.repository;

import com.tkck.domain.workbench.model.entity.WorkbenchAgentCardEntity;
import com.tkck.domain.workbench.model.entity.WorkbenchRecentRunEntity;

import java.util.List;

public interface IWorkbenchDashboardRepository {

    List<WorkbenchAgentCardEntity> queryAgentCards(String range);

    List<WorkbenchRecentRunEntity> queryRecentRuns(int limit);
}
