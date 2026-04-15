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
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
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
                if (!invokeSafeSend(emitter, sseData)) {
                    emitter.send(sseData);
                }
            }
        } catch (IOException e) {
            log.error("send sse result failed: {}", e.getMessage(), e);
        } catch (IllegalStateException e) {
            log.warn("sse emitter already completed, skip current event: {}", e.getMessage());
        }
    }

    private boolean invokeSafeSend(ResponseBodyEmitter emitter, String sseData) {
        try {
            Method method = emitter.getClass().getMethod("safeSend", Object.class);
            Object sent = method.invoke(emitter, sseData);
            return sent instanceof Boolean && (Boolean) sent;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (Exception e) {
            log.warn("invoke safeSend failed: {}", e.getMessage());
            return true;
        }
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
}
