package com.tkck.domain.agent.service.runtime.resilience;

import com.tkck.types.exception.AppException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Function;

@Component
public class ExecutionResilienceCoordinator {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName("agent-resilience-" + thread.getId());
        return thread;
    });

    private final ExecutionTimeoutPolicy timeoutPolicy;
    private final ExecutionRetryPolicy retryPolicy;
    private final ExecutionErrorClassifier errorClassifier;

    public ExecutionResilienceCoordinator() {
        this(ExecutionTimeoutPolicy.defaults(), ExecutionRetryPolicy.defaults());
    }

    public ExecutionResilienceCoordinator(ExecutionTimeoutPolicy timeoutPolicy,
                                          ExecutionRetryPolicy retryPolicy) {
        this.timeoutPolicy = timeoutPolicy;
        this.retryPolicy = retryPolicy;
        this.errorClassifier = new ExecutionErrorClassifier();
    }

    public <T> ExecutionStageResult<T> execute(ExecutionStage stage,
                                               ExecutionFailureContext context,
                                               ThrowingSupplier<T> supplier,
                                               Function<ExecutionFailure, T> degradeFunction) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                T payload = callWithTimeout(stage, supplier);
                return ExecutionStageResult.success(payload, attempt);
            } catch (Throwable throwable) {
                ExecutionErrorCode errorCode = errorClassifier.classify(throwable);
                ExecutionFailure failure = new ExecutionFailure(
                        stage, context, errorCode, throwable, attempt, errorCode.isRetryable());
                if (retryPolicy.shouldRetry(errorCode, attempt)) {
                    sleepQuietly(retryPolicy.nextBackoffMillis(attempt));
                    continue;
                }
                if (degradeFunction != null) {
                    return ExecutionStageResult.degraded(degradeFunction.apply(failure), errorCode, attempt);
                }
                throw new AppException(errorCode.getCode(), errorCode.getMessage(), throwable);
            }
        }
    }

    private <T> T callWithTimeout(ExecutionStage stage, ThrowingSupplier<T> supplier) throws Exception {
        Duration timeout = timeoutPolicy.getTimeout(stage);
        Future<T> future = EXECUTOR.submit(supplier::get);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
