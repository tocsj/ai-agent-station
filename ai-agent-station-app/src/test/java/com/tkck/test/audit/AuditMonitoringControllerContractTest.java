package com.tkck.test.audit;

import com.tkck.domain.audit.model.entity.AuditDashboardOverviewEntity;
import com.tkck.domain.audit.model.entity.AuditEventEntity;
import com.tkck.domain.audit.model.entity.AuditEventPageEntity;
import com.tkck.domain.audit.model.entity.AuditLlmCallMetricEntity;
import com.tkck.domain.audit.model.entity.AuditModelMetricEntity;
import com.tkck.domain.audit.service.IAuditMonitoringService;
import com.tkck.trigger.http.AuditMonitoringController;
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

public class AuditMonitoringControllerContractTest {

    @Test
    public void shouldExposeOverviewModelMetricsAndAuditEvents() throws Exception {
        IAuditMonitoringService auditMonitoringService = mock(IAuditMonitoringService.class);
        AuditMonitoringController controller = new AuditMonitoringController();
        ReflectionTestUtils.setField(controller, "auditMonitoringService", auditMonitoringService);

        when(auditMonitoringService.queryOverview("today", "all")).thenReturn(AuditDashboardOverviewEntity.builder()
                .range("today")
                .taskType("all")
                .taskTotal(10)
                .successTotal(8)
                .failedTotal(1)
                .runningTotal(1)
                .successRate(80.0)
                .avgDurationMs(1500L)
                .timeoutTotal(0)
                .degradedTotal(1)
                .modelCalls(4)
                .promptTokens(600L)
                .completionTokens(500L)
                .totalTokens(1100L)
                .publishSuccessTotal(4)
                .publishFailedTotal(1)
                .build());
        when(auditMonitoringService.queryModelMetrics("7d", "all")).thenReturn(List.of(
                AuditModelMetricEntity.builder()
                        .clientId("5301")
                        .modelCode("2007")
                        .callTotal(8)
                        .totalTokens(2100L)
                        .build()
        ));
        when(auditMonitoringService.queryLlmCalls("trace-1")).thenReturn(List.of(
                AuditLlmCallMetricEntity.builder()
                        .callId("llm-1")
                        .stepName("draft")
                        .totalTokens(1200L)
                        .build()
        ));
        when(auditMonitoringService.queryEvents("all", null, null, "FAILED", 1, 20)).thenReturn(AuditEventPageEntity.builder()
                .page(1)
                .pageSize(20)
                .total(1L)
                .items(List.of(AuditEventEntity.builder()
                        .eventId("audit-1")
                        .eventType("CONTENT_PUBLISH_EXECUTE")
                        .bizType("content_automation")
                        .bizId("21")
                        .status("FAILED")
                        .location("PublishExecutorNode#apply")
                        .metadataJson("{}")
                        .build()))
                .build());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/v1/audit/dashboard/overview").param("range", "today").param("taskType", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.successRate").value(80.0))
                .andExpect(jsonPath("$.data.totalTokens").value(1100));
        mockMvc.perform(get("/api/v1/audit/dashboard/model-metrics").param("range", "7d").param("taskType", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].clientId").value("5301"));
        mockMvc.perform(get("/api/v1/audit/execution/trace-1/llm-calls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].callId").value("llm-1"));
        mockMvc.perform(get("/api/v1/audit/events").param("taskType", "all").param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].eventType").value("CONTENT_PUBLISH_EXECUTE"));
    }
}
