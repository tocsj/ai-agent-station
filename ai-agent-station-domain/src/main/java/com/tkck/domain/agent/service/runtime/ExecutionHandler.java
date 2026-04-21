package com.tkck.domain.agent.service.runtime;

import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

public interface ExecutionHandler {

    boolean supports(ExecuteCommandEntity command);

    void execute(ExecuteCommandEntity command, ResponseBodyEmitter emitter) throws Exception;
}
