package com.tkck.domain.agent.service.runtime.structured;

import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.model.valobj.ExecutionMode;
import com.tkck.domain.agent.service.runtime.ExecutionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Order(10)
public class StructuredPlanExecuteHandler implements ExecutionHandler {

    public static final String LEGACY_AUTO_AGENT = "legacy_auto_agent";

    private final Map<String, StructuredWorkflowExecutor> workflowExecutorMap;

    public StructuredPlanExecuteHandler(List<StructuredWorkflowExecutor> workflowExecutors) {
        this.workflowExecutorMap = workflowExecutors.stream()
                .collect(Collectors.toMap(StructuredWorkflowExecutor::getTaskType, Function.identity()));
    }

    @Override
    public boolean supports(ExecuteCommandEntity command) {
        return command.getExecutionMode() == null
                || ExecutionMode.STRUCTURED_PLAN_EXECUTE.equals(command.getExecutionMode());
    }

    @Override
    public void execute(ExecuteCommandEntity command, ResponseBodyEmitter emitter) throws Exception {
        String taskType = StringUtils.hasText(command.getTaskType()) ? command.getTaskType() : LEGACY_AUTO_AGENT;
        StructuredWorkflowExecutor executor = workflowExecutorMap.get(resolveExecutorTaskType(taskType));
        if (executor == null) {
            throw new IllegalArgumentException("no structured workflow executor found for taskType=" + taskType);
        }
        executor.execute(command, emitter);
    }

    private String resolveExecutorTaskType(String taskType) {
        if ("resume_evaluation".equals(taskType) || "resume_interview".equals(taskType)) {
            return LEGACY_AUTO_AGENT;
        }
        return taskType;
    }
}
