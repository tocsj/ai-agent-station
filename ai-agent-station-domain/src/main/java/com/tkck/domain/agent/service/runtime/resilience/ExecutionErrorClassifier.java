package com.tkck.domain.agent.service.runtime.resilience;

import com.tkck.types.exception.AppException;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ExecutionErrorClassifier {

    public ExecutionErrorCode classify(Throwable throwable) {
        Throwable cause = unwrap(throwable);
        if (containsCause(throwable, InterruptedException.class)
                || containsCause(throwable, TimeoutException.class)
                || containsCause(throwable, SocketTimeoutException.class)) {
            return ExecutionErrorCode.STAGE_TIMEOUT;
        }
        if (cause instanceof ResourceAccessException || cause instanceof IOException) {
            String message = cause.getMessage() == null ? "" : cause.getMessage();
            if (message.contains("Request was interrupted") || message.contains("timed out")) {
                return ExecutionErrorCode.STAGE_TIMEOUT;
            }
        }
        if (cause instanceof AppException appException) {
            return ExecutionErrorCode.fromCode(appException.getCode());
        }
        if (cause instanceof TimeoutException || cause instanceof SocketTimeoutException) {
            return ExecutionErrorCode.STAGE_TIMEOUT;
        }
        if (cause instanceof IllegalArgumentException || cause instanceof MaxUploadSizeExceededException) {
            return ExecutionErrorCode.INVALID_REQUEST;
        }
        if (cause instanceof EmptyResultDataAccessException) {
            return ExecutionErrorCode.DATA_NOT_FOUND;
        }
        if (cause instanceof IllegalStateException && cause.getMessage() != null
                && cause.getMessage().contains("ResponseBodyEmitter has already completed")) {
            return ExecutionErrorCode.SSE_DISCONNECTED;
        }
        if (cause instanceof NonTransientAiException) {
            String message = cause.getMessage() == null ? "" : cause.getMessage();
            if (message.contains("429") || message.contains("5")) {
                return ExecutionErrorCode.MODEL_TRANSIENT_ERROR;
            }
            return ExecutionErrorCode.MODEL_REQUEST_ERROR;
        }
        return ExecutionErrorCode.SYSTEM_ERROR;
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof ExecutionException || current instanceof RuntimeException)
                && current.getCause() != null
                && current != current.getCause()) {
            current = current.getCause();
        }
        return current;
    }

    private boolean containsCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null && current.getCause() != current) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
