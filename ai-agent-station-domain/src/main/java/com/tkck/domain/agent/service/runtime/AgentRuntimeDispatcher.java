package com.tkck.domain.agent.service.runtime;

import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;

@Service
public class AgentRuntimeDispatcher {

    private final List<ExecutionHandler> executionHandlers;

    public AgentRuntimeDispatcher(List<ExecutionHandler> executionHandlers) {
        this.executionHandlers = executionHandlers;
    }

    public void dispatch(ExecuteCommandEntity command, ResponseBodyEmitter emitter) throws Exception {
        for (ExecutionHandler executionHandler : executionHandlers) {
            if (executionHandler.supports(command)) {
                executionHandler.execute(command, emitter);
                return;
            }
        }
        throw new IllegalArgumentException("no execution handler found for taskType=" + command.getTaskType()
                + ", mode=" + command.getExecutionMode());
    }
}
