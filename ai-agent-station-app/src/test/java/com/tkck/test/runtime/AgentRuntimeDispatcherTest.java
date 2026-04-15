package com.tkck.test.runtime;

import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.model.valobj.ExecutionMode;
import com.tkck.domain.agent.service.runtime.AgentRuntimeDispatcher;
import com.tkck.domain.agent.service.runtime.ExecutionHandler;
import org.junit.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AgentRuntimeDispatcherTest {

    @Test
    public void should_dispatch_to_first_supported_handler() throws Exception {
        ExecutionHandler structuredHandler = mock(ExecutionHandler.class);
        ExecutionHandler singleShotHandler = mock(ExecutionHandler.class);
        AgentRuntimeDispatcher dispatcher = new AgentRuntimeDispatcher(List.of(structuredHandler, singleShotHandler));
        ExecuteCommandEntity command = ExecuteCommandEntity.builder()
                .executionMode(ExecutionMode.STRUCTURED_PLAN_EXECUTE)
                .taskType("legacy_auto_agent")
                .build();
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();

        when(structuredHandler.supports(any(ExecuteCommandEntity.class))).thenReturn(true);
        when(singleShotHandler.supports(any(ExecuteCommandEntity.class))).thenReturn(false);

        dispatcher.dispatch(command, emitter);

        verify(structuredHandler).execute(command, emitter);
        verify(singleShotHandler, never()).execute(any(), any());
    }
}
