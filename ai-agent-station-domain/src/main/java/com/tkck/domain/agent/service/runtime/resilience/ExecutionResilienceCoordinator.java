package com.tkck.domain.agent.service.runtime.resilience;

import com.tkck.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Function;

@Slf4j
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
                    log.warn("阶段执行失败，准备重试 | 场景={} | 阶段={} | 会话={} | 位置={} | 尝试={} | 错误码={} | 原因={}",
                            context.getScene(), stage.name(), context.getSessionId(), context.getLocation(),
                            attempt, errorCode.getCode(), simplifyMessage(throwable));
                    sleepQuietly(retryPolicy.nextBackoffMillis(attempt));
                    continue;
                }
                if (degradeFunction != null) {
                    log.error("阶段执行降级 | 场景={} | 阶段={} | 会话={} | 位置={} | 尝试={} | 错误码={} | 原因={}",
                            context.getScene(), stage.name(), context.getSessionId(), context.getLocation(),
                            attempt, errorCode.getCode(), simplifyMessage(throwable), throwable);
                    return ExecutionStageResult.degraded(degradeFunction.apply(failure), errorCode, attempt);
                }
                log.error("阶段执行失败，终止抛错 | 场景={} | 阶段={} | 会话={} | 位置={} | 尝试={} | 错误码={} | 原因={}",
                        context.getScene(), stage.name(), context.getSessionId(), context.getLocation(),
                        attempt, errorCode.getCode(), simplifyMessage(throwable), throwable);
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

    private String simplifyMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getClass().getSimpleName() + ": " + (current.getMessage() == null ? "" : current.getMessage());
    }
}
