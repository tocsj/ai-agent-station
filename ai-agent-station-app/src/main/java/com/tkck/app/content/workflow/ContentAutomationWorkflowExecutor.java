package com.tkck.app.content.workflow;

import com.alibaba.fastjson.JSON;
import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionFailure;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionFailureContext;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionResilienceCoordinator;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionStageResult;
import com.tkck.domain.agent.service.runtime.structured.StructuredWorkflowExecutor;
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

@Slf4j
@Service
public class ContentAutomationWorkflowExecutor implements StructuredWorkflowExecutor {

    @Resource
    private IContentAutomationService contentAutomationService;
    @Resource
    private ExecutionResilienceCoordinator executionResilienceCoordinator;

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
        ContentTaskEntity task = contentAutomationService.queryTask(command.getContentTaskId());
        contentAutomationService.markTaskRunning(task.getTaskId(), "topic_plan");
        ContentWorkflowContext context = ContentWorkflowContext.builder()
                .task(task)
                .emitter(emitter)
                .build();

        for (ContentWorkflowNode node : workflowNodes) {
            ExecutionStageResult<String> result = executionResilienceCoordinator.execute(
                    node.stage(),
                    new ExecutionFailureContext(task.getTaskCode(), getTaskType()),
                    () -> node.apply(context),
                    failure -> buildFallback(node, context, failure)
            );
            String output = result.getPayload();
            contentAutomationService.appendStep(
                    task.getTaskId(),
                    node.stepNo(),
                    node.stepName(),
                    result.isDegraded() ? "DEGRADED" : "COMPLETED",
                    output,
                    JSON.toJSONString(Map.of("stage", node.stage().name(), "degraded", result.isDegraded()))
            );
            applyTaskArtifacts(task.getTaskId(), node.stepName(), context, output);
            sendEvent(emitter, ContentTaskStreamEventEntity.builder()
                    .type("content_step")
                    .taskId(task.getTaskId())
                    .stepNo(node.stepNo())
                    .stepName(node.stepName())
                    .status(result.isDegraded() ? "DEGRADED" : "COMPLETED")
                    .content(output)
                    .completed(false)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }

        PublishResultEntity publishResult = context.getPublishResult();
        contentAutomationService.completeTask(
                task.getTaskId(),
                stringValue(context, "polished"),
                stringValue(context, "summary"),
                publishResult
        );
        sendEvent(emitter, ContentTaskStreamEventEntity.builder()
                .type("content_complete")
                .taskId(task.getTaskId())
                .status("COMPLETED")
                .content(stringValue(context, "summary"))
                .completed(true)
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

    private String buildFallback(ContentWorkflowNode node, ContentWorkflowContext context, ExecutionFailure failure) {
        return "stage=" + node.stepName() + "\nstatus=DEGRADED\nreason=" + failure.getErrorCode().getCode();
    }

    private void sendEvent(ResponseBodyEmitter emitter, ContentTaskStreamEventEntity event) throws Exception {
        emitter.send("data: " + JSON.toJSONString(event) + "\n\n");
    }

    private String stringValue(ContentWorkflowContext context, String key) {
        Object value = context.getValue(key);
        return value == null ? null : String.valueOf(value);
    }
}
