package com.tkck.app.audit;

import com.tkck.domain.audit.adapter.repository.IAuditMonitoringRepository;
import com.tkck.domain.audit.model.entity.AuditDashboardOverviewEntity;
import com.tkck.domain.audit.model.entity.AuditDocumentModeMetricEntity;
import com.tkck.domain.audit.model.entity.AuditEventEntity;
import com.tkck.domain.audit.model.entity.AuditEventPageEntity;
import com.tkck.domain.audit.model.entity.AuditExecutionDetailEntity;
import com.tkck.domain.audit.model.entity.AuditExecutionMetricEntity;
import com.tkck.domain.audit.model.entity.AuditInterviewMetricEntity;
import com.tkck.domain.audit.model.entity.AuditLlmCallMetricEntity;
import com.tkck.domain.audit.model.entity.AuditModelMetricEntity;
import com.tkck.domain.audit.model.entity.AuditPublishChannelMetricEntity;
import com.tkck.domain.audit.model.entity.AuditStepMetricEntity;
import com.tkck.domain.audit.model.entity.AuditStepMetricSummaryEntity;
import com.tkck.domain.audit.model.entity.AuditTaskTrendEntity;
import com.tkck.domain.audit.model.entity.AuditTaskTypeMetricEntity;
import com.tkck.domain.audit.service.IAuditMonitoringService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditMonitoringServiceImpl implements IAuditMonitoringService {

    @Resource
    private IAuditMonitoringRepository auditMonitoringRepository;

    @Override
    public void recordEvent(AuditEventEntity event) {
        auditMonitoringRepository.recordEvent(event);
    }

    @Override
    public void startExecution(AuditExecutionMetricEntity executionMetric) {
        auditMonitoringRepository.startExecution(executionMetric);
    }

    @Override
    public void recordStep(AuditStepMetricEntity stepMetric) {
        auditMonitoringRepository.recordStep(stepMetric);
    }

    @Override
    public void recordLlmCall(AuditLlmCallMetricEntity llmCallMetric) {
        auditMonitoringRepository.recordLlmCall(llmCallMetric);
    }

    @Override
    public void finishExecution(String traceId, String status, long totalDurationMs) {
        auditMonitoringRepository.finishExecution(traceId, status, totalDurationMs);
    }

    @Override
    public AuditDashboardOverviewEntity queryOverview(String range, String taskType) {
        return auditMonitoringRepository.queryOverview(range, taskType);
    }

    @Override
    public List<AuditTaskTrendEntity> queryTaskTrend(Integer days, String taskType) {
        return auditMonitoringRepository.queryTaskTrend(days, taskType);
    }

    @Override
    public List<AuditTaskTypeMetricEntity> queryTaskTypeMetrics(String range) {
        return auditMonitoringRepository.queryTaskTypeMetrics(range);
    }

    @Override
    public List<AuditStepMetricSummaryEntity> queryStepMetrics(String taskType, String range) {
        return auditMonitoringRepository.queryStepMetrics(taskType, range);
    }

    @Override
    public List<AuditModelMetricEntity> queryModelMetrics(String range, String taskType) {
        return auditMonitoringRepository.queryModelMetrics(range, taskType);
    }

    @Override
    public List<AuditPublishChannelMetricEntity> queryPublishChannelMetrics(String range) {
        return auditMonitoringRepository.queryPublishChannelMetrics(range);
    }

    @Override
    public List<AuditDocumentModeMetricEntity> queryDocumentModeMetrics(String range) {
        return auditMonitoringRepository.queryDocumentModeMetrics(range);
    }

    @Override
    public AuditInterviewMetricEntity queryInterviewMetrics(String range) {
        return auditMonitoringRepository.queryInterviewMetrics(range);
    }

    @Override
    public AuditEventPageEntity queryEvents(String taskType, String bizType, String eventType, String status, Integer page, Integer pageSize) {
        return auditMonitoringRepository.queryEvents(taskType, bizType, eventType, status, page, pageSize);
    }

    @Override
    public AuditExecutionDetailEntity queryExecutionDetail(String traceId) {
        return auditMonitoringRepository.queryExecutionDetail(traceId);
    }

    @Override
    public List<AuditLlmCallMetricEntity> queryLlmCalls(String traceId) {
        return auditMonitoringRepository.queryLlmCalls(traceId);
    }
}
