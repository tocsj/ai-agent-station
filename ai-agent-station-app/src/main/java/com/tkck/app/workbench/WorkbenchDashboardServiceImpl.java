package com.tkck.app.workbench;

import com.tkck.domain.audit.model.entity.AuditDashboardOverviewEntity;
import com.tkck.domain.audit.service.IAuditMonitoringService;
import com.tkck.domain.workbench.adapter.repository.IWorkbenchDashboardRepository;
import com.tkck.domain.workbench.model.entity.WorkbenchDashboardEntity;
import com.tkck.domain.workbench.model.entity.WorkbenchOverviewEntity;
import com.tkck.domain.workbench.model.entity.WorkbenchPlatformStatusEntity;
import com.tkck.domain.workbench.service.IWorkbenchDashboardService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class WorkbenchDashboardServiceImpl implements IWorkbenchDashboardService {

    @Resource
    private IWorkbenchDashboardRepository workbenchDashboardRepository;

    @Resource
    private IAuditMonitoringService auditMonitoringService;

    @Override
    public WorkbenchDashboardEntity queryDashboard(String range, Integer recentLimit) {
        String normalizedRange = normalizeRange(range);
        int actualLimit = normalizeLimit(recentLimit);
        AuditDashboardOverviewEntity overview = auditMonitoringService.queryOverview(normalizedRange, "all");
        return WorkbenchDashboardEntity.builder()
                .range(normalizedRange)
                .platform(WorkbenchPlatformStatusEntity.builder()
                        .apiVersion("v1.2")
                        .apiStatus("RUNNING")
                        .statusText("Platform API 正常运行")
                        .build())
                .agentCards(workbenchDashboardRepository.queryAgentCards(normalizedRange))
                .overview(WorkbenchOverviewEntity.builder()
                        .taskTotal(overview.getTaskTotal())
                        .runningTotal(overview.getRunningTotal())
                        .avgDurationMs(overview.getAvgDurationMs())
                        .modelCalls(overview.getModelCalls())
                        .successRate(overview.getSuccessRate())
                        .totalTokens(overview.getTotalTokens())
                        .build())
                .recentRuns(workbenchDashboardRepository.queryRecentRuns(actualLimit))
                .build();
    }

    private String normalizeRange(String range) {
        if ("today".equalsIgnoreCase(range) || "30d".equalsIgnoreCase(range)) {
            return range.toLowerCase();
        }
        return "7d";
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 10;
        }
        return Math.max(1, Math.min(limit, 20));
    }
}
