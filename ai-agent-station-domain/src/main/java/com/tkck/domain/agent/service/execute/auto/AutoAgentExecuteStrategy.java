package com.tkck.domain.agent.service.execute.auto;

import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.service.execute.IExecuteStrategy;
import com.tkck.domain.agent.service.runtime.AgentRuntimeDispatcher;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

@Slf4j
@Service
public class AutoAgentExecuteStrategy implements IExecuteStrategy {

    @Resource
    private AgentRuntimeDispatcher agentRuntimeDispatcher;

    @Override
    public void execute(ExecuteCommandEntity executeCommandEntity, ResponseBodyEmitter emitter) throws Exception {
        agentRuntimeDispatcher.dispatch(executeCommandEntity, emitter);
    }
}
