package com.tkck.app.audit;

import com.tkck.domain.audit.model.entity.AuditDashboardOverviewEntity;
import com.tkck.domain.audit.model.entity.AuditEventEntity;
import com.tkck.domain.audit.model.entity.AuditEventPageEntity;
import com.tkck.domain.audit.model.entity.AuditExecutionDetailEntity;
import com.tkck.domain.audit.model.entity.AuditExecutionMetricEntity;
import com.tkck.domain.audit.model.entity.AuditInterviewMetricEntity;
import com.tkck.domain.audit.model.entity.AuditLlmCallMetricEntity;
import com.tkck.domain.audit.model.entity.AuditModelMetricEntity;
import com.tkck.domain.audit.model.entity.AuditDocumentModeMetricEntity;
import com.tkck.domain.audit.model.entity.AuditPublishChannelMetricEntity;
import com.tkck.domain.audit.model.entity.AuditStepMetricEntity;
import com.tkck.domain.audit.model.entity.AuditStepMetricSummaryEntity;
import com.tkck.domain.audit.model.entity.AuditTaskTrendEntity;
import com.tkck.domain.audit.model.entity.AuditTaskTypeMetricEntity;
import com.tkck.domain.audit.service.IAuditMonitoringService;
import jakarta.annotation.Resource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditMonitoringServiceImpl implements IAuditMonitoringService {

    private static final String GLOBAL_OPERATOR_ID = "global";
    private static final String GLOBAL_OPERATOR_NAME = "全局账号";

    @Resource(name = "mysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbcTemplate;

    @Override
    public void recordEvent(AuditEventEntity event) {
        mysqlJdbcTemplate.update("""
                INSERT INTO audit_event (
                    event_id, event_type, biz_type, biz_id, session_id, execution_mode,
                    operator_id, operator_name, request_uri, request_method, status,
                    error_code, error_message, location, metadata_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON))
                """,
                defaultString(event.getEventId(), newId("audit")),
                event.getEventType(),
                event.getBizType(),
                event.getBizId(),
                event.getSessionId(),
                event.getExecutionMode(),
                defaultString(event.getOperatorId(), GLOBAL_OPERATOR_ID),
                defaultString(event.getOperatorName(), GLOBAL_OPERATOR_NAME),
                event.getRequestUri(),
                event.getRequestMethod(),
                event.getStatus(),
                event.getErrorCode(),
                event.getErrorMessage(),
                event.getLocation(),
                defaultString(event.getMetadataJson(), "{}")
        );
    }

    @Override
    public void startExecution(AuditExecutionMetricEntity executionMetric) {
        mysqlJdbcTemplate.update("""
                INSERT INTO agent_execution_metric (
                    trace_id, task_type, task_sub_type, task_id, session_id, execution_mode, status,
                    total_duration_ms, step_count, success_step_count, failed_step_count,
                    timeout_count, retry_count, degraded_count, model_calls
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                executionMetric.getTraceId(),
                executionMetric.getTaskType(),
                executionMetric.getTaskSubType(),
                executionMetric.getTaskId(),
                executionMetric.getSessionId(),
                executionMetric.getExecutionMode(),
                defaultString(executionMetric.getStatus(), "RUNNING"),
                defaultLong(executionMetric.getTotalDurationMs()),
                defaultInt(executionMetric.getStepCount()),
                defaultInt(executionMetric.getSuccessStepCount()),
                defaultInt(executionMetric.getFailedStepCount()),
                defaultInt(executionMetric.getTimeoutCount()),
                defaultInt(executionMetric.getRetryCount()),
                defaultInt(executionMetric.getDegradedCount()),
                defaultInt(executionMetric.getModelCalls())
        );
    }

    @Override
    public void recordStep(AuditStepMetricEntity stepMetric) {
        mysqlJdbcTemplate.update("""
                INSERT INTO agent_step_metric (
                    trace_id, task_id, session_id, step_no, step_name, stage, client_id, model_code,
                    status, duration_ms, retry_count, timeout_flag, degraded_flag,
                    error_code, error_message, location
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                stepMetric.getTraceId(),
                stepMetric.getTaskId(),
                stepMetric.getSessionId(),
                stepMetric.getStepNo(),
                stepMetric.getStepName(),
                stepMetric.getStage(),
                stepMetric.getClientId(),
                stepMetric.getModelCode(),
                stepMetric.getStatus(),
                defaultLong(stepMetric.getDurationMs()),
                defaultInt(stepMetric.getRetryCount()),
                Boolean.TRUE.equals(stepMetric.getTimeoutFlag()) ? 1 : 0,
                Boolean.TRUE.equals(stepMetric.getDegradedFlag()) ? 1 : 0,
                stepMetric.getErrorCode(),
                stepMetric.getErrorMessage(),
                stepMetric.getLocation()
        );
    }

    @Override
    public void recordLlmCall(AuditLlmCallMetricEntity llmCallMetric) {
        mysqlJdbcTemplate.update("""
                INSERT INTO agent_llm_call_metric (
                    call_id, trace_id, task_type, task_sub_type, task_id, session_id,
                    step_name, stage, client_id, model_code, status, duration_ms,
                    prompt_tokens, completion_tokens, total_tokens,
                    error_code, error_message, location
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                defaultString(llmCallMetric.getCallId(), newId("llm")),
                llmCallMetric.getTraceId(),
                llmCallMetric.getTaskType(),
                llmCallMetric.getTaskSubType(),
                llmCallMetric.getTaskId(),
                llmCallMetric.getSessionId(),
                llmCallMetric.getStepName(),
                llmCallMetric.getStage(),
                llmCallMetric.getClientId(),
                llmCallMetric.getModelCode(),
                llmCallMetric.getStatus(),
                defaultLong(llmCallMetric.getDurationMs()),
                defaultLong(llmCallMetric.getPromptTokens()),
                defaultLong(llmCallMetric.getCompletionTokens()),
                defaultLong(llmCallMetric.getTotalTokens()),
                llmCallMetric.getErrorCode(),
                llmCallMetric.getErrorMessage(),
                llmCallMetric.getLocation()
        );
    }

    @Override
    public void finishExecution(String traceId, String status, long totalDurationMs) {
        mysqlJdbcTemplate.update("""
                UPDATE agent_execution_metric a
                LEFT JOIN (
                    SELECT trace_id,
                           COUNT(1) AS step_count,
                           SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_step_count,
                           SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_step_count,
                           SUM(CASE WHEN timeout_flag = 1 THEN 1 ELSE 0 END) AS timeout_count,
                           SUM(retry_count) AS retry_count,
                           SUM(CASE WHEN degraded_flag = 1 THEN 1 ELSE 0 END) AS degraded_count,
                           SUM(CASE WHEN client_id IS NULL OR client_id = '' THEN 0 ELSE 1 END) AS model_calls
                    FROM agent_step_metric
                    WHERE trace_id = ?
                    GROUP BY trace_id
                ) s ON a.trace_id = s.trace_id
                LEFT JOIN (
                    SELECT trace_id,
                           COUNT(1) AS llm_call_total,
                           SUM(prompt_tokens) AS prompt_tokens,
                           SUM(completion_tokens) AS completion_tokens,
                           SUM(total_tokens) AS total_tokens
                    FROM agent_llm_call_metric
                    WHERE trace_id = ?
                    GROUP BY trace_id
                ) l ON a.trace_id = l.trace_id
                SET a.status = ?,
                    a.total_duration_ms = ?,
                    a.step_count = IFNULL(s.step_count, 0),
                    a.success_step_count = IFNULL(s.success_step_count, 0),
                    a.failed_step_count = IFNULL(s.failed_step_count, 0),
                    a.timeout_count = IFNULL(s.timeout_count, 0),
                    a.retry_count = IFNULL(s.retry_count, 0),
                    a.degraded_count = IFNULL(s.degraded_count, 0),
                    a.model_calls = IFNULL(l.llm_call_total, IFNULL(s.model_calls, 0)),
                    a.prompt_tokens = IFNULL(l.prompt_tokens, 0),
                    a.completion_tokens = IFNULL(l.completion_tokens, 0),
                    a.total_tokens = IFNULL(l.total_tokens, 0),
                    a.finish_time = NOW()
                WHERE a.trace_id = ?
                """,
                traceId,
                traceId,
                status,
                totalDurationMs,
                traceId
        );
    }

    @Override
    public AuditDashboardOverviewEntity queryOverview(String range, String taskType) {
        String normalizedRange = normalizeRange(range, "today");
        String normalizedTaskType = normalizeTaskType(taskType);
        String condition = timeCondition("create_time", normalizedRange) + taskTypeCondition("task_type", normalizedTaskType);
        Map<String, Object> row = mysqlJdbcTemplate.queryForMap("""
                SELECT COUNT(1) AS task_total,
                       SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_total,
                       SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_total,
                       SUM(CASE WHEN status = 'RUNNING' THEN 1 ELSE 0 END) AS running_total,
                       AVG(total_duration_ms) AS avg_duration_ms,
                       SUM(timeout_count) AS timeout_total,
                       SUM(degraded_count) AS degraded_total,
                       SUM(model_calls) AS model_calls,
                       SUM(prompt_tokens) AS prompt_tokens,
                       SUM(completion_tokens) AS completion_tokens,
                       SUM(total_tokens) AS total_tokens
                FROM agent_execution_metric
                WHERE %s
                """.formatted(condition));
        Map<String, Object> publishRow = ("all".equals(normalizedTaskType) || "content_automation".equals(normalizedTaskType))
                ? first(mysqlJdbcTemplate.queryForList("""
                SELECT SUM(CASE WHEN status IN ('DRAFT_SAVED', 'PUBLISHED', 'SUCCESS') THEN 1 ELSE 0 END) AS success_total,
                       SUM(CASE WHEN status IN ('FAILED', 'ERROR', 'BLOCKED') THEN 1 ELSE 0 END) AS failed_total
                FROM content_publish_record
                WHERE %s
                """.formatted(timeCondition("create_time", normalizedRange))))
                : Map.of("success_total", 0, "failed_total", 0);
        int taskTotal = intValue(row.get("task_total"));
        int successTotal = intValue(row.get("success_total"));
        return AuditDashboardOverviewEntity.builder()
                .range(normalizedRange)
                .taskType(normalizedTaskType)
                .taskTotal(taskTotal)
                .successTotal(successTotal)
                .failedTotal(intValue(row.get("failed_total")))
                .runningTotal(intValue(row.get("running_total")))
                .successRate(rate(successTotal, taskTotal))
                .avgDurationMs(longValue(row.get("avg_duration_ms")))
                .timeoutTotal(intValue(row.get("timeout_total")))
                .degradedTotal(intValue(row.get("degraded_total")))
                .modelCalls(intValue(row.get("model_calls")))
                .promptTokens(longValue(row.get("prompt_tokens")))
                .completionTokens(longValue(row.get("completion_tokens")))
                .totalTokens(longValue(row.get("total_tokens")))
                .publishSuccessTotal(intValue(publishRow.get("success_total")))
                .publishFailedTotal(intValue(publishRow.get("failed_total")))
                .build();
    }

    @Override
    public List<AuditTaskTrendEntity> queryTaskTrend(Integer days, String taskType) {
        int actualDays = days == null ? 7 : Math.max(1, Math.min(days, 30));
        String normalizedTaskType = normalizeTaskType(taskType);
        return mysqlJdbcTemplate.queryForList("""
                        SELECT DATE(create_time) AS date_value,
                               COUNT(1) AS task_total,
                               SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_total,
                               SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_total,
                               SUM(CASE WHEN status = 'RUNNING' THEN 1 ELSE 0 END) AS running_total,
                               SUM(prompt_tokens) AS prompt_tokens,
                               SUM(completion_tokens) AS completion_tokens,
                               SUM(total_tokens) AS total_tokens
                        FROM agent_execution_metric
                        WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
                          AND (? = 'all' OR task_type = ?)
                        GROUP BY DATE(create_time)
                        ORDER BY DATE(create_time) ASC
                        """,
                        actualDays - 1,
                        normalizedTaskType,
                        normalizedTaskType)
                .stream()
                .map(row -> AuditTaskTrendEntity.builder()
                        .date(String.valueOf(row.get("date_value")))
                        .taskTotal(intValue(row.get("task_total")))
                        .successTotal(intValue(row.get("success_total")))
                        .failedTotal(intValue(row.get("failed_total")))
                        .runningTotal(intValue(row.get("running_total")))
                        .promptTokens(longValue(row.get("prompt_tokens")))
                        .completionTokens(longValue(row.get("completion_tokens")))
                        .totalTokens(longValue(row.get("total_tokens")))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditTaskTypeMetricEntity> queryTaskTypeMetrics(String range) {
        String condition = timeCondition("create_time", normalizeRange(range, "7d"));
        return mysqlJdbcTemplate.queryForList("""
                        SELECT task_type,
                               COUNT(1) AS task_total,
                               SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_total,
                               SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_total,
                               SUM(prompt_tokens) AS prompt_tokens,
                               SUM(completion_tokens) AS completion_tokens,
                               SUM(total_tokens) AS total_tokens
                        FROM agent_execution_metric
                        WHERE %s
                        GROUP BY task_type
                        ORDER BY task_total DESC
                        """.formatted(condition))
                .stream()
                .map(row -> {
                    int total = intValue(row.get("task_total"));
                    int success = intValue(row.get("success_total"));
                    String taskType = stringValue(row.get("task_type"));
                    return AuditTaskTypeMetricEntity.builder()
                            .taskType(taskType)
                            .taskTypeName(taskTypeName(taskType))
                            .taskTotal(total)
                            .successTotal(success)
                            .failedTotal(intValue(row.get("failed_total")))
                            .successRate(rate(success, total))
                            .promptTokens(longValue(row.get("prompt_tokens")))
                            .completionTokens(longValue(row.get("completion_tokens")))
                            .totalTokens(longValue(row.get("total_tokens")))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditStepMetricSummaryEntity> queryStepMetrics(String taskType, String range) {
        String condition = timeCondition("s.create_time", normalizeRange(range, "7d"));
        String normalizedTaskType = defaultString(taskType, "content_automation");
        return mysqlJdbcTemplate.queryForList("""
                        SELECT s.step_name,
                               s.stage,
                               COUNT(1) AS execute_total,
                               SUM(CASE WHEN s.status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_total,
                               SUM(CASE WHEN s.status = 'FAILED' THEN 1 ELSE 0 END) AS failed_total,
                               SUM(CASE WHEN s.timeout_flag = 1 THEN 1 ELSE 0 END) AS timeout_total,
                               SUM(CASE WHEN s.degraded_flag = 1 THEN 1 ELSE 0 END) AS degraded_total,
                               AVG(s.duration_ms) AS avg_duration_ms,
                               COUNT(c.id) AS llm_call_total,
                               SUM(c.prompt_tokens) AS prompt_tokens,
                               SUM(c.completion_tokens) AS completion_tokens,
                               SUM(c.total_tokens) AS total_tokens,
                               AVG(c.total_tokens) AS avg_total_tokens
                        FROM agent_step_metric s
                        JOIN agent_execution_metric e ON s.trace_id = e.trace_id
                        LEFT JOIN agent_llm_call_metric c
                               ON s.trace_id = c.trace_id
                              AND s.step_name = c.step_name
                              AND s.stage = c.stage
                        WHERE e.task_type = ? AND %s
                        GROUP BY s.step_name, s.stage
                        ORDER BY MIN(s.step_no) ASC
                        """.formatted(condition),
                        normalizedTaskType)
                .stream()
                .map(row -> {
                    int total = intValue(row.get("execute_total"));
                    int success = intValue(row.get("success_total"));
                    String stepName = stringValue(row.get("step_name"));
                    return AuditStepMetricSummaryEntity.builder()
                            .stepName(stepName)
                            .stepNameLabel(stepNameLabel(stepName))
                            .stage(stringValue(row.get("stage")))
                            .executeTotal(total)
                            .successTotal(success)
                            .failedTotal(intValue(row.get("failed_total")))
                            .timeoutTotal(intValue(row.get("timeout_total")))
                            .degradedTotal(intValue(row.get("degraded_total")))
                            .avgDurationMs(longValue(row.get("avg_duration_ms")))
                            .successRate(rate(success, total))
                            .llmCallTotal(intValue(row.get("llm_call_total")))
                            .promptTokens(longValue(row.get("prompt_tokens")))
                            .completionTokens(longValue(row.get("completion_tokens")))
                            .totalTokens(longValue(row.get("total_tokens")))
                            .avgTotalTokens(longValue(row.get("avg_total_tokens")))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditModelMetricEntity> queryModelMetrics(String range, String taskType) {
        String condition = timeCondition("c.create_time", normalizeRange(range, "7d"));
        String normalizedTaskType = normalizeTaskType(taskType);
        return mysqlJdbcTemplate.queryForList("""
                        SELECT c.client_id,
                               c.model_code,
                               COUNT(1) AS call_total,
                               SUM(CASE WHEN c.status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_total,
                               SUM(CASE WHEN c.status <> 'SUCCESS' THEN 1 ELSE 0 END) AS failed_total,
                               AVG(c.duration_ms) AS avg_duration_ms,
                               SUM(c.prompt_tokens) AS prompt_tokens,
                               SUM(c.completion_tokens) AS completion_tokens,
                               SUM(c.total_tokens) AS total_tokens,
                               AVG(c.total_tokens) AS avg_total_tokens
                        FROM agent_llm_call_metric c
                        WHERE %s
                          AND (? = 'all' OR c.task_type = ?)
                        GROUP BY c.client_id, c.model_code
                        ORDER BY call_total DESC, total_tokens DESC
                        """.formatted(condition),
                        normalizedTaskType,
                        normalizedTaskType)
                .stream()
                .map(row -> {
                    long totalTokens = longValue(row.get("total_tokens"));
                    int callTotal = intValue(row.get("call_total"));
                    long avgTotalTokens = row.get("avg_total_tokens") == null
                            ? (callTotal <= 0 ? 0L : totalTokens / callTotal)
                            : longValue(row.get("avg_total_tokens"));
                    return AuditModelMetricEntity.builder()
                            .clientId(stringValue(row.get("client_id")))
                            .modelCode(stringValue(row.get("model_code")))
                            .callTotal(callTotal)
                            .successTotal(intValue(row.get("success_total")))
                            .failedTotal(intValue(row.get("failed_total")))
                            .avgDurationMs(longValue(row.get("avg_duration_ms")))
                            .promptTokens(longValue(row.get("prompt_tokens")))
                            .completionTokens(longValue(row.get("completion_tokens")))
                            .totalTokens(totalTokens)
                            .avgTotalTokens(avgTotalTokens)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditPublishChannelMetricEntity> queryPublishChannelMetrics(String range) {
        String condition = timeCondition("create_time", normalizeRange(range, "7d"));
        return mysqlJdbcTemplate.queryForList("""
                        SELECT channel_code,
                               COUNT(1) AS attempt_total,
                               SUM(CASE WHEN status IN ('DRAFT_SAVED', 'PUBLISHED', 'SUCCESS') THEN 1 ELSE 0 END) AS success_total,
                               SUM(CASE WHEN status IN ('FAILED', 'ERROR', 'BLOCKED') THEN 1 ELSE 0 END) AS failed_total
                        FROM content_publish_record
                        WHERE %s
                        GROUP BY channel_code
                        ORDER BY attempt_total DESC
                        """.formatted(condition))
                .stream()
                .map(row -> {
                    String channel = stringValue(row.get("channel_code"));
                    Map<String, Object> latest = latestPublishRecord(channel);
                    int total = intValue(row.get("attempt_total"));
                    int success = intValue(row.get("success_total"));
                    return AuditPublishChannelMetricEntity.builder()
                            .channel(channel)
                            .channelName(channelName(channel))
                            .attemptTotal(total)
                            .successTotal(success)
                            .failedTotal(intValue(row.get("failed_total")))
                            .successRate(rate(success, total))
                            .lastStatus(stringValue(latest.get("status")))
                            .lastMessage(stringValue(latest.get("error_message")))
                            .lastExternalUrl(stringValue(latest.get("external_url")))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditDocumentModeMetricEntity> queryDocumentModeMetrics(String range) {
        String condition = timeCondition("create_time", normalizeRange(range, "7d"));
        return mysqlJdbcTemplate.queryForList("""
                        SELECT task_sub_type,
                               COUNT(1) AS task_total,
                               SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_total,
                               SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_total,
                               SUM(prompt_tokens) AS prompt_tokens,
                               SUM(completion_tokens) AS completion_tokens,
                               SUM(total_tokens) AS total_tokens
                        FROM agent_execution_metric
                        WHERE task_type = 'document_workspace'
                          AND %s
                        GROUP BY task_sub_type
                        ORDER BY task_total DESC
                        """.formatted(condition))
                .stream()
                .map(row -> AuditDocumentModeMetricEntity.builder()
                        .mode(stringValue(row.get("task_sub_type")))
                        .modeName(documentModeName(stringValue(row.get("task_sub_type"))))
                        .taskTotal(intValue(row.get("task_total")))
                        .successTotal(intValue(row.get("success_total")))
                        .failedTotal(intValue(row.get("failed_total")))
                        .promptTokens(longValue(row.get("prompt_tokens")))
                        .completionTokens(longValue(row.get("completion_tokens")))
                        .totalTokens(longValue(row.get("total_tokens")))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public AuditInterviewMetricEntity queryInterviewMetrics(String range) {
        String condition = timeCondition("create_time", normalizeRange(range, "7d"));
        Map<String, Object> sessionRow = first(mysqlJdbcTemplate.queryForList("""
                SELECT COUNT(1) AS session_total,
                       SUM(CASE WHEN status = 'FINISHED' THEN 1 ELSE 0 END) AS completed_session_total,
                       SUM(CASE WHEN status IN ('ABORTED', 'FAILED') THEN 1 ELSE 0 END) AS aborted_session_total
                FROM resume_interview_session
                WHERE %s
                """.formatted(condition)));
        Map<String, Object> roundRow = first(mysqlJdbcTemplate.queryForList("""
                SELECT COUNT(1) AS round_total
                FROM resume_interview_round
                WHERE %s
                """.formatted(condition.replace("create_time", "create_time"))));
        Map<String, Object> tokenRow = first(mysqlJdbcTemplate.queryForList("""
                SELECT SUM(prompt_tokens) AS prompt_tokens,
                       SUM(completion_tokens) AS completion_tokens,
                       SUM(total_tokens) AS total_tokens
                FROM agent_execution_metric
                WHERE task_type = 'resume_interview'
                  AND %s
                """.formatted(condition)));
        int sessionTotal = intValue(sessionRow.get("session_total"));
        int roundTotal = intValue(roundRow.get("round_total"));
        return AuditInterviewMetricEntity.builder()
                .sessionTotal(sessionTotal)
                .completedSessionTotal(intValue(sessionRow.get("completed_session_total")))
                .abortedSessionTotal(intValue(sessionRow.get("aborted_session_total")))
                .roundTotal(roundTotal)
                .avgRoundsPerSession(sessionTotal == 0 ? 0.0 : Math.round(roundTotal * 100.0 / sessionTotal) / 100.0)
                .promptTokens(longValue(tokenRow.get("prompt_tokens")))
                .completionTokens(longValue(tokenRow.get("completion_tokens")))
                .totalTokens(longValue(tokenRow.get("total_tokens")))
                .build();
    }

    @Override
    public AuditEventPageEntity queryEvents(String taskType, String bizType, String eventType, String status, Integer page, Integer pageSize) {
        int actualPage = page == null ? 1 : Math.max(1, page);
        int actualPageSize = pageSize == null ? 20 : Math.max(1, Math.min(pageSize, 100));
        int offset = (actualPage - 1) * actualPageSize;
        List<Object> args = new ArrayList<>();
        String where = buildEventWhere(taskType, bizType, eventType, status, args);
        Long total = mysqlJdbcTemplate.queryForObject("SELECT COUNT(1) FROM audit_event WHERE " + where, Long.class, args.toArray());
        args.add(actualPageSize);
        args.add(offset);
        List<AuditEventEntity> items = mysqlJdbcTemplate.queryForList("""
                        SELECT * FROM audit_event
                        WHERE %s
                        ORDER BY id DESC
                        LIMIT ? OFFSET ?
                        """.formatted(where),
                        args.toArray())
                .stream()
                .map(this::toAuditEvent)
                .collect(Collectors.toList());
        return AuditEventPageEntity.builder()
                .page(actualPage)
                .pageSize(actualPageSize)
                .total(total == null ? 0L : total)
                .items(items)
                .build();
    }

    @Override
    public AuditExecutionDetailEntity queryExecutionDetail(String traceId) {
        Map<String, Object> row = mysqlJdbcTemplate.queryForMap("SELECT * FROM agent_execution_metric WHERE trace_id = ?", traceId);
        List<AuditLlmCallMetricEntity> llmCalls = queryLlmCalls(traceId);
        List<AuditStepMetricEntity> steps = mysqlJdbcTemplate.queryForList(
                        "SELECT * FROM agent_step_metric WHERE trace_id = ? ORDER BY step_no ASC, id ASC",
                        traceId)
                .stream()
                .map(step -> toStepMetric(step, llmCalls))
                .collect(Collectors.toList());
        return AuditExecutionDetailEntity.builder()
                .traceId(stringValue(row.get("trace_id")))
                .taskType(stringValue(row.get("task_type")))
                .taskId(stringValue(row.get("task_id")))
                .sessionId(stringValue(row.get("session_id")))
                .executionMode(stringValue(row.get("execution_mode")))
                .status(stringValue(row.get("status")))
                .totalDurationMs(longValue(row.get("total_duration_ms")))
                .stepCount(intValue(row.get("step_count")))
                .successStepCount(intValue(row.get("success_step_count")))
                .failedStepCount(intValue(row.get("failed_step_count")))
                .timeoutCount(intValue(row.get("timeout_count")))
                .retryCount(intValue(row.get("retry_count")))
                .degradedCount(intValue(row.get("degraded_count")))
                .modelCalls(intValue(row.get("model_calls")))
                .promptTokens(longValue(row.get("prompt_tokens")))
                .completionTokens(longValue(row.get("completion_tokens")))
                .totalTokens(longValue(row.get("total_tokens")))
                .createTime(stringValue(row.get("create_time")))
                .finishTime(stringValue(row.get("finish_time")))
                .steps(steps)
                .llmCalls(llmCalls)
                .build();
    }

    @Override
    public List<AuditLlmCallMetricEntity> queryLlmCalls(String traceId) {
        return mysqlJdbcTemplate.queryForList(
                        "SELECT * FROM agent_llm_call_metric WHERE trace_id = ? ORDER BY id ASC",
                        traceId)
                .stream()
                .map(this::toLlmCallMetric)
                .collect(Collectors.toList());
    }

    private String buildEventWhere(String taskType, String bizType, String eventType, String status, List<Object> args) {
        List<String> conditions = new ArrayList<>();
        conditions.add("1 = 1");
        if (taskType != null && !taskType.isBlank() && !"all".equalsIgnoreCase(taskType)) {
            conditions.add("biz_type = ?");
            args.add(taskType);
        }
        if (bizType != null && !bizType.isBlank()) {
            conditions.add("biz_type = ?");
            args.add(bizType);
        }
        if (eventType != null && !eventType.isBlank()) {
            conditions.add("event_type = ?");
            args.add(eventType);
        }
        if (status != null && !status.isBlank()) {
            conditions.add("status = ?");
            args.add(status);
        }
        return String.join(" AND ", conditions);
    }

    private Map<String, Object> latestPublishRecord(String channel) {
        try {
            return mysqlJdbcTemplate.queryForMap(
                    "SELECT * FROM content_publish_record WHERE channel_code = ? ORDER BY id DESC LIMIT 1",
                    channel);
        } catch (EmptyResultDataAccessException ex) {
            return Map.of();
        }
    }

    private AuditEventEntity toAuditEvent(Map<String, Object> row) {
        String eventType = stringValue(row.get("event_type"));
        return AuditEventEntity.builder()
                .id(longValue(row.get("id")))
                .eventId(stringValue(row.get("event_id")))
                .eventType(eventType)
                .eventTypeName(eventTypeName(eventType))
                .bizType(stringValue(row.get("biz_type")))
                .bizId(stringValue(row.get("biz_id")))
                .sessionId(stringValue(row.get("session_id")))
                .executionMode(stringValue(row.get("execution_mode")))
                .operatorId(stringValue(row.get("operator_id")))
                .operatorName(stringValue(row.get("operator_name")))
                .requestUri(stringValue(row.get("request_uri")))
                .requestMethod(stringValue(row.get("request_method")))
                .status(stringValue(row.get("status")))
                .errorCode(stringValue(row.get("error_code")))
                .errorMessage(stringValue(row.get("error_message")))
                .location(stringValue(row.get("location")))
                .metadataJson(stringValue(row.get("metadata_json")))
                .createTime(stringValue(row.get("create_time")))
                .build();
    }

    private AuditStepMetricEntity toStepMetric(Map<String, Object> row, List<AuditLlmCallMetricEntity> llmCalls) {
        String stepName = stringValue(row.get("step_name"));
        String stage = stringValue(row.get("stage"));
        List<AuditLlmCallMetricEntity> stepCalls = llmCalls.stream()
                .filter(call -> stepName.equals(call.getStepName()) && stage.equals(call.getStage()))
                .toList();
        return AuditStepMetricEntity.builder()
                .id(longValue(row.get("id")))
                .traceId(stringValue(row.get("trace_id")))
                .taskId(stringValue(row.get("task_id")))
                .sessionId(stringValue(row.get("session_id")))
                .stepNo(intValue(row.get("step_no")))
                .stepName(stepName)
                .stepNameLabel(stepNameLabel(stepName))
                .stage(stage)
                .clientId(stringValue(row.get("client_id")))
                .modelCode(stringValue(row.get("model_code")))
                .status(stringValue(row.get("status")))
                .durationMs(longValue(row.get("duration_ms")))
                .retryCount(intValue(row.get("retry_count")))
                .timeoutFlag(intValue(row.get("timeout_flag")) == 1)
                .degradedFlag(intValue(row.get("degraded_flag")) == 1)
                .llmCallTotal(stepCalls.size())
                .promptTokens(sumPromptTokens(stepCalls))
                .completionTokens(sumCompletionTokens(stepCalls))
                .totalTokens(sumTotalTokens(stepCalls))
                .errorCode(stringValue(row.get("error_code")))
                .errorMessage(stringValue(row.get("error_message")))
                .location(stringValue(row.get("location")))
                .createTime(stringValue(row.get("create_time")))
                .build();
    }

    private AuditLlmCallMetricEntity toLlmCallMetric(Map<String, Object> row) {
        return AuditLlmCallMetricEntity.builder()
                .id(longValue(row.get("id")))
                .callId(stringValue(row.get("call_id")))
                .traceId(stringValue(row.get("trace_id")))
                .taskType(stringValue(row.get("task_type")))
                .taskSubType(stringValue(row.get("task_sub_type")))
                .taskId(stringValue(row.get("task_id")))
                .sessionId(stringValue(row.get("session_id")))
                .stepName(stringValue(row.get("step_name")))
                .stage(stringValue(row.get("stage")))
                .clientId(stringValue(row.get("client_id")))
                .modelCode(stringValue(row.get("model_code")))
                .status(stringValue(row.get("status")))
                .durationMs(longValue(row.get("duration_ms")))
                .promptTokens(longValue(row.get("prompt_tokens")))
                .completionTokens(longValue(row.get("completion_tokens")))
                .totalTokens(longValue(row.get("total_tokens")))
                .errorCode(stringValue(row.get("error_code")))
                .errorMessage(stringValue(row.get("error_message")))
                .location(stringValue(row.get("location")))
                .createTime(stringValue(row.get("create_time")))
                .build();
    }

    private String normalizeRange(String range, String defaultRange) {
        if (range == null || range.isBlank()) {
            return defaultRange;
        }
        String value = range.trim().toLowerCase();
        if ("today".equals(value) || "7d".equals(value) || "30d".equals(value)) {
            return value;
        }
        return defaultRange;
    }

    private String normalizeTaskType(String taskType) {
        if (taskType == null || taskType.isBlank()) {
            return "all";
        }
        return taskType.trim().toLowerCase();
    }

    private String taskTypeCondition(String column, String taskType) {
        if ("all".equalsIgnoreCase(taskType)) {
            return "";
        }
        return " AND " + column + " = '" + taskType + "'";
    }

    private String timeCondition(String column, String range) {
        return switch (range) {
            case "today" -> column + " >= CURDATE()";
            case "30d" -> column + " >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
            default -> column + " >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
        };
    }

    private String taskTypeName(String taskType) {
        return switch (defaultString(taskType, "")) {
            case "content_automation" -> "内容自动发布";
            case "document_workspace" -> "文档知识助手";
            case "resume_evaluation" -> "简历评估";
            case "resume_interview" -> "模拟面试";
            case "legacy_auto_agent" -> "通用 Agent";
            default -> taskType;
        };
    }

    private String eventTypeName(String eventType) {
        return switch (defaultString(eventType, "")) {
            case "CONTENT_TASK_START" -> "内容任务开始";
            case "CONTENT_TASK_COMPLETE" -> "内容任务完成";
            case "CONTENT_TASK_FAILED" -> "内容任务失败";
            case "CONTENT_PUBLISH_EXECUTE" -> "内容发布执行";
            case "RESUME_EVALUATION_START" -> "简历评估开始";
            case "RESUME_EVALUATION_COMPLETE" -> "简历评估完成";
            case "RESUME_EVALUATION_FAILED" -> "简历评估失败";
            case "RESUME_INTERVIEW_START" -> "模拟面试开始";
            case "RESUME_INTERVIEW_COMPLETE" -> "模拟面试完成";
            case "RESUME_INTERVIEW_FAILED" -> "模拟面试失败";
            case "DOCUMENT_TASK_START" -> "文档任务开始";
            case "DOCUMENT_TASK_COMPLETE" -> "文档任务完成";
            case "DOCUMENT_TASK_FAILED" -> "文档任务失败";
            default -> eventType;
        };
    }

    private String stepNameLabel(String stepName) {
        return switch (defaultString(stepName, "")) {
            case "topic_plan" -> "选题规划";
            case "outline" -> "生成大纲";
            case "draft" -> "生成初稿";
            case "polish" -> "润色校验";
            case "compliance" -> "合规审查";
            case "publish_plan" -> "发布规划";
            case "publish_execute" -> "执行发布";
            case "publish_summary" -> "发布总结";
            case "analyze" -> "分析";
            case "execute" -> "执行";
            case "verify" -> "质检";
            case "summarize" -> "总结";
            case "ask" -> "文档提问";
            case "summary" -> "文档摘要";
            case "followup" -> "生成追问";
            case "quiz" -> "生成测验";
            case "interview_opening" -> "面试开场";
            default -> stepName;
        };
    }

    private String channelName(String channel) {
        return switch (defaultString(channel, "")) {
            case "mock" -> "Mock";
            case "juejin" -> "掘金";
            case "cnblogs" -> "博客园";
            case "devto" -> "Dev.to";
            default -> channel;
        };
    }

    private String documentModeName(String mode) {
        return switch (defaultString(mode, "")) {
            case "ask" -> "文档提问";
            case "summary" -> "文档摘要";
            case "followup" -> "生成追问";
            case "quiz" -> "生成测验";
            default -> mode;
        };
    }

    private Map<String, Object> first(List<Map<String, Object>> rows) {
        return rows == null || rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private int intValue(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private Long longValue(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private long sumPromptTokens(List<AuditLlmCallMetricEntity> calls) {
        return calls.stream().map(AuditLlmCallMetricEntity::getPromptTokens).filter(v -> v != null).mapToLong(Long::longValue).sum();
    }

    private long sumCompletionTokens(List<AuditLlmCallMetricEntity> calls) {
        return calls.stream().map(AuditLlmCallMetricEntity::getCompletionTokens).filter(v -> v != null).mapToLong(Long::longValue).sum();
    }

    private long sumTotalTokens(List<AuditLlmCallMetricEntity> calls) {
        return calls.stream().map(AuditLlmCallMetricEntity::getTotalTokens).filter(v -> v != null).mapToLong(Long::longValue).sum();
    }

    private Double rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round((numerator * 10000.0 / denominator)) / 100.0;
    }
}
