package com.tkck.trigger.http;

import com.tkck.api.response.Response;
import com.tkck.domain.audit.model.entity.AuditDashboardOverviewEntity;
import com.tkck.domain.audit.model.entity.AuditEventPageEntity;
import com.tkck.domain.audit.model.entity.AuditExecutionDetailEntity;
import com.tkck.domain.audit.model.entity.AuditInterviewMetricEntity;
import com.tkck.domain.audit.model.entity.AuditLlmCallMetricEntity;
import com.tkck.domain.audit.model.entity.AuditModelMetricEntity;
import com.tkck.domain.audit.model.entity.AuditDocumentModeMetricEntity;
import com.tkck.domain.audit.model.entity.AuditPublishChannelMetricEntity;
import com.tkck.domain.audit.model.entity.AuditStepMetricSummaryEntity;
import com.tkck.domain.audit.model.entity.AuditTaskTrendEntity;
import com.tkck.domain.audit.model.entity.AuditTaskTypeMetricEntity;
import com.tkck.domain.audit.service.IAuditMonitoringService;
import com.tkck.types.enums.ResponseCode;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class AuditMonitoringController {

    @Resource
    private IAuditMonitoringService auditMonitoringService;

    @GetMapping("/dashboard/overview")
    public Response<AuditDashboardOverviewEntity> overview(@RequestParam(value = "range", required = false, defaultValue = "today") String range,
                                                           @RequestParam(value = "taskType", required = false, defaultValue = "all") String taskType) {
        return success(auditMonitoringService.queryOverview(range, taskType));
    }

    @GetMapping("/dashboard/task-trend")
    public Response<List<AuditTaskTrendEntity>> taskTrend(@RequestParam(value = "days", required = false, defaultValue = "7") Integer days,
                                                          @RequestParam(value = "taskType", required = false, defaultValue = "all") String taskType) {
        return success(auditMonitoringService.queryTaskTrend(days, taskType));
    }

    @GetMapping("/dashboard/task-type")
    public Response<List<AuditTaskTypeMetricEntity>> taskType(@RequestParam(value = "range", required = false, defaultValue = "7d") String range) {
        return success(auditMonitoringService.queryTaskTypeMetrics(range));
    }

    @GetMapping("/dashboard/step-metrics")
    public Response<List<AuditStepMetricSummaryEntity>> stepMetrics(@RequestParam(value = "taskType", required = false, defaultValue = "content_automation") String taskType,
                                                                    @RequestParam(value = "range", required = false, defaultValue = "7d") String range) {
        return success(auditMonitoringService.queryStepMetrics(taskType, range));
    }

    @GetMapping("/dashboard/model-metrics")
    public Response<List<AuditModelMetricEntity>> modelMetrics(@RequestParam(value = "range", required = false, defaultValue = "7d") String range,
                                                               @RequestParam(value = "taskType", required = false, defaultValue = "all") String taskType) {
        return success(auditMonitoringService.queryModelMetrics(range, taskType));
    }

    @GetMapping("/dashboard/publish-channel")
    public Response<List<AuditPublishChannelMetricEntity>> publishChannel(@RequestParam(value = "range", required = false, defaultValue = "7d") String range) {
        return success(auditMonitoringService.queryPublishChannelMetrics(range));
    }

    @GetMapping("/dashboard/document-mode")
    public Response<List<AuditDocumentModeMetricEntity>> documentMode(@RequestParam(value = "range", required = false, defaultValue = "7d") String range) {
        return success(auditMonitoringService.queryDocumentModeMetrics(range));
    }

    @GetMapping("/dashboard/interview-metrics")
    public Response<AuditInterviewMetricEntity> interviewMetrics(@RequestParam(value = "range", required = false, defaultValue = "7d") String range) {
        return success(auditMonitoringService.queryInterviewMetrics(range));
    }

    @GetMapping("/events")
    public Response<AuditEventPageEntity> events(@RequestParam(value = "taskType", required = false, defaultValue = "all") String taskType,
                                                 @RequestParam(value = "bizType", required = false) String bizType,
                                                 @RequestParam(value = "eventType", required = false) String eventType,
                                                 @RequestParam(value = "status", required = false) String status,
                                                 @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                                 @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        return success(auditMonitoringService.queryEvents(taskType, bizType, eventType, status, page, pageSize));
    }

    @GetMapping("/execution/{traceId}")
    public Response<AuditExecutionDetailEntity> executionDetail(@PathVariable("traceId") String traceId) {
        return success(auditMonitoringService.queryExecutionDetail(traceId));
    }

    @GetMapping("/execution/{traceId}/llm-calls")
    public Response<List<AuditLlmCallMetricEntity>> llmCalls(@PathVariable("traceId") String traceId) {
        return success(auditMonitoringService.queryLlmCalls(traceId));
    }

    private <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(data)
                .build();
    }
}
