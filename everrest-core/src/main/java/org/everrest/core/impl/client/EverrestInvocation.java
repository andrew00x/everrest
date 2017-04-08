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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;

import static org.everrest.core.util.ReflectionUtils.getParameterizedType;

public class EverrestInvocation implements Invocation {
    private final ClientRequest request;
    private final InvocationPipeline requestInvocationPipeline;
    private final Supplier<ExecutorService> executorProvider;

    EverrestInvocation(ClientRequest request,
                       InvocationPipeline requestInvocationPipeline,
                       Supplier<ExecutorService> executorProvider) {
        this.request = request;
        this.requestInvocationPipeline = requestInvocationPipeline;
        this.executorProvider = executorProvider;
    }

    @Override
    public Invocation property(String name, Object value) {
        request.setProperty(name, value);
        return this;
    }

    @Override
    public Response invoke() {
        return requestInvocationPipeline.execute(request).getResponse();
    }

    @Override
    public <T> T invoke(Class<T> responseType) {
        return invoke(new GenericType<T>(responseType));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T invoke(GenericType<T> responseType) {
        Response response = invoke();
        if (responseType.getRawType() == Response.class) {
            return (T) response;
        } else if ((response.getStatus() / 100) == 2) {
            return response.readEntity(responseType);
        } else {
            response.bufferEntity();
            throw new WebApplicationException(String.format("Response status is %d", response.getStatus()), response);
        }
    }

    @Override
    public Future<Response> submit() {
        return executorProvider.get().submit(() -> invoke());
    }

    @Override
    public <T> Future<T> submit(Class<T> responseType) {
        return executorProvider.get().submit(() -> invoke(responseType));
    }

    @Override
    public <T> Future<T> submit(GenericType<T> responseType) {
        return executorProvider.get().submit(() -> invoke(responseType));
    }

    @Override
    public <T> Future<T> submit(InvocationCallback<T> callback) {
        ParameterizedType callbackType = getParameterizedType(callback.getClass(), InvocationCallback.class);
        Type callbackParamType;
        if (callbackType == null || callbackType.getActualTypeArguments().length == 0) {
            callbackParamType = Response.class;
        } else {
            callbackParamType = callbackType.getActualTypeArguments()[0];
        }
        final GenericType<T> responseType = new GenericType<>(callbackParamType);
        FutureTask<T> future = new FutureTask<T>(() -> invoke(responseType)) {
            @Override
            protected void set(T responseOrEntity) {
                callback.completed(responseOrEntity);
                super.set(responseOrEntity);
            }

            @Override
            protected void setException(Throwable t) {
                callback.failed(t);
                super.setException(t);
            }
        };
        executorProvider.get().execute(future);
        return future;
    }
}
