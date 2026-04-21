package com.tkck.test.runtime;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionErrorCode;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionFailureContext;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionResilienceCoordinator;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionRetryPolicy;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionStage;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionStageResult;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionTimeoutPolicy;
import com.tkck.types.exception.AppException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutionResilienceCoordinatorTest {

    @Test
    public void should_retry_retryable_stage_failure_and_return_success() {
        ExecutionResilienceCoordinator coordinator = new ExecutionResilienceCoordinator(
                ExecutionTimeoutPolicy.defaults(),
                ExecutionRetryPolicy.defaults()
        );
        AtomicInteger attempts = new AtomicInteger(0);

        ExecutionStageResult<String> result = coordinator.execute(
                ExecutionStage.STEP2_EXECUTE,
                new ExecutionFailureContext("session-1", "resume-eval"),
                () -> {
                    if (attempts.incrementAndGet() < 3) {
                        throw new AppException(ExecutionErrorCode.MODEL_TRANSIENT_ERROR.getCode(), "temporary model failure");
                    }
                    return "ok";
                },
                failure -> "fallback"
        );

        Assert.assertTrue(result.isSuccess());
        Assert.assertFalse(result.isDegraded());
        Assert.assertEquals("ok", result.getPayload());
        Assert.assertEquals(3, attempts.get());
    }

    @Test
    public void should_degrade_when_timeout_happens() {
        ExecutionResilienceCoordinator coordinator = new ExecutionResilienceCoordinator(
                ExecutionTimeoutPolicy.builder().timeout(ExecutionStage.STEP1_ANALYZE, Duration.ofMillis(50)).build(),
                ExecutionRetryPolicy.defaults()
        );

        ExecutionStageResult<String> result = coordinator.execute(
                ExecutionStage.STEP1_ANALYZE,
                new ExecutionFailureContext("session-2", "resume-eval"),
                () -> {
                    Thread.sleep(120);
                    return "never";
                },
                failure -> "timeout-fallback"
        );

        Assert.assertTrue(result.isSuccess());
        Assert.assertTrue(result.isDegraded());
        Assert.assertEquals("timeout-fallback", result.getPayload());
        Assert.assertEquals(ExecutionErrorCode.STAGE_TIMEOUT, result.getErrorCode());
    }

    @Test
    public void should_log_chinese_timeout_message_with_location() {
        Logger logger = (Logger) LoggerFactory.getLogger(ExecutionResilienceCoordinator.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            ExecutionResilienceCoordinator coordinator = new ExecutionResilienceCoordinator(
                    ExecutionTimeoutPolicy.builder().timeout(ExecutionStage.CONTENT_TOPIC_PLAN, Duration.ofMillis(50)).build(),
                    ExecutionRetryPolicy.defaults()
            );

            coordinator.execute(
                    ExecutionStage.CONTENT_TOPIC_PLAN,
                    new ExecutionFailureContext("content-1", "content_automation", "TopicPlannerNode.apply"),
                    () -> {
                        Thread.sleep(120);
                        return "never";
                    },
                    failure -> "timeout-fallback"
            );

            boolean matched = listAppender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("阶段执行降级")
                            && message.contains("位置=TopicPlannerNode.apply")
                            && message.contains("CONTENT_TOPIC_PLAN"));
            Assert.assertTrue(matched);
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    @Test
    public void should_not_retry_non_retryable_error() {
        ExecutionResilienceCoordinator coordinator = new ExecutionResilienceCoordinator(
                ExecutionTimeoutPolicy.defaults(),
                ExecutionRetryPolicy.defaults()
        );
        AtomicInteger attempts = new AtomicInteger(0);

        ExecutionStageResult<String> result = coordinator.execute(
                ExecutionStage.STEP3_VERIFY,
                new ExecutionFailureContext("session-3", "resume-eval"),
                () -> {
                    attempts.incrementAndGet();
                    throw new AppException(ExecutionErrorCode.INVALID_REQUEST.getCode(), "bad request");
                },
                failure -> "verify-fallback"
        );

        Assert.assertEquals(1, attempts.get());
        Assert.assertTrue(result.isSuccess());
        Assert.assertTrue(result.isDegraded());
        Assert.assertEquals("verify-fallback", result.getPayload());
        Assert.assertEquals(ExecutionErrorCode.INVALID_REQUEST, result.getErrorCode());
    }
}
