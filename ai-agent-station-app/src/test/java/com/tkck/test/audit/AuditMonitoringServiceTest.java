package com.tkck.test.audit;

import com.tkck.app.audit.AuditMonitoringServiceImpl;
import com.tkck.domain.audit.model.entity.AuditDashboardOverviewEntity;
import com.tkck.domain.audit.model.entity.AuditEventEntity;
import com.tkck.domain.audit.model.entity.AuditLlmCallMetricEntity;
import com.tkck.domain.audit.model.entity.AuditModelMetricEntity;
import com.tkck.domain.audit.model.entity.AuditStepMetricEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuditMonitoringServiceTest {

    @Test
    public void shouldRecordAuditEventAndStepMetric() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AuditMonitoringServiceImpl service = new AuditMonitoringServiceImpl();
        ReflectionTestUtils.setField(service, "mysqlJdbcTemplate", jdbcTemplate);

        service.recordEvent(AuditEventEntity.builder()
                .eventType("CONTENT_TASK_START")
                .bizType("content_automation")
                .bizId("21")
                .sessionId("content-21")
                .executionMode("STRUCTURED_PLAN_EXECUTE")
                .status("RUNNING")
                .location("ContentAutomationWorkflowExecutor#execute")
                .metadataJson("{}")
                .build());
        service.recordStep(AuditStepMetricEntity.builder()
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
                .retryCount(0)
                .timeoutFlag(false)
                .degradedFlag(false)
                .location("TopicPlannerNode#apply")
                .build());
        service.recordLlmCall(AuditLlmCallMetricEntity.builder()
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
                .build());

        verify(jdbcTemplate).update(eq("""
                INSERT INTO audit_event (
                    event_id, event_type, biz_type, biz_id, session_id, execution_mode,
                    operator_id, operator_name, request_uri, request_method, status,
                    error_code, error_message, location, metadata_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON))
                """), any(Object[].class));
        verify(jdbcTemplate).update(eq("""
                INSERT INTO agent_step_metric (
                    trace_id, task_id, session_id, step_no, step_name, stage, client_id, model_code,
                    status, duration_ms, retry_count, timeout_flag, degraded_flag,
                    error_code, error_message, location
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """), any(Object[].class));
        verify(jdbcTemplate).update(eq("""
                INSERT INTO agent_llm_call_metric (
                    call_id, trace_id, task_type, task_sub_type, task_id, session_id,
                    step_name, stage, client_id, model_code, status, duration_ms,
                    prompt_tokens, completion_tokens, total_tokens,
                    error_code, error_message, location
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """), any(Object[].class));
    }

    @Test
    public void shouldQueryOverviewMetrics() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AuditMonitoringServiceImpl service = new AuditMonitoringServiceImpl();
        ReflectionTestUtils.setField(service, "mysqlJdbcTemplate", jdbcTemplate);

        when(jdbcTemplate.queryForMap(any(String.class))).thenReturn(Map.ofEntries(
                Map.entry("task_total", 10),
                Map.entry("success_total", 8),
                Map.entry("failed_total", 1),
                Map.entry("running_total", 1),
                Map.entry("avg_duration_ms", 1500),
                Map.entry("timeout_total", 2),
                Map.entry("degraded_total", 3),
                Map.entry("model_calls", 6),
                Map.entry("prompt_tokens", 1000),
                Map.entry("completion_tokens", 700),
                Map.entry("total_tokens", 1700)
        ));
        when(jdbcTemplate.queryForList(any(String.class))).thenReturn(List.of(Map.of(
                "success_total", 4,
                "failed_total", 1
        )));

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
    public void shouldQueryModelMetrics() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AuditMonitoringServiceImpl service = new AuditMonitoringServiceImpl();
        ReflectionTestUtils.setField(service, "mysqlJdbcTemplate", jdbcTemplate);

        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(Map.of(
                "client_id", "5301",
                "model_code", "2007",
                "call_total", 8,
                "success_total", 7,
                "failed_total", 1,
                "avg_duration_ms", 3200,
                "prompt_tokens", 1200,
                "completion_tokens", 900,
                "total_tokens", 2100
        )));

        List<AuditModelMetricEntity> items = service.queryModelMetrics("7d", "all");

        Assert.assertEquals(1, items.size());
        Assert.assertEquals("5301", items.get(0).getClientId());
        Assert.assertEquals(Long.valueOf(2100L), items.get(0).getTotalTokens());
        Assert.assertEquals(Long.valueOf(262L), items.get(0).getAvgTotalTokens());
    }
}
