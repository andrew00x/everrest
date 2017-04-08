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
import javax.ws.rs.core.Response;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

class ResponseInvocationCallback implements InvocationCallback<Response> {
    AtomicInteger completed = new AtomicInteger();
    AtomicInteger failed = new AtomicInteger();
    Response response;
    Throwable throwable;

    void assertCompletedWith(Object entity) {
        assertEquals(1, completed.get());
        Response response = (Response) entity;
        assertEquals(response.getStatus(), this.response.getStatus());
        assertEquals(response.getHeaders(), this.response.getHeaders());
        assertEquals(response.getEntity(), this.response.getEntity());
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
    public void completed(Response response) {
        completed.incrementAndGet();
        this.response = response;
    }

    @Override
    public void failed(Throwable throwable) {
        failed.incrementAndGet();
        this.throwable = throwable;
    }
}
