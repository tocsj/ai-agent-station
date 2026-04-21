package com.tkck.test.runtime;

import com.tkck.domain.agent.service.runtime.resilience.ExecutionErrorClassifier;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionErrorCode;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;

public class ExecutionErrorClassifierTest {

    @Test
    public void should_classify_interrupted_http_request_as_stage_timeout() {
        ExecutionErrorClassifier classifier = new ExecutionErrorClassifier();
        InterruptedException interruptedException = new InterruptedException();
        IOException ioException = new IOException("Request was interrupted: null", interruptedException);
        ResourceAccessException resourceAccessException = new ResourceAccessException(
                "I/O error on POST request for chat/completions",
                ioException
        );

        ExecutionErrorCode errorCode = classifier.classify(resourceAccessException);

        Assert.assertEquals(ExecutionErrorCode.STAGE_TIMEOUT, errorCode);
    }
}
