package com.tkck.domain.agent.service.runtime.open;

import com.alibaba.fastjson.JSON;
import com.tkck.domain.agent.model.entity.AutoAgentExecuteResultEntity;
import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.model.valobj.ExecutionMode;
import com.tkck.domain.agent.service.runtime.ExecutionHandler;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionErrorCode;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

@Service
@Order(30)
public class OpenPlanExecuteHandler implements ExecutionHandler {

    @Override
    public boolean supports(ExecuteCommandEntity command) {
        return ExecutionMode.OPEN_PLAN_EXECUTE.equals(command.getExecutionMode());
    }

    @Override
    public void execute(ExecuteCommandEntity command, ResponseBodyEmitter emitter) throws Exception {
        AutoAgentExecuteResultEntity errorResult = AutoAgentExecuteResultEntity.createErrorResult(
                "open plan execute is reserved for future tasks",
                ExecutionErrorCode.INVALID_REQUEST.getCode(),
                "ROOT",
                false,
                false,
                command.getSessionId()
        );
        emitter.send("data: " + JSON.toJSONString(errorResult) + "\n\n");
    }
}
