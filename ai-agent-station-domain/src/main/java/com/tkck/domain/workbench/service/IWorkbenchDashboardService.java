package com.tkck.domain.workbench.service;

import com.tkck.domain.workbench.model.entity.WorkbenchDashboardEntity;

public interface IWorkbenchDashboardService {

    WorkbenchDashboardEntity queryDashboard(String range, Integer recentLimit);
}
