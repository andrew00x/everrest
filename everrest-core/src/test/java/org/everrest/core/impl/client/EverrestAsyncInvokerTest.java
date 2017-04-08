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

import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.concurrent.Future;

import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EverrestAsyncInvokerTest {
    private InvocationBuilder builder;
    private EverrestAsyncInvoker asyncInvoker;

    @Before
    public void setUp() throws Exception {
        builder = mock(InvocationBuilder.class);
        asyncInvoker = new EverrestAsyncInvoker(builder);
    }

    @Test
    public void submitAsyncGet() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.buildGet()).thenReturn(invocation);
        when(invocation.submit()).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.get();

        assertEquals(response, responseFuture.get());
    }

    @Test
    public void submitAsyncGetWithResponseType() throws Exception {
        Future future = mock(Future.class);
        when(future.get()).thenReturn("hello");

        Invocation invocation = mock(Invocation.class);
        when(builder.buildGet()).thenReturn(invocation);
        when(invocation.submit(String.class)).thenReturn(future);

        Future<String> responseFuture = asyncInvoker.get(String.class);

        assertEquals("hello", responseFuture.get());
    }

    @Test
    public void submitAsyncGetWithGenericResponseType() throws Exception {
        Future future = mock(Future.class);
        when(future.get()).thenReturn("hello");

        Invocation invocation = mock(Invocation.class);
        when(builder.buildGet()).thenReturn(invocation);
        when(invocation.submit(new GenericType<>(String.class))).thenReturn(future);

        Future<String> responseFuture = asyncInvoker.get(new GenericType<>(String.class));

        assertEquals("hello", responseFuture.get());
    }

    @Test
    public void submitAsyncGetWithInvocationCallback() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        InvocationCallback<Response> callback = mock(InvocationCallback.class);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.buildGet()).thenReturn(invocation);
        when(invocation.submit(callback)).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.get(callback);

        assertEquals(response, responseFuture.get());
    }

    @Test
    public void submitAsyncPut() throws Exception {
        Entity<String> requestEntity = Entity.text("hello");

        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.buildPut(requestEntity)).thenReturn(invocation);
        when(invocation.submit()).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.put(requestEntity);

        assertEquals(response, responseFuture.get());
    }

    @Test
    public void submitAsyncPutWithResponseType() throws Exception {
        Entity<String> requestEntity = Entity.text("hello");

        Future future = mock(Future.class);
        when(future.get()).thenReturn("hello");

        Invocation invocation = mock(Invocation.class);
        when(builder.buildPut(requestEntity)).thenReturn(invocation);
        when(invocation.submit(String.class)).thenReturn(future);

        Future<String> responseFuture = asyncInvoker.put(requestEntity, String.class);

        assertEquals("hello", responseFuture.get());
    }

    @Test
    public void submitAsyncPutWithGenericResponseType() throws Exception {
        Entity<String> requestEntity = Entity.text("hello");

        Future future = mock(Future.class);
        when(future.get()).thenReturn("hello");

        Invocation invocation = mock(Invocation.class);
        when(builder.buildPut(requestEntity)).thenReturn(invocation);
        when(invocation.submit(new GenericType<>(String.class))).thenReturn(future);

        Future<String> responseFuture = asyncInvoker.put(requestEntity, new GenericType<>(String.class));

        assertEquals("hello", responseFuture.get());
    }

    @Test
    public void submitAsyncPutWithInvocationCallback() throws Exception {
        Entity<String> requestEntity = Entity.text("hello");
        InvocationCallback<Response> callback = mock(InvocationCallback.class);

        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.buildPut(requestEntity)).thenReturn(invocation);
        when(invocation.submit(callback)).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.put(requestEntity, callback);

        assertEquals(response, responseFuture.get());
    }

    @Test
    public void submitAsyncPost() throws Exception {
        Entity<String> requestEntity = Entity.text("hello");

        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.buildPost(requestEntity)).thenReturn(invocation);
        when(invocation.submit()).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.post(requestEntity);

        assertEquals(response, responseFuture.get());
    }

    @Test
    public void submitAsyncPostWithResponseType() throws Exception {
        Entity<String> requestEntity = Entity.text("hello");

        Future future = mock(Future.class);
        when(future.get()).thenReturn("hello");

        Invocation invocation = mock(Invocation.class);
        when(builder.buildPost(requestEntity)).thenReturn(invocation);
        when(invocation.submit(String.class)).thenReturn(future);

        Future<String> responseFuture = asyncInvoker.post(requestEntity, String.class);

        assertEquals("hello", responseFuture.get());
    }

    @Test
    public void submitAsyncPostWithGenericResponseType() throws Exception {
        Entity<String> requestEntity = Entity.text("hello");

        Future future = mock(Future.class);
        when(future.get()).thenReturn("hello");

        Invocation invocation = mock(Invocation.class);
        when(builder.buildPost(requestEntity)).thenReturn(invocation);
        when(invocation.submit(new GenericType<>(String.class))).thenReturn(future);

        Future<String> responseFuture = asyncInvoker.post(requestEntity, new GenericType<>(String.class));

        assertEquals("hello", responseFuture.get());
    }

    @Test
    public void submitAsyncPostWithInvocationCallback() throws Exception {
        Entity<String> requestEntity = Entity.text("hello");
        InvocationCallback<Response> callback = mock(InvocationCallback.class);

        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.buildPost(requestEntity)).thenReturn(invocation);
        when(invocation.submit(callback)).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.post(requestEntity, callback);

        assertEquals(response, responseFuture.get());
    }

    @Test
    public void submitAsyncDelete() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.buildDelete()).thenReturn(invocation);
        when(invocation.submit()).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.delete();

        assertEquals(response, responseFuture.get());
    }

    @Test
    public void submitAsyncDeleteWithResponseType() throws Exception {
        Future future = mock(Future.class);
        when(future.get()).thenReturn("hello");

        Invocation invocation = mock(Invocation.class);
        when(builder.buildDelete()).thenReturn(invocation);
        when(invocation.submit(String.class)).thenReturn(future);

        Future<String> responseFuture = asyncInvoker.delete(String.class);

        assertEquals("hello", responseFuture.get());
    }

    @Test
    public void submitAsyncDeleteWithGenericResponseType() throws Exception {
        Future future = mock(Future.class);
        when(future.get()).thenReturn("hello");

        Invocation invocation = mock(Invocation.class);
        when(builder.buildDelete()).thenReturn(invocation);
        when(invocation.submit(new GenericType<>(String.class))).thenReturn(future);

        Future<String> responseFuture = asyncInvoker.delete(new GenericType<>(String.class));

        assertEquals("hello", responseFuture.get());
    }

    @Test
    public void submitAsyncDeleteWithInvocationCallback() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        InvocationCallback<Response> callback = mock(InvocationCallback.class);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.buildDelete()).thenReturn(invocation);
        when(invocation.submit(callback)).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.delete(callback);

        assertEquals(response, responseFuture.get());
    }

    @Test
    public void submitAsyncHead() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.build(HEAD)).thenReturn(invocation);
        when(invocation.submit()).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.head();

        assertEquals(response, responseFuture.get());
    }

    @Test
    public void submitAsyncHeadWithInvocationCallback() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        InvocationCallback<Response> callback = mock(InvocationCallback.class);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.build(HEAD)).thenReturn(invocation);
        when(invocation.submit(callback)).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.head(callback);

        assertEquals(response, responseFuture.get());
    }

    @Test
    public void submitAsyncOptions() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.build(OPTIONS)).thenReturn(invocation);
        when(invocation.submit()).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.options();

        assertEquals(response, responseFuture.get());
    }

    @Test
    public void submitAsyncOptionsWithResponseType() throws Exception {
        Future future = mock(Future.class);
        when(future.get()).thenReturn("hello");

        Invocation invocation = mock(Invocation.class);
        when(builder.build(OPTIONS)).thenReturn(invocation);
        when(invocation.submit(String.class)).thenReturn(future);

        Future<String> responseFuture = asyncInvoker.options(String.class);

        assertEquals("hello", responseFuture.get());
    }

    @Test
    public void submitAsyncOptionsWithGenericResponseType() throws Exception {
        Future future = mock(Future.class);
        when(future.get()).thenReturn("hello");

        Invocation invocation = mock(Invocation.class);
        when(builder.build(OPTIONS)).thenReturn(invocation);
        when(invocation.submit(new GenericType<>(String.class))).thenReturn(future);

        Future<String> responseFuture = asyncInvoker.options(new GenericType<>(String.class));

        assertEquals("hello", responseFuture.get());
    }

    @Test
    public void submitAsyncOptionsWithInvocationCallback() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        InvocationCallback<Response> callback = mock(InvocationCallback.class);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.build(OPTIONS)).thenReturn(invocation);
        when(invocation.submit(callback)).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.options(callback);

        assertEquals(response, responseFuture.get());
    }

    @Test
    public void submitAsyncTrace() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.build("TRACE")).thenReturn(invocation);
        when(invocation.submit()).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.trace();

        assertEquals(response, responseFuture.get());
    }

    @Test
    public void submitAsyncTraceWithResponseType() throws Exception {
        Future future = mock(Future.class);
        when(future.get()).thenReturn("hello");

        Invocation invocation = mock(Invocation.class);
        when(builder.build("TRACE")).thenReturn(invocation);
        when(invocation.submit(String.class)).thenReturn(future);

        Future<String> responseFuture = asyncInvoker.trace(String.class);

        assertEquals("hello", responseFuture.get());
    }

    @Test
    public void submitAsyncTraceWithGenericResponseType() throws Exception {
        Future future = mock(Future.class);
        when(future.get()).thenReturn("hello");

        Invocation invocation = mock(Invocation.class);
        when(builder.build("TRACE")).thenReturn(invocation);
        when(invocation.submit(new GenericType<>(String.class))).thenReturn(future);

        Future<String> responseFuture = asyncInvoker.trace(new GenericType<>(String.class));

        assertEquals("hello", responseFuture.get());
    }

    @Test
    public void submitAsyncTraceWithInvocationCallback() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        InvocationCallback<Response> callback = mock(InvocationCallback.class);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.build("TRACE")).thenReturn(invocation);
        when(invocation.submit(callback)).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.trace(callback);

        assertEquals(response, responseFuture.get());
    }

    @Test
    public void submitAsyncWithHttpMethod() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.build(POST)).thenReturn(invocation);
        when(invocation.submit()).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.method(POST);

        assertEquals(response, responseFuture.get());
    }

    @Test
    public void submitAsyncWithHttpMethodAndResponseType() throws Exception {
        Future future = mock(Future.class);
        when(future.get()).thenReturn("hello");

        Invocation invocation = mock(Invocation.class);
        when(builder.build(POST)).thenReturn(invocation);
        when(invocation.submit(String.class)).thenReturn(future);

        Future<String> responseFuture = asyncInvoker.method(POST, String.class);

        assertEquals("hello", responseFuture.get());
    }

    @Test
    public void submitAsyncWithHttpMethodAndGenericResponseType() throws Exception {
        Future future = mock(Future.class);
        when(future.get()).thenReturn("hello");

        Invocation invocation = mock(Invocation.class);
        when(builder.build(POST)).thenReturn(invocation);
        when(invocation.submit(new GenericType<>(String.class))).thenReturn(future);

        Future<String> responseFuture = asyncInvoker.method(POST, new GenericType<>(String.class));

        assertEquals("hello", responseFuture.get());
    }

    @Test
    public void submitAsyncWithHttpMethodAndInvocationCallback() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        InvocationCallback<Response> callback = mock(InvocationCallback.class);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.build(POST)).thenReturn(invocation);
        when(invocation.submit(callback)).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.method(POST, callback);

        assertEquals(response, responseFuture.get());
    }
    @Test
    public void submitAsyncWithHttpMethodAndEntity() throws Exception {
        Entity<String> requestEntity = Entity.text("hello");

        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.build(POST, requestEntity)).thenReturn(invocation);
        when(invocation.submit()).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.method(POST, requestEntity);

        assertEquals(response, responseFuture.get());
    }

    @Test
    public void submitAsyncWithHttpMethodAndEntityAndResponseType() throws Exception {
        Entity<String> requestEntity = Entity.text("hello");

        Future future = mock(Future.class);
        when(future.get()).thenReturn("hello");

        Invocation invocation = mock(Invocation.class);
        when(builder.build(POST, requestEntity)).thenReturn(invocation);
        when(invocation.submit(String.class)).thenReturn(future);

        Future<String> responseFuture = asyncInvoker.method(POST, requestEntity, String.class);

        assertEquals("hello", responseFuture.get());
    }

    @Test
    public void submitAsyncWithHttpMethodAndEntityAndGenericResponseType() throws Exception {
        Entity<String> requestEntity = Entity.text("hello");

        Future future = mock(Future.class);
        when(future.get()).thenReturn("hello");

        Invocation invocation = mock(Invocation.class);
        when(builder.build(POST, requestEntity)).thenReturn(invocation);
        when(invocation.submit(new GenericType<>(String.class))).thenReturn(future);

        Future<String> responseFuture = asyncInvoker.method(POST, requestEntity, new GenericType<>(String.class));

        assertEquals("hello", responseFuture.get());
    }

    @Test
    public void submitAsyncWithHttpMethodAndEntityAndInvocationCallback() throws Exception {
        Entity<String> requestEntity = Entity.text("hello");
        InvocationCallback<Response> callback = mock(InvocationCallback.class);

        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);

        Future future = mock(Future.class);
        when(future.get()).thenReturn(response);

        Invocation invocation = mock(Invocation.class);
        when(builder.build(POST, requestEntity)).thenReturn(invocation);
        when(invocation.submit(callback)).thenReturn(future);

        Future<Response> responseFuture = asyncInvoker.method(POST, requestEntity, callback);

        assertEquals(response, responseFuture.get());
    }
}