package com.tkck.app.content.workflow;

import com.alibaba.fastjson.JSON;
import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionFailure;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionFailureContext;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionResilienceCoordinator;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionStageResult;
import com.tkck.domain.agent.service.runtime.structured.StructuredWorkflowExecutor;
import com.tkck.domain.audit.model.entity.AuditEventEntity;
import com.tkck.domain.audit.model.entity.AuditExecutionMetricEntity;
import com.tkck.domain.audit.model.entity.AuditStepMetricEntity;
import com.tkck.domain.audit.service.IAuditMonitoringService;
import com.tkck.domain.content.model.entity.ContentTaskEntity;
import com.tkck.domain.content.model.entity.ContentTaskStreamEventEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import com.tkck.domain.content.service.IContentAutomationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ContentAutomationWorkflowExecutor implements StructuredWorkflowExecutor {

    @Resource
    private IContentAutomationService contentAutomationService;
    @Resource
    private ExecutionResilienceCoordinator executionResilienceCoordinator;
    @Resource
    private IAuditMonitoringService auditMonitoringService;

    private final List<ContentWorkflowNode> workflowNodes;

    public ContentAutomationWorkflowExecutor(List<ContentWorkflowNode> workflowNodes) {
        this.workflowNodes = workflowNodes.stream()
                .sorted((a, b) -> Integer.compare(a.stepNo(), b.stepNo()))
                .toList();
    }

    @Override
    public String getTaskType() {
        return "content_automation";
    }

    @Override
    public void execute(ExecuteCommandEntity command, ResponseBodyEmitter emitter) throws Exception {
        if (command.getContentTaskId() == null) {
            throw new IllegalArgumentException("contentTaskId is required");
        }
        long startTime = System.currentTimeMillis();
        String traceId = "trace_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        ContentTaskEntity task = contentAutomationService.queryTask(command.getContentTaskId());
        String executionMode = command.getExecutionMode() == null ? null : command.getExecutionMode().name();

        auditMonitoringService.startExecution(AuditExecutionMetricEntity.builder()
                .traceId(traceId)
                .taskType(getTaskType())
                .taskId(String.valueOf(task.getTaskId()))
                .sessionId(command.getSessionId())
                .executionMode(executionMode)
                .status("RUNNING")
                .build());
        auditMonitoringService.recordEvent(AuditEventEntity.builder()
                .eventType("CONTENT_TASK_START")
                .bizType(getTaskType())
                .bizId(String.valueOf(task.getTaskId()))
                .sessionId(command.getSessionId())
                .executionMode(executionMode)
                .status("RUNNING")
                .location("ContentAutomationWorkflowExecutor#execute")
                .metadataJson(JSON.toJSONString(Map.of("traceId", traceId, "taskCode", task.getTaskCode(), "channel", task.getChannel())))
                .build());

        contentAutomationService.markTaskRunning(task.getTaskId(), "topic_plan");
        ContentWorkflowContext context = ContentWorkflowContext.builder()
                .task(task)
                .emitter(emitter)
                .build();
        context.setValue("traceId", traceId);
        context.setValue("sessionId", command.getSessionId());

        try {
            for (ContentWorkflowNode node : workflowNodes) {
                executeNode(command, traceId, task, context, emitter, node);
            }

            PublishResultEntity publishResult = context.getPublishResult();
            contentAutomationService.completeTask(
                    task.getTaskId(),
                    stringValue(context, "polished"),
                    stringValue(context, "summary"),
                    publishResult
            );
            auditMonitoringService.finishExecution(traceId, "SUCCESS", System.currentTimeMillis() - startTime);
            auditMonitoringService.recordEvent(AuditEventEntity.builder()
                    .eventType("CONTENT_TASK_COMPLETE")
                    .bizType(getTaskType())
                    .bizId(String.valueOf(task.getTaskId()))
                    .sessionId(command.getSessionId())
                    .executionMode(executionMode)
                    .status("SUCCESS")
                    .location("ContentAutomationWorkflowExecutor#execute")
                    .metadataJson(JSON.toJSONString(Map.of("traceId", traceId, "publishStatus", publishResult == null ? "" : publishResult.getStatus())))
                    .build());
            sendEvent(emitter, ContentTaskStreamEventEntity.builder()
                    .type("content_complete")
                    .taskId(task.getTaskId())
                    .status("COMPLETED")
                    .content(stringValue(context, "summary"))
                    .completed(true)
                    .timestamp(System.currentTimeMillis())
                    .build());
        } catch (Exception ex) {
            auditMonitoringService.finishExecution(traceId, "FAILED", System.currentTimeMillis() - startTime);
            auditMonitoringService.recordEvent(AuditEventEntity.builder()
                    .eventType("CONTENT_TASK_FAILED")
                    .bizType(getTaskType())
                    .bizId(String.valueOf(task.getTaskId()))
                    .sessionId(command.getSessionId())
                    .executionMode(executionMode)
                    .status("FAILED")
                    .errorCode(ex.getClass().getSimpleName())
                    .errorMessage(ex.getMessage())
                    .location("ContentAutomationWorkflowExecutor#execute")
                    .metadataJson(JSON.toJSONString(Map.of("traceId", traceId)))
                    .build());
            throw ex;
        }
    }

    private void executeNode(ExecuteCommandEntity command,
                             String traceId,
                             ContentTaskEntity task,
                             ContentWorkflowContext context,
                             ResponseBodyEmitter emitter,
                             ContentWorkflowNode node) throws Exception {
        long nodeStart = System.currentTimeMillis();
        String location = node.getClass().getSimpleName() + "#apply";
        log.info("内容工作流开始执行节点, taskId={}, stepNo={}, stepName={}, stage={}, location={}",
                task.getTaskId(), node.stepNo(), node.stepName(), node.stage().name(), location);
        ExecutionStageResult<String> result = executionResilienceCoordinator.execute(
                node.stage(),
                new ExecutionFailureContext(task.getTaskCode(), getTaskType(), location),
                () -> node.apply(context),
                failure -> buildFallback(node, failure)
        );
        String output = result.getPayload();
        String stepStatus = result.isDegraded() ? "DEGRADED" : "COMPLETED";
        log.info("内容工作流节点执行结束, taskId={}, stepNo={}, stepName={}, status={}, outputLength={}",
                task.getTaskId(), node.stepNo(), node.stepName(), stepStatus, output == null ? 0 : output.length());
        contentAutomationService.appendStep(
                task.getTaskId(),
                node.stepNo(),
                node.stepName(),
                stepStatus,
                output,
                JSON.toJSONString(Map.of("stage", node.stage().name(), "degraded", result.isDegraded(), "traceId", traceId))
        );
        auditMonitoringService.recordStep(AuditStepMetricEntity.builder()
                .traceId(traceId)
                .taskId(String.valueOf(task.getTaskId()))
                .sessionId(command.getSessionId())
                .stepNo(node.stepNo())
                .stepName(node.stepName())
                .stage(node.stage().name())
                .clientId(node.clientId())
                .modelCode(modelCode(node.clientId()))
                .status(result.isDegraded() ? "DEGRADED" : "SUCCESS")
                .durationMs(System.currentTimeMillis() - nodeStart)
                .retryCount(result.getAttempts() <= 0 ? 0 : result.getAttempts() - 1)
                .timeoutFlag(result.getErrorCode() != null && "TIMEOUT".equalsIgnoreCase(result.getErrorCode().getCode()))
                .degradedFlag(result.isDegraded())
                .errorCode(result.getErrorCode() == null ? null : result.getErrorCode().getCode())
                .errorMessage(result.getErrorCode() == null ? null : result.getErrorCode().getMessage())
                .location(location)
                .build());
        applyTaskArtifacts(task.getTaskId(), node.stepName(), context, output);
        sendEvent(emitter, ContentTaskStreamEventEntity.builder()
                .type("content_step")
                .taskId(task.getTaskId())
                .stepNo(node.stepNo())
                .stepName(node.stepName())
                .status(stepStatus)
                .content(output)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .build());
    }

    private void applyTaskArtifacts(Long taskId, String stepName, ContentWorkflowContext context, String output) {
        switch (stepName) {
            case "topic_plan" -> {
                String title = output.contains("\n") ? output.substring(0, output.indexOf('\n')).trim() : output.trim();
                contentAutomationService.updateTaskArtifact(taskId, "title", title, stepName);
            }
            case "outline" -> contentAutomationService.updateTaskArtifact(taskId, "outline_text", output, stepName);
            case "draft" -> contentAutomationService.updateTaskArtifact(taskId, "draft_content", output, stepName);
            case "polish" -> contentAutomationService.updateTaskArtifact(taskId, "final_content", output, stepName);
            case "compliance" -> contentAutomationService.updateTaskArtifact(taskId, "compliance_result", output, stepName);
            case "publish_summary" -> contentAutomationService.updateTaskArtifact(taskId, "summary_text", output, stepName);
            default -> {
            }
        }
    }

    private String buildFallback(ContentWorkflowNode node, ExecutionFailure failure) {
        return "阶段=" + node.stepName()
                + "\n状态=DEGRADED"
                + "\n错误码=" + failure.getErrorCode().getCode()
                + "\n说明=" + failure.getErrorCode().getMessage()
                + "\n位置=" + node.getClass().getSimpleName() + "#apply";
    }

    private void sendEvent(ResponseBodyEmitter emitter, ContentTaskStreamEventEntity event) throws Exception {
        emitter.send("data: " + JSON.toJSONString(event) + "\n\n");
    }

    private String stringValue(ContentWorkflowContext context, String key) {
        Object value = context.getValue(key);
        return value == null ? null : String.valueOf(value);
    }

    private String modelCode(String clientId) {
        if ("5301".equals(clientId)) {
            return "2007";
        }
        if ("5302".equals(clientId)) {
            return "2008";
        }
        return "";
    }
}
