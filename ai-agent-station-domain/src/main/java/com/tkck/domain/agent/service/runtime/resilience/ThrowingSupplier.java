package com.tkck.domain.agent.service.runtime.resilience;

@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws Exception;
}
