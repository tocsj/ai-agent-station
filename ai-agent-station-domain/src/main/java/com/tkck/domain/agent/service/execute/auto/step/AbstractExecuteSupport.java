package com.tkck.domain.agent.service.execute.auto.step;

import cn.bugstack.wrench.design.framework.tree.AbstractMultiThreadStrategyRouter;
import com.alibaba.fastjson.JSON;
import com.tkck.domain.agent.adapter.repository.IAgentRepository;
import com.tkck.domain.agent.model.entity.AutoAgentExecuteResultEntity;
import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.model.valobj.enums.AiAgentEnumVO;
import com.tkck.domain.agent.service.execute.auto.step.factory.DefaultAutoAgentExecuteStrategyFactory;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionFailure;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionFailureContext;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionResilienceCoordinator;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionStage;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionStageResult;
import com.tkck.domain.agent.service.runtime.resilience.ThrowingSupplier;
import com.tkck.domain.audit.model.entity.AuditLlmCallMetricEntity;
import com.tkck.domain.audit.model.entity.AuditStepMetricEntity;
import com.tkck.domain.audit.service.IAuditMonitoringService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractExecuteSupport extends AbstractMultiThreadStrategyRouter<ExecuteCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext, String> {

    private final Logger log = LoggerFactory.getLogger(AbstractExecuteSupport.class);

    @Resource
    protected ApplicationContext applicationContext;

    @Resource
    protected IAgentRepository repository;
    @Resource
    protected ExecutionResilienceCoordinator executionResilienceCoordinator;
    @Resource
    protected IAuditMonitoringService auditMonitoringService;

    public static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";
    public static final String CHAT_MEMORY_RETRIEVE_SIZE_KEY = "chat_memory_response_size";
    public static final String QA_FILTER_EXPRESSION_KEY = "qa_filter_expression";

    @Override
    protected void multiThread(ExecuteCommandEntity armoryCommandEntity,
                               DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext)
            throws ExecutionException, InterruptedException, TimeoutException {
    }

    protected ChatClient getChatClientByClientId(String clientId) {
        return getBean(AiAgentEnumVO.AI_CLIENT.getBeanName(clientId));
    }

    @SuppressWarnings("unchecked")
    protected <T> T getBean(String beanName) {
        return (T) applicationContext.getBean(beanName);
    }

    protected void sendSseResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                 AutoAgentExecuteResultEntity result) {
        try {
            ResponseBodyEmitter emitter = dynamicContext.getValue("emitter");
            if (emitter != null) {
                String sseData = "data: " + JSON.toJSONString(result) + "\n\n";
                SafeSendState safeSendState = invokeSafeSend(emitter, sseData);
                if (safeSendState == SafeSendState.NOT_SUPPORTED) {
                    emitter.send(sseData);
                }
            }
        } catch (IOException e) {
            log.error("send sse result failed: {}", e.getMessage(), e);
        } catch (IllegalStateException e) {
            log.warn("sse emitter already completed, skip current event: {}", e.getMessage());
        }
    }

    private SafeSendState invokeSafeSend(ResponseBodyEmitter emitter, String sseData) {
        try {
            Method method = emitter.getClass().getMethod("safeSend", Object.class);
            Object sent = method.invoke(emitter, sseData);
            if (!(sent instanceof Boolean)) {
                return SafeSendState.NOT_SUPPORTED;
            }
            return (Boolean) sent ? SafeSendState.SENT : SafeSendState.CLOSED;
        } catch (NoSuchMethodException e) {
            return SafeSendState.NOT_SUPPORTED;
        } catch (Exception e) {
            log.warn("invoke safeSend failed: {}", e.getMessage());
            return SafeSendState.CLOSED;
        }
    }

    private enum SafeSendState {
        SENT,
        CLOSED,
        NOT_SUPPORTED
    }

    protected String executeStage(ExecutionStage stage,
                                  ExecuteCommandEntity requestParameter,
                                  DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                  ThrowingSupplier<String> supplier,
                                  Function<ExecutionFailure, String> degradeFunction) throws Exception {
        ExecutionStageResult<String> result = executionResilienceCoordinator.execute(
                stage,
                new ExecutionFailureContext(requestParameter.getSessionId(), requestParameter.getAiAgentId()),
                supplier,
                degradeFunction
        );
        dynamicContext.setValue(stage.name() + "_result", result);
        if (result.isDegraded()) {
            dynamicContext.setValue(stage.name() + "_degraded", true);
            AutoAgentExecuteResultEntity errorResult = AutoAgentExecuteResultEntity.createErrorResult(
                    "阶段已降级: " + result.getErrorCode().getMessage(),
                    result.getErrorCode().getCode(),
                    stage.name(),
                    result.getErrorCode().isRetryable(),
                    true,
                    requestParameter.getSessionId()
            );
            sendSseResult(dynamicContext, errorResult);
        }
        return result.getPayload();
    }

    protected String callChatClientWithAudit(ExecuteCommandEntity requestParameter,
                                             DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                             ExecutionStage stage,
                                             String stepName,
                                             String clientId,
                                             String location,
                                             ThrowingSupplier<ChatResponse> supplier) throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            ChatResponse response = supplier.get();
            Usage usage = response == null || response.getMetadata() == null ? null : response.getMetadata().getUsage();
            String content = response == null || response.getResult() == null || response.getResult().getOutput() == null
                    ? ""
                    : safe(response.getResult().getOutput().getText());
            recordLlmCall(requestParameter, dynamicContext, stepName, stage, clientId, location, "SUCCESS", usage, System.currentTimeMillis() - startTime, null, null);
            return content;
        } catch (Exception e) {
            recordLlmCall(requestParameter, dynamicContext, stepName, stage, clientId, location, "FAILED", null, System.currentTimeMillis() - startTime, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    protected void recordStageMetric(ExecuteCommandEntity requestParameter,
                                     DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                     ExecutionStage stage,
                                     String stepName,
                                     String clientId,
                                     String location,
                                     long durationMs) {
        if (auditMonitoringService == null) {
            return;
        }
        ExecutionStageResult<?> result = dynamicContext.getValue(stage.name() + "_result");
        auditMonitoringService.recordStep(AuditStepMetricEntity.builder()
                .traceId(dynamicContext.getValue("traceId"))
                .taskId(resolveAuditTaskId(requestParameter, dynamicContext))
                .sessionId(requestParameter.getSessionId())
                .stepNo(stepNo(stage))
                .stepName(stepName)
                .stage(stage.name())
                .clientId(clientId)
                .modelCode(resolveModelCode(clientId))
                .status(result != null && result.isDegraded() ? "DEGRADED" : "SUCCESS")
                .durationMs(durationMs)
                .retryCount(result == null || result.getAttempts() <= 0 ? 0 : result.getAttempts() - 1)
                .timeoutFlag(result != null && result.getErrorCode() != null && "TIMEOUT".equalsIgnoreCase(result.getErrorCode().getCode()))
                .degradedFlag(result != null && result.isDegraded())
                .errorCode(result == null || result.getErrorCode() == null ? null : result.getErrorCode().getCode())
                .errorMessage(result == null || result.getErrorCode() == null ? null : result.getErrorCode().getMessage())
                .location(location)
                .build());
    }

    private void recordLlmCall(ExecuteCommandEntity requestParameter,
                               DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                               String stepName,
                               ExecutionStage stage,
                               String clientId,
                               String location,
                               String status,
                               Usage usage,
                               long durationMs,
                               String errorCode,
                               String errorMessage) {
        if (auditMonitoringService == null) {
            return;
        }
        auditMonitoringService.recordLlmCall(AuditLlmCallMetricEntity.builder()
                .traceId(dynamicContext.getValue("traceId"))
                .taskType(resolveAuditTaskType(requestParameter))
                .taskSubType(resolveAuditTaskSubType(requestParameter))
                .taskId(resolveAuditTaskId(requestParameter, dynamicContext))
                .sessionId(requestParameter.getSessionId())
                .stepName(stepName)
                .stage(stage.name())
                .clientId(clientId)
                .modelCode(resolveModelCode(clientId))
                .status(status)
                .durationMs(durationMs)
                .promptTokens(usage == null ? 0L : usage.getPromptTokens())
                .completionTokens(usage == null ? 0L : usage.getCompletionTokens())
                .totalTokens(usage == null ? 0L : usage.getTotalTokens())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .location(location)
                .build());
    }

    protected String resolveAuditTaskType(ExecuteCommandEntity requestParameter) {
        if (requestParameter.getTaskType() != null && !requestParameter.getTaskType().isBlank()) {
            return requestParameter.getTaskType();
        }
        if ("1001".equals(requestParameter.getAiAgentId())) {
            return "resume_evaluation";
        }
        if ("1002".equals(requestParameter.getAiAgentId())) {
            return "resume_interview";
        }
        return "legacy_auto_agent";
    }

    protected String resolveAuditTaskSubType(ExecuteCommandEntity requestParameter) {
        if ("resume_interview".equals(resolveAuditTaskType(requestParameter))) {
            return requestParameter.getInterviewSessionId() == null ? "start" : "round_answer";
        }
        return requestParameter.getSubType();
    }

    protected String resolveAuditTaskId(ExecuteCommandEntity requestParameter,
                                        DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) {
        if (requestParameter.getInterviewSessionId() != null) {
            return String.valueOf(requestParameter.getInterviewSessionId());
        }
        Object taskId = dynamicContext.getValue("taskId");
        if (taskId != null) {
            return String.valueOf(taskId);
        }
        return requestParameter.getSessionId();
    }

    protected String resolveModelCode(String clientId) {
        if ("5301".equals(clientId)) {
            return "2007";
        }
        if ("5302".equals(clientId)) {
            return "2008";
        }
        return clientId == null ? "" : clientId;
    }

    protected int stepNo(ExecutionStage stage) {
        return switch (stage) {
            case STEP1_ANALYZE -> 1;
            case STEP2_EXECUTE -> 2;
            case STEP3_VERIFY -> 3;
            case STEP4_SUMMARIZE -> 4;
            default -> 0;
        };
    }

    protected String safe(String value) {
        return value == null ? "" : value;
    }
}
