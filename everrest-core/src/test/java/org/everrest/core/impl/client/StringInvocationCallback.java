/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.everrest.core.impl.client;

import javax.ws.rs.client.InvocationCallback;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

class StringInvocationCallback implements InvocationCallback<String> {
    AtomicInteger completed = new AtomicInteger();
    AtomicInteger failed = new AtomicInteger();
    Object entity;
    Throwable throwable;

    void assertCompletedWith(Object entity) {
        assertEquals(1, completed.get());
        assertEquals(entity, this.entity);
    }

    void assertFailedWith(Class<? extends Throwable> thrown) {
        assertEquals(1, failed.get());
        assertEquals(thrown, this.throwable.getClass());
    }

    void assertNotFailed() {
        assertEquals(0, failed.get());
    }

    void assertNotCompleted() {
        assertEquals(0, completed.get());
    }

    @Override
    public void completed(String str) {
        completed.incrementAndGet();
        entity = str;
    }

    @Override
    public void failed(Throwable throwable) {
        failed.incrementAndGet();
        this.throwable = throwable;
    }
}
