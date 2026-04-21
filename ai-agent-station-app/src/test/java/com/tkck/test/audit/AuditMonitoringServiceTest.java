package com.tkck.test.audit;

import com.tkck.app.audit.AuditMonitoringServiceImpl;
import com.tkck.domain.audit.adapter.repository.IAuditMonitoringRepository;
import com.tkck.domain.audit.model.entity.AuditDashboardOverviewEntity;
import com.tkck.domain.audit.model.entity.AuditEventEntity;
import com.tkck.domain.audit.model.entity.AuditLlmCallMetricEntity;
import com.tkck.domain.audit.model.entity.AuditModelMetricEntity;
import com.tkck.domain.audit.model.entity.AuditStepMetricEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuditMonitoringServiceTest {

    @Test
    public void shouldDelegateWriteOperationsToRepository() {
        IAuditMonitoringRepository repository = mock(IAuditMonitoringRepository.class);
        AuditMonitoringServiceImpl service = new AuditMonitoringServiceImpl();
        ReflectionTestUtils.setField(service, "auditMonitoringRepository", repository);

        AuditEventEntity event = AuditEventEntity.builder()
                .eventType("CONTENT_TASK_START")
                .bizType("content_automation")
                .bizId("21")
                .sessionId("content-21")
                .executionMode("STRUCTURED_PLAN_EXECUTE")
                .status("RUNNING")
                .location("ContentAutomationWorkflowExecutor#execute")
                .metadataJson("{}")
                .build();
        AuditStepMetricEntity step = AuditStepMetricEntity.builder()
                .traceId("trace-1")
                .taskId("21")
                .sessionId("content-21")
                .stepNo(1)
                .stepName("topic_plan")
                .stage("CONTENT_TOPIC_PLAN")
                .clientId("5301")
                .modelCode("2007")
                .status("SUCCESS")
                .durationMs(1000L)
                .location("TopicPlannerNode#apply")
                .build();
        AuditLlmCallMetricEntity call = AuditLlmCallMetricEntity.builder()
                .traceId("trace-1")
                .taskType("content_automation")
                .taskId("21")
                .sessionId("content-21")
                .stepName("topic_plan")
                .stage("CONTENT_TOPIC_PLAN")
                .clientId("5301")
                .modelCode("2007")
                .status("SUCCESS")
                .durationMs(900L)
                .promptTokens(120L)
                .completionTokens(80L)
                .totalTokens(200L)
                .location("TopicPlannerNode#apply")
                .build();

        service.recordEvent(event);
        service.recordStep(step);
        service.recordLlmCall(call);

        verify(repository).recordEvent(event);
        verify(repository).recordStep(step);
        verify(repository).recordLlmCall(call);
    }

    @Test
    public void shouldReturnOverviewMetricsFromRepository() {
        IAuditMonitoringRepository repository = mock(IAuditMonitoringRepository.class);
        AuditMonitoringServiceImpl service = new AuditMonitoringServiceImpl();
        ReflectionTestUtils.setField(service, "auditMonitoringRepository", repository);

        when(repository.queryOverview("today", "all")).thenReturn(AuditDashboardOverviewEntity.builder()
                .range("today")
                .taskTotal(10)
                .successTotal(8)
                .successRate(80.0)
                .modelCalls(6)
                .totalTokens(1700L)
                .publishSuccessTotal(4)
                .publishFailedTotal(1)
                .build());

        AuditDashboardOverviewEntity overview = service.queryOverview("today", "all");

        Assert.assertEquals("today", overview.getRange());
        Assert.assertEquals(Integer.valueOf(10), overview.getTaskTotal());
        Assert.assertEquals(Double.valueOf(80.0), overview.getSuccessRate());
        Assert.assertEquals(Integer.valueOf(6), overview.getModelCalls());
        Assert.assertEquals(Long.valueOf(1700L), overview.getTotalTokens());
        Assert.assertEquals(Integer.valueOf(4), overview.getPublishSuccessTotal());
        Assert.assertEquals(Integer.valueOf(1), overview.getPublishFailedTotal());
    }

    @Test
    public void shouldReturnModelMetricsFromRepository() {
        IAuditMonitoringRepository repository = mock(IAuditMonitoringRepository.class);
        AuditMonitoringServiceImpl service = new AuditMonitoringServiceImpl();
        ReflectionTestUtils.setField(service, "auditMonitoringRepository", repository);

        when(repository.queryModelMetrics("7d", "all")).thenReturn(List.of(AuditModelMetricEntity.builder()
                .clientId("5301")
                .modelCode("2007")
                .callTotal(8)
                .successTotal(7)
                .failedTotal(1)
                .avgDurationMs(3200L)
                .promptTokens(1200L)
                .completionTokens(900L)
                .totalTokens(2100L)
                .avgTotalTokens(262L)
                .build()));

        List<AuditModelMetricEntity> items = service.queryModelMetrics("7d", "all");

        Assert.assertEquals(1, items.size());
        Assert.assertEquals("5301", items.get(0).getClientId());
        Assert.assertEquals(Long.valueOf(2100L), items.get(0).getTotalTokens());
        Assert.assertEquals(Long.valueOf(262L), items.get(0).getAvgTotalTokens());
    }
}
