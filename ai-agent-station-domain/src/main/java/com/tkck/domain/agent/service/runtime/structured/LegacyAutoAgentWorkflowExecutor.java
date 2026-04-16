package com.tkck.domain.agent.service.runtime.structured;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.alibaba.fastjson.JSON;
import com.tkck.domain.agent.model.entity.AutoAgentExecuteResultEntity;
import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.service.execute.auto.step.factory.DefaultAutoAgentExecuteStrategyFactory;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionFailureContext;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionResilienceCoordinator;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionStage;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionStageResult;
import com.tkck.domain.audit.model.entity.AuditEventEntity;
import com.tkck.domain.audit.model.entity.AuditExecutionMetricEntity;
import com.tkck.domain.audit.service.IAuditMonitoringService;
import com.tkck.domain.resume.service.IResumeWorkflowService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class LegacyAutoAgentWorkflowExecutor implements StructuredWorkflowExecutor {

    @Resource
    private DefaultAutoAgentExecuteStrategyFactory defaultAutoAgentExecuteStrategyFactory;
    @Resource
    private IResumeWorkflowService resumeWorkflowService;
    @Resource
    private ExecutionResilienceCoordinator executionResilienceCoordinator;
    @Resource
    private IAuditMonitoringService auditMonitoringService;

    @Override
    public String getTaskType() {
        return StructuredPlanExecuteHandler.LEGACY_AUTO_AGENT;
    }

    @Override
    public void execute(ExecuteCommandEntity executeCommandEntity, ResponseBodyEmitter emitter) throws Exception {
        long startTime = System.currentTimeMillis();
        String traceId = "trace_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String taskType = resolveTaskType(executeCommandEntity);
        String taskId = resolveTaskId(executeCommandEntity);
        StrategyHandler<ExecuteCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext, String> executeHandler =
                defaultAutoAgentExecuteStrategyFactory.armoryStrategyHandler();

        DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext = new DefaultAutoAgentExecuteStrategyFactory.DynamicContext();
        dynamicContext.setMaxStep(executeCommandEntity.getMaxStep() != null ? executeCommandEntity.getMaxStep() : 3);
        dynamicContext.setExecutionHistory(new StringBuilder());
        dynamicContext.setCurrentTask(executeCommandEntity.getMessage());
        dynamicContext.setValue("emitter", emitter);
        dynamicContext.setValue("traceId", traceId);
        dynamicContext.setValue("taskId", taskId);

        auditMonitoringService.startExecution(AuditExecutionMetricEntity.builder()
                .traceId(traceId)
                .taskType(taskType)
                .taskSubType(resolveTaskSubType(executeCommandEntity, taskType))
                .taskId(taskId)
                .sessionId(executeCommandEntity.getSessionId())
                .executionMode(executeCommandEntity.getExecutionMode() == null ? "STRUCTURED_PLAN_EXECUTE" : executeCommandEntity.getExecutionMode().name())
                .status("RUNNING")
                .build());
        auditMonitoringService.recordEvent(AuditEventEntity.builder()
                .eventType("LEGACY_AUTO_AGENT_START")
                .bizType(taskType)
                .bizId(taskId)
                .sessionId(executeCommandEntity.getSessionId())
                .executionMode(executeCommandEntity.getExecutionMode() == null ? "STRUCTURED_PLAN_EXECUTE" : executeCommandEntity.getExecutionMode().name())
                .status("RUNNING")
                .location("LegacyAutoAgentWorkflowExecutor#execute")
                .metadataJson(JSON.toJSONString(Map.of("traceId", traceId, "aiAgentId", executeCommandEntity.getAiAgentId())))
                .build());

        try {
            ExecutionStageResult<String> stageResult = executionResilienceCoordinator.execute(
                    ExecutionStage.ROOT,
                    new ExecutionFailureContext(executeCommandEntity.getSessionId(), executeCommandEntity.getAiAgentId()),
                    () -> executeHandler.apply(executeCommandEntity, dynamicContext),
                    failure -> "agent execution degraded"
            );
            String apply = stageResult.getPayload();
            log.info("legacy auto agent execute result: {}", apply);

            if (executeCommandEntity.getInterviewSessionId() != null) {
                resumeWorkflowService.persistInterviewRoundResult(executeCommandEntity, dynamicContext);
            }
            if (executeCommandEntity.getResumeEvaluationTaskId() != null) {
                resumeWorkflowService.persistResumeEvaluationResult(
                        executeCommandEntity,
                        dynamicContext,
                        stageResult.isDegraded() ? "DEGRADED" : "SUCCESS",
                        null);
            }

            auditMonitoringService.finishExecution(traceId, stageResult.isDegraded() ? "DEGRADED" : "SUCCESS", System.currentTimeMillis() - startTime);
            auditMonitoringService.recordEvent(AuditEventEntity.builder()
                    .eventType("LEGACY_AUTO_AGENT_COMPLETE")
                    .bizType(taskType)
                    .bizId(taskId)
                    .sessionId(executeCommandEntity.getSessionId())
                    .executionMode(executeCommandEntity.getExecutionMode() == null ? "STRUCTURED_PLAN_EXECUTE" : executeCommandEntity.getExecutionMode().name())
                    .status(stageResult.isDegraded() ? "DEGRADED" : "SUCCESS")
                    .location("LegacyAutoAgentWorkflowExecutor#execute")
                    .metadataJson(JSON.toJSONString(Map.of("traceId", traceId)))
                    .build());
        } catch (Exception e) {
            if (executeCommandEntity.getResumeEvaluationTaskId() != null) {
                resumeWorkflowService.persistResumeEvaluationResult(executeCommandEntity, dynamicContext, "FAILED", e.getMessage());
            }
            auditMonitoringService.finishExecution(traceId, "FAILED", System.currentTimeMillis() - startTime);
            auditMonitoringService.recordEvent(AuditEventEntity.builder()
                    .eventType("LEGACY_AUTO_AGENT_FAILED")
                    .bizType(taskType)
                    .bizId(taskId)
                    .sessionId(executeCommandEntity.getSessionId())
                    .executionMode(executeCommandEntity.getExecutionMode() == null ? "STRUCTURED_PLAN_EXECUTE" : executeCommandEntity.getExecutionMode().name())
                    .status("FAILED")
                    .errorCode(e.getClass().getSimpleName())
                    .errorMessage(e.getMessage())
                    .location("LegacyAutoAgentWorkflowExecutor#execute")
                    .metadataJson(JSON.toJSONString(Map.of("traceId", traceId)))
                    .build());
            throw e;
        }

        AutoAgentExecuteResultEntity completeResult = AutoAgentExecuteResultEntity.createCompleteResult(executeCommandEntity.getSessionId());
        safeSend(emitter, "data: " + JSON.toJSONString(completeResult) + "\n\n");
    }

    private String resolveTaskType(ExecuteCommandEntity executeCommandEntity) {
        if (executeCommandEntity.getTaskType() != null && !executeCommandEntity.getTaskType().isBlank()) {
            return executeCommandEntity.getTaskType();
        }
        if ("1001".equals(executeCommandEntity.getAiAgentId())) {
            return "resume_evaluation";
        }
        if ("1002".equals(executeCommandEntity.getAiAgentId())) {
            return "resume_interview";
        }
        return StructuredPlanExecuteHandler.LEGACY_AUTO_AGENT;
    }

    private String resolveTaskId(ExecuteCommandEntity executeCommandEntity) {
        if (executeCommandEntity.getInterviewSessionId() != null) {
            return String.valueOf(executeCommandEntity.getInterviewSessionId());
        }
        return executeCommandEntity.getSessionId();
    }

    private String resolveTaskSubType(ExecuteCommandEntity executeCommandEntity, String taskType) {
        if ("resume_interview".equals(taskType)) {
            return executeCommandEntity.getInterviewSessionId() == null ? "start" : "round_answer";
        }
        return executeCommandEntity.getSubType();
    }

    private void safeSend(ResponseBodyEmitter emitter, String payload) throws Exception {
        try {
            Method method = emitter.getClass().getMethod("safeSend", Object.class);
            Object sent = method.invoke(emitter, payload);
            if (sent instanceof Boolean) {
                if ((Boolean) sent) {
                    return;
                }
                log.info("sse emitter already disconnected, skip completion event");
                return;
            }
        } catch (NoSuchMethodException ignored) {
            // fallback to regular emitter send
        }
        emitter.send(payload);
    }
}
