package com.tkck.test.runtime;

import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.model.valobj.ExecutionMode;
import com.tkck.domain.agent.service.runtime.structured.StructuredPlanExecuteHandler;
import com.tkck.domain.agent.service.runtime.structured.StructuredWorkflowExecutor;
import org.junit.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StructuredPlanExecuteHandlerTest {

    @Test
    public void should_route_to_matching_structured_workflow_executor() throws Exception {
        StructuredWorkflowExecutor legacyExecutor = mock(StructuredWorkflowExecutor.class);
        StructuredWorkflowExecutor contentExecutor = mock(StructuredWorkflowExecutor.class);
        when(legacyExecutor.getTaskType()).thenReturn(StructuredPlanExecuteHandler.LEGACY_AUTO_AGENT);
        when(contentExecutor.getTaskType()).thenReturn("content_automation");
        StructuredPlanExecuteHandler handler = new StructuredPlanExecuteHandler(List.of(legacyExecutor, contentExecutor));
        ExecuteCommandEntity command = ExecuteCommandEntity.builder()
                .executionMode(ExecutionMode.STRUCTURED_PLAN_EXECUTE)
                .taskType("content_automation")
                .build();
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();

        handler.execute(command, emitter);

        verify(contentExecutor).execute(command, emitter);
    }

    @Test
    public void should_route_resume_task_types_to_legacy_auto_agent_executor() throws Exception {
        StructuredWorkflowExecutor legacyExecutor = mock(StructuredWorkflowExecutor.class);
        StructuredWorkflowExecutor contentExecutor = mock(StructuredWorkflowExecutor.class);
        when(legacyExecutor.getTaskType()).thenReturn(StructuredPlanExecuteHandler.LEGACY_AUTO_AGENT);
        when(contentExecutor.getTaskType()).thenReturn("content_automation");
        StructuredPlanExecuteHandler handler = new StructuredPlanExecuteHandler(List.of(legacyExecutor, contentExecutor));
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();
        ExecuteCommandEntity resumeEvaluation = ExecuteCommandEntity.builder()
                .executionMode(ExecutionMode.STRUCTURED_PLAN_EXECUTE)
                .taskType("resume_evaluation")
                .build();
        ExecuteCommandEntity resumeInterview = ExecuteCommandEntity.builder()
                .executionMode(ExecutionMode.STRUCTURED_PLAN_EXECUTE)
                .taskType("resume_interview")
                .build();

        handler.execute(resumeEvaluation, emitter);
        handler.execute(resumeInterview, emitter);

        verify(legacyExecutor).execute(resumeEvaluation, emitter);
        verify(legacyExecutor).execute(resumeInterview, emitter);
    }
}
