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

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.concurrent.Future;

import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;

class EverrestAsyncInvoker implements AsyncInvoker {
    private final InvocationBuilder builder;

    EverrestAsyncInvoker(InvocationBuilder builder) {
        this.builder = builder;
    }

    @Override
    public Future<Response> get() {
        return builder.buildGet().submit();
    }

    @Override
    public <T> Future<T> get(Class<T> responseType) {
        return builder.buildGet().submit(responseType);
    }

    @Override
    public <T> Future<T> get(GenericType<T> responseType) {
        return builder.buildGet().submit(responseType);
    }

    @Override
    public <T> Future<T> get(InvocationCallback<T> callback) {
        return builder.buildGet().submit(callback);
    }

    @Override
    public Future<Response> put(Entity<?> entity) {
        return builder.buildPut(entity).submit();
    }

    @Override
    public <T> Future<T> put(Entity<?> entity, Class<T> responseType) {
        return builder.buildPut(entity).submit(responseType);
    }

    @Override
    public <T> Future<T> put(Entity<?> entity, GenericType<T> responseType) {
        return builder.buildPut(entity).submit(responseType);
    }

    @Override
    public <T> Future<T> put(Entity<?> entity, InvocationCallback<T> callback) {
        return builder.buildPut(entity).submit(callback);
    }

    @Override
    public Future<Response> post(Entity<?> entity) {
        return builder.buildPost(entity).submit();
    }

    @Override
    public <T> Future<T> post(Entity<?> entity, Class<T> responseType) {
        return builder.buildPost(entity).submit(responseType);
    }

    @Override
    public <T> Future<T> post(Entity<?> entity, GenericType<T> responseType) {
        return builder.buildPost(entity).submit(responseType);
    }

    @Override
    public <T> Future<T> post(Entity<?> entity, InvocationCallback<T> callback) {
        return builder.buildPost(entity).submit(callback);
    }

    @Override
    public Future<Response> delete() {
        return builder.buildDelete().submit();
    }

    @Override
    public <T> Future<T> delete(Class<T> responseType) {
        return builder.buildDelete().submit(responseType);
    }

    @Override
    public <T> Future<T> delete(GenericType<T> responseType) {
        return builder.buildDelete().submit(responseType);
    }

    @Override
    public <T> Future<T> delete(InvocationCallback<T> callback) {
        return builder.buildDelete().submit(callback);
    }

    @Override
    public Future<Response> head() {
        return builder.build(HEAD).submit();
    }

    @Override
    public Future<Response> head(InvocationCallback<Response> callback) {
        return builder.build(HEAD).submit(callback);
    }

    @Override
    public Future<Response> options() {
        return builder.build(OPTIONS).submit();
    }

    @Override
    public <T> Future<T> options(Class<T> responseType) {
        return builder.build(OPTIONS).submit(responseType);
    }

    @Override
    public <T> Future<T> options(GenericType<T> responseType) {
        return builder.build(OPTIONS).submit(responseType);
    }

    @Override
    public <T> Future<T> options(InvocationCallback<T> callback) {
        return builder.build(OPTIONS).submit(callback);
    }

    @Override
    public Future<Response> trace() {
        return builder.build("TRACE").submit();
    }

    @Override
    public <T> Future<T> trace(Class<T> responseType) {
        return builder.build("TRACE").submit(responseType);
    }

    @Override
    public <T> Future<T> trace(GenericType<T> responseType) {
        return builder.build("TRACE").submit(responseType);
    }

    @Override
    public <T> Future<T> trace(InvocationCallback<T> callback) {
        return builder.build("TRACE").submit(callback);
    }

    @Override
    public Future<Response> method(String name) {
        return builder.build(name).submit();
    }

    @Override
    public <T> Future<T> method(String name, Class<T> responseType) {
        return builder.build(name).submit(responseType);
    }

    @Override
    public <T> Future<T> method(String name, GenericType<T> responseType) {
        return builder.build(name).submit(responseType);
    }

    @Override
    public <T> Future<T> method(String name, InvocationCallback<T> callback) {
        return builder.build(name).submit(callback);
    }

    @Override
    public Future<Response> method(String name, Entity<?> entity) {
        return builder.build(name, entity).submit();
    }

    @Override
    public <T> Future<T> method(String name, Entity<?> entity, Class<T> responseType) {
        return builder.build(name, entity).submit(responseType);
    }

    @Override
    public <T> Future<T> method(String name, Entity<?> entity, GenericType<T> responseType) {
        return builder.build(name, entity).submit(responseType);
    }

    @Override
    public <T> Future<T> method(String name, Entity<?> entity, InvocationCallback<T> callback) {
        return builder.build(name, entity).submit(callback);
    }
}
