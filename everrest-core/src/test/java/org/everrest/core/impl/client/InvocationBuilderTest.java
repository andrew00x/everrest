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
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.ACCEPT_ENCODING;
import static javax.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.HttpHeaders.CONTENT_ENCODING;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LANGUAGE;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.COOKIE;
import static org.everrest.core.impl.header.CacheControlBuilder.aCacheControl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InvocationBuilderTest {

    private ClientRequest request;
    private Supplier<ExecutorService> executorProvider;
    private ExecutorService executor;
    private InvocationPipeline requestInvocationPipeline;

    private InvocationBuilder builder;

    @Before
    public void setUp() {
        request = mock(ClientRequest.class);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(request.getHeaders()).thenReturn(headers);

        executorProvider = mock(Supplier.class);
        executor = mock(ExecutorService.class);
        when(executorProvider.get()).thenReturn(executor);

        requestInvocationPipeline = mock(InvocationPipeline.class);

        builder = new InvocationBuilder(executorProvider, requestInvocationPipeline, request);
    }

    @Test
    public void setsAcceptHeaderAsStrings() {
        builder.accept("application/json", "application/xml");
        assertTrue(request.getHeaders().get(ACCEPT).containsAll(newArrayList("application/json", "application/xml")));
    }

    @Test
    public void setsAcceptHeaderAsMediaTypes() {
        builder.accept(new MediaType("application", "json"), new MediaType("application", "xml"));
        assertTrue(request.getHeaders().get(ACCEPT).containsAll(newArrayList(new MediaType("application", "json"), new MediaType("application", "xml"))));
    }

    @Test
    public void setsAcceptLanguageHeaderAsStrings() {
        builder.acceptLanguage("en-us", "*");
        assertTrue(request.getHeaders().get(ACCEPT_LANGUAGE).containsAll(newArrayList("en-us", "*")));
    }

    @Test
    public void setsAcceptLanguageHeaderAsLocales() {
        builder.acceptLanguage(new Locale("en", "us"), new Locale("*"));
        assertTrue(request.getHeaders().get(ACCEPT_LANGUAGE).containsAll(newArrayList(new Locale("en", "us"), new Locale("*"))));
    }

    @Test
    public void setsAcceptEncodingHeader() {
        builder.acceptEncoding("gzip");
        assertTrue(request.getHeaders().get(ACCEPT_ENCODING).contains("gzip"));
    }

    @Test
    public void setsCookie() {
        builder.cookie(new Cookie("name", "value"));
        assertTrue(request.getHeaders().get(COOKIE).contains(new Cookie("name", "value")));
    }

    @Test
    public void setsCookieAsNameAndValue() {
        builder.cookie("name", "value");
        assertTrue(request.getHeaders().get(COOKIE).contains(new Cookie("name", "value")));
    }

    @Test
    public void setsCacheControlHeader() {
        builder.cacheControl(aCacheControl().withMaxAge(60).build());
        assertTrue(request.getHeaders().get(CACHE_CONTROL).contains(aCacheControl().withMaxAge(60).build()));
    }

    @Test
    public void nullCacheControlRemovesAllExistingCacheControlHeaders() {
        builder.cacheControl(aCacheControl().withMaxAge(60).build());
        builder.cacheControl(null);
        assertNull(request.getHeaders().get(CACHE_CONTROL));
    }

    @Test
    public void setsHeader() {
        builder.header("name", "value");
        assertTrue(request.getHeaders().get("name").contains("value"));
    }

    @Test
    public void nullHeaderRemovesAllExistingHeadersWithTheSameName() {
        builder.header("name", "value");
        builder.header("name", null);
        assertNull(request.getHeaders().get("name"));
    }

    @Test
    public void settingMapOfHeadersReplacesAlExistingHeaders() {
        request.getHeaders().add("name1", "value1");
        request.getHeaders().add("name2", "value2");
        MultivaluedMap<String, Object> newHeaders = new MultivaluedHashMap<>();
        newHeaders.add("name3", "value3");
        newHeaders.add("name4", "value4");
        builder.headers(newHeaders);
        assertEquals(newHeaders, request.getHeaders());
    }

    @Test
    public void setsProperty() {
        builder.property("name", "value");
        verify(request).setProperty("name", "value");
    }

    @Test
    public void buildsInvocationWithMethod() {
        EverrestInvocation invocation = (EverrestInvocation) builder.accept("text/plain")
                .acceptLanguage("en-us")
                .acceptEncoding("gzip")
                .cookie("name", "value")
                .build(GET);

        assertNotNull(invocation);
        verify(request).setMethod(GET);
        assertEquals(newArrayList("en-us"), request.getHeaders().get(ACCEPT_LANGUAGE));
        assertEquals(newArrayList("text/plain"), request.getHeaders().get(ACCEPT));
        assertEquals(newArrayList("gzip"), request.getHeaders().get(ACCEPT_ENCODING));
        assertEquals(newArrayList(new Cookie("name", "value")), request.getHeaders().get(COOKIE));
    }

    @Test
    public void buildsInvocationWithMethodAndEntityWhichOverridesContentTypeContentLanguageAndContentEncoding() {
        Entity<String> entity = Entity.entity("entity",
                new Variant(new MediaType("text", "plain"), "en", "us", "gzip"),
                new Annotation[0]);
        builder.header(CONTENT_TYPE, "application/xml");
        builder.header(CONTENT_LANGUAGE, "ua-ua");
        builder.header(CONTENT_ENCODING, "compress");

        EverrestInvocation invocation = (EverrestInvocation) builder.build(POST, entity);

        assertNotNull(invocation);
        verify(request).setMethod(POST);
        verify(request).setEntity("entity", new Annotation[0], new MediaType("text", "plain"));
        assertEquals(new Locale("en", "us"), request.getHeaders().getFirst(CONTENT_LANGUAGE));
        assertEquals(new MediaType("text", "plain"), request.getHeaders().getFirst(CONTENT_TYPE));
        assertEquals("gzip", request.getHeaders().getFirst(CONTENT_ENCODING));
    }

    @Test
    public void buildsGetInvocation() {
        EverrestInvocation invocation = (EverrestInvocation) builder.accept("text/plain")
                .acceptLanguage("en-us")
                .acceptEncoding("gzip")
                .cookie("name", "value")
                .buildGet();

        assertNotNull(invocation);
        verify(request).setMethod(GET);
        assertEquals(newArrayList("en-us"), request.getHeaders().get(ACCEPT_LANGUAGE));
        assertEquals(newArrayList("text/plain"), request.getHeaders().get(ACCEPT));
        assertEquals(newArrayList("gzip"), request.getHeaders().get(ACCEPT_ENCODING));
        assertEquals(newArrayList(new Cookie("name", "value")), request.getHeaders().get(COOKIE));
    }

    @Test
    public void buildsDeleteInvocation() {
        EverrestInvocation invocation = (EverrestInvocation) builder.buildDelete();
        assertNotNull(invocation);
        verify(request).setMethod(DELETE);
    }

    @Test
    public void buildsPostInvocation() {
        Entity<String> entity = Entity.entity("entity",
                new Variant(new MediaType("text", "plain"), "en", "us", "gzip"),
                new Annotation[0]);

        EverrestInvocation invocation = (EverrestInvocation) builder.buildPost(entity);

        assertNotNull(invocation);
        verify(request).setMethod(POST);
        verify(request).setEntity("entity", new Annotation[0], new MediaType("text", "plain"));
        assertEquals(new Locale("en", "us"), request.getHeaders().getFirst(CONTENT_LANGUAGE));
        assertEquals(new MediaType("text", "plain"), request.getHeaders().getFirst(CONTENT_TYPE));
        assertEquals("gzip", request.getHeaders().getFirst(CONTENT_ENCODING));
    }

    @Test
    public void buildsPutInvocation() {
        Entity<String> entity = Entity.entity("entity",
                new Variant(new MediaType("text", "plain"), "en", "us", "gzip"),
                new Annotation[0]);

        EverrestInvocation invocation = (EverrestInvocation) builder.buildPut(entity);

        assertNotNull(invocation);
        verify(request).setMethod(PUT);
        verify(request).setEntity("entity", new Annotation[0], new MediaType("text", "plain"));
        assertEquals(new Locale("en", "us"), request.getHeaders().getFirst(CONTENT_LANGUAGE));
        assertEquals(new MediaType("text", "plain"), request.getHeaders().getFirst(CONTENT_TYPE));
        assertEquals("gzip", request.getHeaders().getFirst(CONTENT_ENCODING));
    }

    @Test
    public void invokesGet() {
        Response response = mockEmptyResponseWithStatus(200);

        assertEquals(response, builder.get());
        verify(request).setMethod(GET);
    }

    @Test
    public void invokesGetWithResponseType() {
        mockResponseWithStatusAndEntityType(200, new GenericType<>(String.class), "hello");

        assertEquals("hello", builder.get(String.class));
        verify(request).setMethod(GET);
    }

    @Test
    public void invokesGetWithGenericResponseType() {
        mockResponseWithStatusAndEntityType(200, new GenericType<>(String.class), "hello");

        assertEquals("hello", builder.get(new GenericType<>(String.class)));
        verify(request).setMethod(GET);
    }

    @Test
    public void invokesPut() {
        Response response = mockEmptyResponseWithStatus(200);
        Entity<String> entity = Entity.text("hello");

        assertEquals(response, builder.put(entity));
        verify(request).setMethod(PUT);
        assertEquals(new MediaType("text", "plain"), request.getHeaders().getFirst(CONTENT_TYPE));
    }

    @Test
    public void invokesPutWithResponseType() {
        Entity<String> entity = Entity.text("hello");
        mockResponseWithStatusAndEntityType(200, new GenericType<>(String.class), "hello");

        assertEquals("hello", builder.put(entity, String.class));
        verify(request).setMethod(PUT);
        assertEquals(new MediaType("text", "plain"), request.getHeaders().getFirst(CONTENT_TYPE));
    }

    @Test
    public void invokesPutWithGenericResponseType() {
        Entity<String> entity = Entity.text("hello");
        mockResponseWithStatusAndEntityType(200, new GenericType<>(String.class), "hello");

        assertEquals("hello", builder.put(entity, new GenericType<>(String.class)));
        verify(request).setMethod(PUT);
        assertEquals(new MediaType("text", "plain"), request.getHeaders().getFirst(CONTENT_TYPE));
    }

    @Test
    public void invokesPost() {
        Response response = mockEmptyResponseWithStatus(200);
        Entity<String> entity = Entity.text("hello");

        assertEquals(response, builder.post(entity));
        verify(request).setMethod(POST);
        assertEquals(new MediaType("text", "plain"), request.getHeaders().getFirst(CONTENT_TYPE));
    }

    @Test
    public void invokesPostWithResponseType() {
        Entity<String> entity = Entity.text("hello");
        mockResponseWithStatusAndEntityType(200, new GenericType<>(String.class), "hello");

        assertEquals("hello", builder.post(entity, String.class));
        verify(request).setMethod(POST);
        assertEquals(new MediaType("text", "plain"), request.getHeaders().getFirst(CONTENT_TYPE));
    }

    @Test
    public void invokesPostWithGenericResponseType() {
        Entity<String> entity = Entity.text("hello");
        mockResponseWithStatusAndEntityType(200, new GenericType<>(String.class), "hello");

        assertEquals("hello", builder.post(entity, new GenericType<>(String.class)));
        verify(request).setMethod(POST);
        assertEquals(new MediaType("text", "plain"), request.getHeaders().getFirst(CONTENT_TYPE));
    }

    @Test
    public void invokesDelete() {
        Response response = mockEmptyResponseWithStatus(200);

        assertEquals(response, builder.delete());
        verify(request).setMethod(DELETE);
    }

    @Test
    public void invokesDeleteWithResponseType() {
        mockResponseWithStatusAndEntityType(200, new GenericType<>(String.class), "hello");

        assertEquals("hello", builder.delete(String.class));
        verify(request).setMethod(DELETE);
    }

    @Test
    public void invokesDeleteWithGenericResponseType() {
        mockResponseWithStatusAndEntityType(200, new GenericType<>(String.class), "hello");

        assertEquals("hello", builder.delete(new GenericType<>(String.class)));
        verify(request).setMethod(DELETE);
    }

    @Test
    public void invokesHead() {
        Response response = mockEmptyResponseWithStatus(200);

        assertEquals(response, builder.head());
        verify(request).setMethod(HEAD);
    }

    @Test
    public void invokesOptions() {
        Response response = mockEmptyResponseWithStatus(200);

        assertEquals(response, builder.options());
        verify(request).setMethod(OPTIONS);
    }

    @Test
    public void invokesOptionsWithResponseType() {
        mockResponseWithStatusAndEntityType(200, new GenericType<>(String.class), "hello");

        assertEquals("hello", builder.options(String.class));
        verify(request).setMethod(OPTIONS);
    }

    @Test
    public void invokesOptionsWithGenericResponseType() {
        mockResponseWithStatusAndEntityType(200, new GenericType<>(String.class), "hello");

        assertEquals("hello", builder.options(new GenericType<>(String.class)));
        verify(request).setMethod(OPTIONS);
    }

    @Test
    public void invokesTrace() {
        Response response = mockEmptyResponseWithStatus(200);

        assertEquals(response, builder.trace());
        verify(request).setMethod("TRACE");
    }

    @Test
    public void invokesTraceWithResponseType() {
        mockResponseWithStatusAndEntityType(200, new GenericType<>(String.class), "hello");

        assertEquals("hello", builder.trace(String.class));
        verify(request).setMethod("TRACE");
    }

    @Test
    public void invokesTraceWithGenericResponseType() {
        mockResponseWithStatusAndEntityType(200, new GenericType<>(String.class), "hello");

        assertEquals("hello", builder.trace(new GenericType<>(String.class)));
        verify(request).setMethod("TRACE");
    }

    @Test
    public void invokesMethod() {
        Response response = mockEmptyResponseWithStatus(200);

        assertEquals(response, builder.method(GET));
        verify(request).setMethod(GET);
    }

    @Test
    public void invokesMethodWithResponseType() {
        mockResponseWithStatusAndEntityType(200, new GenericType<>(String.class), "hello");

        assertEquals("hello", builder.method(GET, String.class));
        verify(request).setMethod(GET);
    }

    @Test
    public void invokesMethodWithGenericResponseType() {
        mockResponseWithStatusAndEntityType(200, new GenericType<>(String.class), "hello");

        assertEquals("hello", builder.method(GET, new GenericType<>(String.class)));
        verify(request).setMethod(GET);
    }

    @Test
    public void invokesMethodWithEntity() {
        Entity<String> entity = Entity.text("hello");
        Response response = mockEmptyResponseWithStatus(200);

        assertEquals(response, builder.method(POST, entity));
        verify(request).setMethod(POST);
        assertEquals(new MediaType("text", "plain"), request.getHeaders().getFirst(CONTENT_TYPE));
    }

    @Test
    public void invokesMethodWithEntityAndGenericResponseType() {
        Entity<String> entity = Entity.text("hello");
        mockResponseWithStatusAndEntityType(200, new GenericType<>(String.class), "hello");

        assertEquals("hello", builder.method(POST, entity, new GenericType<>(String.class)));
        verify(request).setMethod(POST);
        assertEquals(new MediaType("text", "plain"), request.getHeaders().getFirst(CONTENT_TYPE));
    }

    @Test
    public void invokesMethodWithEntityAndResponseType() {
        Entity<String> entity = Entity.text("hello");
        mockResponseWithStatusAndEntityType(200, new GenericType<>(String.class), "hello");

        assertEquals("hello", builder.method(POST, entity, String.class));
        verify(request).setMethod(POST);
        assertEquals(new MediaType("text", "plain"), request.getHeaders().getFirst(CONTENT_TYPE));
    }

    private Response mockEmptyResponseWithStatus(int status) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);

        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.getResponse()).thenReturn(response);
        when(requestInvocationPipeline.execute(request)).thenReturn(clientResponse);

        return response;
    }

    private <T> Response mockResponseWithStatusAndEntityType(int status, GenericType<T> responseType, T entity) {
        Response response = mockEmptyResponseWithStatus(status);
        when(response.readEntity(responseType)).thenReturn(entity);
        return response;
    }
}
