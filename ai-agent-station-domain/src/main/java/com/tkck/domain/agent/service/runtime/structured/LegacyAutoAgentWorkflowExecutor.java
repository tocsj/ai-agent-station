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
import com.tkck.domain.resume.service.IResumeWorkflowService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

@Slf4j
@Service
public class LegacyAutoAgentWorkflowExecutor implements StructuredWorkflowExecutor {

    @Resource
    private DefaultAutoAgentExecuteStrategyFactory defaultAutoAgentExecuteStrategyFactory;
    @Resource
    private IResumeWorkflowService resumeWorkflowService;
    @Resource
    private ExecutionResilienceCoordinator executionResilienceCoordinator;

    @Override
    public String getTaskType() {
        return StructuredPlanExecuteHandler.LEGACY_AUTO_AGENT;
    }

    @Override
    public void execute(ExecuteCommandEntity executeCommandEntity, ResponseBodyEmitter emitter) throws Exception {
        StrategyHandler<ExecuteCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext, String> executeHandler =
                defaultAutoAgentExecuteStrategyFactory.armoryStrategyHandler();

        DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext = new DefaultAutoAgentExecuteStrategyFactory.DynamicContext();
        dynamicContext.setMaxStep(executeCommandEntity.getMaxStep() != null ? executeCommandEntity.getMaxStep() : 3);
        dynamicContext.setExecutionHistory(new StringBuilder());
        dynamicContext.setCurrentTask(executeCommandEntity.getMessage());
        dynamicContext.setValue("emitter", emitter);

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

        AutoAgentExecuteResultEntity completeResult = AutoAgentExecuteResultEntity.createCompleteResult(executeCommandEntity.getSessionId());
        emitter.send("data: " + JSON.toJSONString(completeResult) + "\n\n");
    }
}
