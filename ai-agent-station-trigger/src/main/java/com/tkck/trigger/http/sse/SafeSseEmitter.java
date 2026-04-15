package com.tkck.trigger.http.sse;

import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SafeSseEmitter extends ResponseBodyEmitter {

    private final AtomicBoolean closed = new AtomicBoolean(false);

    public SafeSseEmitter(Long timeout) {
        super(timeout);
        onCompletion(() -> closed.set(true));
        onTimeout(() -> closed.set(true));
        onError(ex -> closed.set(true));
    }

    public boolean safeSend(Object object) {
        if (closed.get()) {
            return false;
        }
        try {
            send(object);
            return true;
        } catch (IllegalStateException | IOException e) {
            closed.set(true);
            return false;
        }
    }

    public void completeSafely() {
        if (closed.compareAndSet(false, true)) {
            super.complete();
        }
    }

    public boolean isClosed() {
        return closed.get();
    }
}
