package com.tkck.test.runtime;

import com.tkck.trigger.http.sse.SafeSseEmitter;
import org.junit.Assert;
import org.junit.Test;

public class SafeSseEmitterTest {

    @Test
    public void should_ignore_send_after_complete() {
        SafeSseEmitter emitter = new SafeSseEmitter(1000L);

        emitter.completeSafely();

        boolean sent = emitter.safeSend("ignored");

        Assert.assertFalse(sent);
        Assert.assertTrue(emitter.isClosed());
    }
}
