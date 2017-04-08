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

import com.google.common.annotations.VisibleForTesting;
import org.everrest.core.util.CaselessStringWrapper;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static com.google.common.collect.Sets.newHashSet;
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
import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.COOKIE;
import static org.everrest.core.util.CaselessStringWrapper.caseLess;

public class InvocationBuilder implements Invocation.Builder {

    private static final Set<CaselessStringWrapper> SINGLE_VALUE_HEADERS =
            newHashSet(new CaselessStringWrapper(CACHE_CONTROL),
                    new CaselessStringWrapper(CONTENT_LANGUAGE),
                    new CaselessStringWrapper(CONTENT_TYPE),
                    new CaselessStringWrapper(CONTENT_ENCODING),
                    new CaselessStringWrapper(CONTENT_LENGTH));

    private final Supplier<ExecutorService> executorProvider;
    private final InvocationPipeline requestInvocationPipeline;
    private final ClientRequest request;

    InvocationBuilder(Supplier<ExecutorService> executorProvider,
                      InvocationPipeline requestInvocationPipeline,
                      ClientRequest request) {
        this.executorProvider = executorProvider;
        this.requestInvocationPipeline = requestInvocationPipeline;
        this.request = request;
    }

    @Override
    public Invocation build(String method) {
        request.setMethod(method);
        return new EverrestInvocation(request, requestInvocationPipeline, executorProvider);
    }

    @Override
    public Invocation build(String method, Entity<?> entity) {
        request.setMethod(method);
        if (entity.getEncoding() != null) {
            request.getHeaders().putSingle(CONTENT_ENCODING, entity.getEncoding());
        }
        if (entity.getLanguage() != null) {
            request.getHeaders().putSingle(CONTENT_LANGUAGE, entity.getLanguage());
        }
        if (entity.getMediaType() != null) {
            request.getHeaders().putSingle(CONTENT_TYPE, entity.getMediaType());
        }
        request.setEntity(entity.getEntity(), entity.getAnnotations(), entity.getMediaType());
        return new EverrestInvocation(request, requestInvocationPipeline, executorProvider);
    }

    @Override
    public Invocation buildGet() {
        return build(GET);
    }

    @Override
    public Invocation buildDelete() {
        return build(DELETE);
    }

    @Override
    public Invocation buildPost(Entity<?> entity) {
        return build(POST, entity);
    }

    @Override
    public Invocation buildPut(Entity<?> entity) {
        return build(PUT, entity);
    }

    @Override
    public AsyncInvoker async() {
        return new EverrestAsyncInvoker(this);
    }

    @Override
    public Invocation.Builder accept(String... mediaTypes) {
        request.getHeaders().addAll(ACCEPT, mediaTypes);
        return this;
    }

    @Override
    public Invocation.Builder accept(MediaType... mediaTypes) {
        request.getHeaders().addAll(ACCEPT, mediaTypes);
        return this;
    }

    @Override
    public Invocation.Builder acceptLanguage(Locale... locales) {
        request.getHeaders().addAll(ACCEPT_LANGUAGE, locales);
        return this;
    }

    @Override
    public Invocation.Builder acceptLanguage(String... locales) {
        request.getHeaders().addAll(ACCEPT_LANGUAGE, locales);
        return this;
    }

    @Override
    public Invocation.Builder acceptEncoding(String... encodings) {
        request.getHeaders().addAll(ACCEPT_ENCODING, encodings);
        return this;
    }

    @Override
    public Invocation.Builder cookie(Cookie cookie) {
        request.getHeaders().add(COOKIE, cookie);
        return this;
    }

    @Override
    public Invocation.Builder cookie(String name, String value) {
        return cookie(new Cookie(name, value));
    }

    @Override
    public Invocation.Builder cacheControl(CacheControl cacheControl) {
        return header(CACHE_CONTROL, cacheControl);
    }

    @Override
    public Invocation.Builder header(String name, Object value) {
        if (value == null) {
            request.getHeaders().remove(name);
        } else {
            if (SINGLE_VALUE_HEADERS.contains(caseLess(name))) {
                request.getHeaders().putSingle(name, value);
            } else {
                request.getHeaders().add(name, value);
            }
        }
        return this;
    }

    @Override
    public Invocation.Builder headers(MultivaluedMap<String, Object> headers) {
        request.getHeaders().clear();
        request.getHeaders().putAll(headers);
        return this;
    }

    @Override
    public Invocation.Builder property(String name, Object value) {
        request.setProperty(name, value);
        return this;
    }

    @Override
    public Response get() {
        return buildGet().invoke();
    }

    @Override
    public <T> T get(Class<T> responseType) {
        return buildGet().invoke(responseType);
    }

    @Override
    public <T> T get(GenericType<T> responseType) {
        return buildGet().invoke(responseType);
    }

    @Override
    public Response put(Entity<?> entity) {
        return buildPut(entity).invoke();
    }

    @Override
    public <T> T put(Entity<?> entity, Class<T> responseType) {
        return buildPut(entity).invoke(responseType);
    }

    @Override
    public <T> T put(Entity<?> entity, GenericType<T> responseType) {
        return buildPut(entity).invoke(responseType);
    }

    @Override
    public Response post(Entity<?> entity) {
        return buildPost(entity).invoke();
    }

    @Override
    public <T> T post(Entity<?> entity, Class<T> responseType) {
        return buildPost(entity).invoke(responseType);
    }

    @Override
    public <T> T post(Entity<?> entity, GenericType<T> responseType) {
        return buildPost(entity).invoke(responseType);
    }

    @Override
    public Response delete() {
        return buildDelete().invoke();
    }

    @Override
    public <T> T delete(Class<T> responseType) {
        return buildDelete().invoke(responseType);
    }

    @Override
    public <T> T delete(GenericType<T> responseType) {
        return buildDelete().invoke(responseType);
    }

    @Override
    public Response head() {
        return build(HEAD).invoke();
    }

    @Override
    public Response options() {
        return build(OPTIONS).invoke();
    }

    @Override
    public <T> T options(Class<T> responseType) {
        return build(OPTIONS).invoke(responseType);
    }

    @Override
    public <T> T options(GenericType<T> responseType) {
        return build(OPTIONS).invoke(responseType);
    }

    @Override
    public Response trace() {
        return build("TRACE").invoke();
    }

    @Override
    public <T> T trace(Class<T> responseType) {
        return build("TRACE").invoke(responseType);
    }

    @Override
    public <T> T trace(GenericType<T> responseType) {
        return build("TRACE").invoke(responseType);
    }

    @Override
    public Response method(String name) {
        return build(name).invoke();
    }

    @Override
    public <T> T method(String name, Class<T> responseType) {
        return build(name).invoke(responseType);
    }

    @Override
    public <T> T method(String name, GenericType<T> responseType) {
        return build(name).invoke(responseType);
    }

    @Override
    public Response method(String name, Entity<?> entity) {
        return build(name, entity).invoke();
    }

    @Override
    public <T> T method(String name, Entity<?> entity, Class<T> responseType) {
        return build(name, entity).invoke(responseType);
    }

    @Override
    public <T> T method(String name, Entity<?> entity, GenericType<T> responseType) {
        return build(name, entity).invoke(responseType);
    }

    @VisibleForTesting
    ClientRequest getRequest() {
        return request;
    }

    @VisibleForTesting
    Supplier<ExecutorService> getExecutorProvider() {
        return executorProvider;
    }
}
