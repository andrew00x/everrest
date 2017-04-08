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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.collect.Lists.newArrayList;
import static org.everrest.core.$matchers.ExceptionMatchers.webApplicationExceptionWithStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EverrestInvocationTest {
    @Rule public ExpectedException thrown = ExpectedException.none();

    private static ExecutorService executor;
    private InvocationPipeline requestInvocationPipeline;
    private EverrestInvocation invocation;

    private ClientRequest clientRequest;
    private ClientResponse clientResponse;

    @BeforeClass
    public static void beforeClass() throws Exception {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        executor.shutdownNow();
    }

    @Before
    public void setUp() throws Exception {
        requestInvocationPipeline = mock(InvocationPipeline.class);
        clientRequest = mock(ClientRequest.class);
        clientResponse = mock(ClientResponse.class);
        invocation = new EverrestInvocation(clientRequest, requestInvocationPipeline, () -> executor);
        when(requestInvocationPipeline.execute(clientRequest)).thenReturn(clientResponse);
    }

    @Test
    public void invokesAndRetrievesResponseWhenResponseStatusIsSuccess() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        when(clientResponse.getResponse()).thenReturn(response);

        Response _response = invocation.invoke();
        assertEquals(200, _response.getStatus());
    }

    @Test
    public void invokesAndRetrievesResponseWhenResponseStatusIsNotSuccess() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(clientResponse.getResponse()).thenReturn(response);

        Response _response = invocation.invoke();
        assertEquals(400, _response.getStatus());
    }

    @Test
    public void invokesAndRetrievesEntityByTypeWhenResponseStatusIsSuccess() throws Exception {
        String entity = "hello";
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(new GenericType<>(String.class))).thenReturn(entity);
        when(clientResponse.getResponse()).thenReturn(response);

        assertEquals(entity, invocation.invoke(String.class));
    }

    @Test
    public void invokesAndRetrievesEntityByTypeWhenResponseStatusIsNotSuccess() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(clientResponse.getResponse()).thenReturn(response);

        thrown.expect(webApplicationExceptionWithStatus(400));
        invocation.invoke(String.class);
    }

    @Test
    public void invokesAndRetrievesEntityByGenericTypeWhenResponseStatusIsSuccess() throws Exception {
        List<String> entity = newArrayList("hello");
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(new GenericType<List<String>>(){})).thenReturn(entity);
        when(clientResponse.getResponse()).thenReturn(response);

        assertEquals(entity, invocation.invoke(new GenericType<List<String>>(){}));
    }

    @Test
    public void invokesAndRetrievesEntityByGenericTypeWhenResponseStatusIsNotSuccess() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(clientResponse.getResponse()).thenReturn(response);

        thrown.expect(webApplicationExceptionWithStatus(400));
        invocation.invoke(new GenericType<List<String>>(){});
    }

    @Test
    public void submitsAndRetrievesResponseWhenResponseStatusIsSuccess() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        when(clientResponse.getResponse()).thenReturn(response);

        Response _response = invocation.submit().get();
        assertEquals(200, _response.getStatus());
    }

    @Test
    public void submitsAndRetrievesResponseWhenResponseStatusIsNotSuccess() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(clientResponse.getResponse()).thenReturn(response);

        Response _response = invocation.submit().get();
        assertEquals(400, _response.getStatus());
    }

    @Test
    public void submitsAndRetrievesEntityByTypeWhenResponseStatusIsSuccess() throws Exception {
        String entity = "hello";
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(new GenericType<>(String.class))).thenReturn(entity);
        when(clientResponse.getResponse()).thenReturn(response);

        assertEquals(entity, invocation.submit(String.class).get());
    }

    @Test
    public void submitsAndRetrievesEntityByTypeWhenResponseStatusIsNotSuccess() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(clientResponse.getResponse()).thenReturn(response);

        thrown.expect(ExecutionException.class);
        thrown.expectCause(webApplicationExceptionWithStatus(400));
        invocation.submit(String.class).get();
    }

    @Test
    public void submitsAndRetrievesEntityByGenericTypeWhenResponseStatusSuccess() throws Exception {
        List<String> entity = newArrayList("hello");
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(new GenericType<List<String>>(){})).thenReturn(entity);
        when(clientResponse.getResponse()).thenReturn(response);

        assertEquals(entity, invocation.submit(new GenericType<List<String>>(){}).get());
    }

    @Test
    public void submitsAndRetrievesEntityByGenericTypeWhenResponseStatusIsNotSuccess() throws Exception {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(clientResponse.getResponse()).thenReturn(response);

        thrown.expect(ExecutionException.class);
        thrown.expectCause(webApplicationExceptionWithStatus(400));
        invocation.submit(new GenericType<List<String>>(){}).get();
    }

    @Test
    public void submitsAndGetsEntityWithInvocationCallbackWhenResponseStatusIsSuccess() throws Exception {
        String entity = "hello";
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(new GenericType<>(String.class))).thenReturn(entity);
        when(clientResponse.getResponse()).thenReturn(response);
        StringInvocationCallback callback = new StringInvocationCallback();

        String responseEntity = invocation.submit(callback).get();

        assertEquals("hello", responseEntity);
        callback.assertCompletedWith("hello");
        callback.assertNotFailed();
    }

    @Test
    public void submitsAndGetsEntityWithInvocationCallbackWhenResponseStatusIsNotSuccess() throws Exception {
        String entity = "hello";
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(response.readEntity(new GenericType<>(String.class))).thenReturn(entity);
        when(clientResponse.getResponse()).thenReturn(response);
        StringInvocationCallback callback = new StringInvocationCallback();

        try {
            invocation.submit(callback).get();
        } catch(ExecutionException expected) {
        }

        callback.assertNotCompleted();
        callback.assertFailedWith(WebApplicationException.class);
    }

    @Test
    public void submitsAndGetsResponseWithInvocationCallbackWhenResponseStatusIsSuccess() throws Exception {
        String entity = "hello";
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        when(response.getEntity()).thenReturn(entity);
        when(clientResponse.getResponse()).thenReturn(response);
        ResponseInvocationCallback callback = new ResponseInvocationCallback();

        Response _response = invocation.submit(callback).get();

        assertEquals(200, _response.getStatus());
        assertEquals(entity, _response.getEntity());
        callback.assertCompletedWith(_response);
        callback.assertNotFailed();
    }

    @Test
    public void submitsAndGetsResponseWithInvocationCallbackWhenResponseStatusIsNotSuccess() throws Exception {
        String entity = "hello";
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(response.getEntity()).thenReturn(entity);
        when(clientResponse.getResponse()).thenReturn(response);
        ResponseInvocationCallback callback = new ResponseInvocationCallback();

        Response _response = invocation.submit(callback).get();

        assertEquals(400, _response.getStatus());
        assertEquals(entity, _response.getEntity());
        callback.assertCompletedWith(_response);
        callback.assertNotFailed();
    }

    @Test
    public void submitsAndGetsResponseWithRawInvocationCallback() throws Exception {
        String entity = "hello";
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(response.getEntity()).thenReturn(entity);
        when(clientResponse.getResponse()).thenReturn(response);
        RawInvocationCallback callback = new RawInvocationCallback();

        Object _response = invocation.submit(callback).get();

        assertTrue(_response instanceof Response);
        assertEquals(400, ((Response) _response).getStatus());
        assertEquals(entity, ((Response) _response).getEntity());
        callback.assertCompletedWith(_response);
        callback.assertNotFailed();
    }

    @Test
    public void submitsAndGetsEntityWithInvocationCallbackWhenExceptionIsThrownWhileProcessing() throws Exception {
        when(requestInvocationPipeline.execute(clientRequest)).thenThrow(ProcessingException.class);
        StringInvocationCallback callback = new StringInvocationCallback();

        try {
            invocation.submit(callback).get();
        } catch(ExecutionException expected) {
        }

        callback.assertNotCompleted();
        callback.assertFailedWith(ProcessingException.class);
    }

    @Test
    public void submitsAndGetsResponseWithInvocationCallbackWhenExceptionIsThrownWhileProcessing() throws Exception {
        when(requestInvocationPipeline.execute(clientRequest)).thenThrow(ProcessingException.class);
        ResponseInvocationCallback callback = new ResponseInvocationCallback();

        try {
            invocation.submit(callback).get();
        } catch(ExecutionException expected) {
        }

        callback.assertNotCompleted();
        callback.assertFailedWith(ProcessingException.class);
    }

    @Test
    public void setsPropertyInRequest() {
        invocation.property("name", "value");
        verify(clientRequest).setProperty("name", "value");
    }
}