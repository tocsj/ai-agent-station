package com.tkck.domain.audit.service;

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

import java.util.List;

public interface IAuditMonitoringService {

    void recordEvent(AuditEventEntity event);

    void startExecution(AuditExecutionMetricEntity executionMetric);

    void recordStep(AuditStepMetricEntity stepMetric);

    void recordLlmCall(AuditLlmCallMetricEntity llmCallMetric);

    void finishExecution(String traceId, String status, long totalDurationMs);

    AuditDashboardOverviewEntity queryOverview(String range, String taskType);

    List<AuditTaskTrendEntity> queryTaskTrend(Integer days, String taskType);

    List<AuditTaskTypeMetricEntity> queryTaskTypeMetrics(String range);

    List<AuditStepMetricSummaryEntity> queryStepMetrics(String taskType, String range);

    List<AuditModelMetricEntity> queryModelMetrics(String range, String taskType);

    List<AuditPublishChannelMetricEntity> queryPublishChannelMetrics(String range);

    List<AuditDocumentModeMetricEntity> queryDocumentModeMetrics(String range);

    AuditInterviewMetricEntity queryInterviewMetrics(String range);

    AuditEventPageEntity queryEvents(String taskType, String bizType, String eventType, String status, Integer page, Integer pageSize);

    AuditExecutionDetailEntity queryExecutionDetail(String traceId);

    List<AuditLlmCallMetricEntity> queryLlmCalls(String traceId);
}
