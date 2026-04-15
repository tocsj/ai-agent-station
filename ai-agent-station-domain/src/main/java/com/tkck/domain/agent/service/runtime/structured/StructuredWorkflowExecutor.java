package com.tkck.domain.agent.service.runtime.structured;

import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

public interface StructuredWorkflowExecutor {

    String getTaskType();

    void execute(ExecuteCommandEntity command, ResponseBodyEmitter emitter) throws Exception;
}
