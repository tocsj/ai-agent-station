package com.tkck.domain.agent.service.runtime.singleshot;

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
@Order(20)
public class SingleShotExecutionHandler implements ExecutionHandler {

    @Override
    public boolean supports(ExecuteCommandEntity command) {
        return ExecutionMode.SINGLE_SHOT.equals(command.getExecutionMode());
    }

    @Override
    public void execute(ExecuteCommandEntity command, ResponseBodyEmitter emitter) throws Exception {
        AutoAgentExecuteResultEntity errorResult = AutoAgentExecuteResultEntity.createErrorResult(
                "single shot runtime is not bound to this stream endpoint",
                ExecutionErrorCode.INVALID_REQUEST.getCode(),
                "ROOT",
                false,
                false,
                command.getSessionId()
        );
        emitter.send("data: " + JSON.toJSONString(errorResult) + "\n\n");
    }
}
