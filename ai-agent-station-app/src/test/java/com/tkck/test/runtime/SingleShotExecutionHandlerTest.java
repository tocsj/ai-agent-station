package com.tkck.test.runtime;

import com.alibaba.fastjson.JSON;
import com.tkck.domain.agent.model.entity.AutoAgentExecuteResultEntity;
import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.model.valobj.ExecutionMode;
import com.tkck.domain.agent.service.runtime.singleshot.SingleShotExecutionHandler;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SingleShotExecutionHandlerTest {

    @Test
    public void shouldSupportSingleShotModeOnly() {
        SingleShotExecutionHandler handler = new SingleShotExecutionHandler();

        Assert.assertTrue(handler.supports(ExecuteCommandEntity.builder()
                .executionMode(ExecutionMode.SINGLE_SHOT)
                .build()));
        Assert.assertFalse(handler.supports(ExecuteCommandEntity.builder()
                .executionMode(ExecutionMode.STRUCTURED_PLAN_EXECUTE)
                .build()));
    }

    @Test
    public void shouldEmitErrorResultForUnsupportedStreamBinding() throws Exception {
        SingleShotExecutionHandler handler = new SingleShotExecutionHandler();
        CapturingEmitter emitter = new CapturingEmitter();

        handler.execute(ExecuteCommandEntity.builder()
                .executionMode(ExecutionMode.SINGLE_SHOT)
                .sessionId("single-001")
                .build(), emitter);

        Assert.assertEquals(1, emitter.events.size());
        String payload = emitter.events.get(0).replace("data: ", "").trim();
        AutoAgentExecuteResultEntity result = JSON.parseObject(payload, AutoAgentExecuteResultEntity.class);
        Assert.assertEquals("error", result.getType());
        Assert.assertEquals("single-001", result.getSessionId());
    }

    private static class CapturingEmitter extends ResponseBodyEmitter {
        private final List<String> events = new ArrayList<>();

        @Override
        public synchronized void send(Object object) throws IOException {
            events.add(String.valueOf(object));
        }
    }
}
