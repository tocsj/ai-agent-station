package com.tkck.test.resume;

import com.tkck.domain.agent.service.execute.auto.step.Step1AnalyzerNode;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionErrorCode;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionFailure;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionFailureContext;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionStage;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class Step1AnalyzerNodeFallbackTest {

    @Test
    public void should_build_timeout_fallback_without_format_exception() {
        Step1AnalyzerNode node = new Step1AnalyzerNode();
        ExecutionFailure failure = new ExecutionFailure(
                ExecutionStage.STEP1_ANALYZE,
                new ExecutionFailureContext("session-x", "1002"),
                ExecutionErrorCode.STAGE_TIMEOUT,
                new RuntimeException("timeout"),
                3,
                true
        );

        String fallback = (String) ReflectionTestUtils.invokeMethod(node, "buildAnalysisFallback", failure);

        Assert.assertNotNull(fallback);
        Assert.assertTrue(fallback.contains("35%"));
        Assert.assertTrue(fallback.contains("CONTINUE"));
    }
}
