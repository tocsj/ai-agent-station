package com.tkck.test.runtime;

import com.tkck.domain.agent.service.runtime.resilience.ExecutionStage;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionTimeoutPolicy;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

public class ExecutionTimeoutPolicyTest {

    @Test
    public void shouldAllowLongerTimeoutForContentStrongModelStages() {
        ExecutionTimeoutPolicy policy = ExecutionTimeoutPolicy.defaults();

        Assert.assertEquals(Duration.ofSeconds(120), policy.getTimeout(ExecutionStage.CONTENT_TOPIC_PLAN));
        Assert.assertEquals(Duration.ofSeconds(120), policy.getTimeout(ExecutionStage.CONTENT_OUTLINE));
        Assert.assertEquals(Duration.ofSeconds(180), policy.getTimeout(ExecutionStage.CONTENT_DRAFT));
        Assert.assertEquals(Duration.ofSeconds(150), policy.getTimeout(ExecutionStage.CONTENT_POLISH));
        Assert.assertEquals(Duration.ofSeconds(60), policy.getTimeout(ExecutionStage.CONTENT_COMPLIANCE));
        Assert.assertEquals(Duration.ofSeconds(90), policy.getTimeout(ExecutionStage.CONTENT_PUBLISH_SUMMARY));
    }
}
