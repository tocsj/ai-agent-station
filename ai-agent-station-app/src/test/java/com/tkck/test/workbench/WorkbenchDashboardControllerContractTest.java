package com.tkck.test.workbench;

import com.tkck.domain.workbench.model.entity.WorkbenchAgentCardEntity;
import com.tkck.domain.workbench.model.entity.WorkbenchDashboardEntity;
import com.tkck.domain.workbench.model.entity.WorkbenchOverviewEntity;
import com.tkck.domain.workbench.model.entity.WorkbenchPlatformStatusEntity;
import com.tkck.domain.workbench.model.entity.WorkbenchRecentRunEntity;
import com.tkck.domain.workbench.service.IWorkbenchDashboardService;
import com.tkck.trigger.http.WorkbenchDashboardController;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class WorkbenchDashboardControllerContractTest {

    @Test
    public void shouldExposeWorkbenchDashboardSnapshot() throws Exception {
        IWorkbenchDashboardService workbenchDashboardService = mock(IWorkbenchDashboardService.class);
        WorkbenchDashboardController controller = new WorkbenchDashboardController();
        ReflectionTestUtils.setField(controller, "workbenchDashboardService", workbenchDashboardService);

        when(workbenchDashboardService.queryDashboard("7d", 10)).thenReturn(WorkbenchDashboardEntity.builder()
                .range("7d")
                .platform(WorkbenchPlatformStatusEntity.builder()
                        .apiVersion("v1.2")
                        .apiStatus("RUNNING")
                        .statusText("Platform API 正常运行")
                        .build())
                .agentCards(List.of(
                        WorkbenchAgentCardEntity.builder()
                                .taskType("resume_evaluation")
                                .taskTypeName("简历评估")
                                .taskTotal(12)
                                .successRate(91.67)
                                .build()
                ))
                .overview(WorkbenchOverviewEntity.builder()
                        .taskTotal(30)
                        .runningTotal(2)
                        .avgDurationMs(1800L)
                        .modelCalls(66)
                        .successRate(86.67)
                        .totalTokens(42000L)
                        .build())
                .recentRuns(List.of(
                        WorkbenchRecentRunEntity.builder()
                                .traceId("trace-001")
                                .displayTaskId("TSK-21")
                                .taskTypeName("内容自动化")
                                .statusText("成功")
                                .totalTokens(3200L)
                                .build()
                ))
                .build());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/v1/workbench/dashboard").param("range", "7d").param("recentLimit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.platform.apiStatus").value("RUNNING"))
                .andExpect(jsonPath("$.data.agentCards[0].taskType").value("resume_evaluation"))
                .andExpect(jsonPath("$.data.overview.totalTokens").value(42000))
                .andExpect(jsonPath("$.data.recentRuns[0].displayTaskId").value("TSK-21"));
    }
}
