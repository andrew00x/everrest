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
import org.everrest.core.ConfigurationProperties;
import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.RuntimeConfigurationProperties;
import org.everrest.core.impl.RuntimeProviderBinder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class EverrestClient implements Client {
    private final SSLContext sslContext;
    private final HostnameVerifier hostNameVerifier;
    private final Supplier<ExecutorService> executorProvider;
    private final ProviderBinder providers;
    private final InvocationPipeline requestInvocationPipeline;
    private final ConfigurationProperties properties;
    private final EverrestConfiguration configuration;
    private final AtomicBoolean closed;
    private final List<Runnable> onCloseTasks;

    EverrestClient(SSLContext sslContext,
                   HostnameVerifier hostNameVerifier,
                   Supplier<ExecutorService> executorProvider,
                   ProviderBinder providers,
                   InvocationPipeline requestInvocationPipeline,
                   ConfigurationProperties properties) {
        this.sslContext = sslContext;
        this.hostNameVerifier = hostNameVerifier;
        this.executorProvider = executorProvider;
        this.providers = providers;
        this.requestInvocationPipeline = requestInvocationPipeline;
        this.properties = properties;
        configuration = new EverrestConfiguration(providers, properties);
        closed = new AtomicBoolean();
        onCloseTasks = new CopyOnWriteArrayList<>();
    }

    public void addOnCloseTask(Runnable task) {
        onCloseTasks.add(task);
    }

    @Override
    public WebTarget target(String uri) {
        return createWebTarget(UriBuilder.fromUri(requireNonNull(uri)));
    }

    @Override
    public WebTarget target(URI uri) {
        return createWebTarget(UriBuilder.fromUri(requireNonNull(uri)));
    }

    @Override
    public WebTarget target(Link link) {
        return target(link.getUri());
    }

    @Override
    public WebTarget target(UriBuilder uriBuilder) {
        return createWebTarget(uriBuilder.clone());
    }

    private WebTarget createWebTarget(UriBuilder uriBuilder) {
        assertNotClosed();
        ConfigurationProperties runtimeProperties = new RuntimeConfigurationProperties(properties);
        ProviderBinder runtimeProviders = new RuntimeProviderBinder(providers, runtimeProperties);
        return new EverrestWebTarget(uriBuilder,
                                     executorProvider,
                                     this,
                                     runtimeProviders,
                                     requestInvocationPipeline,
                                     runtimeProperties);
    }

    @Override
    public Invocation.Builder invocation(Link link) {
        assertNotClosed();
        ClientRequest request = new ClientRequest(link.getUri(), this, providers, new RuntimeConfigurationProperties(properties));
        InvocationBuilder invocationBuilder = new InvocationBuilder(executorProvider, requestInvocationPipeline, request);
        if (link.getType() != null) {
            invocationBuilder.accept(link.getType());
        }
        return invocationBuilder;
    }

    @Override
    public SSLContext getSslContext() {
        assertNotClosed();
        return sslContext;
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        assertNotClosed();
        return hostNameVerifier;
    }

    @Override
    public Configuration getConfiguration() {
        assertNotClosed();
        return configuration;
    }

    @Override
    public Client property(String name, Object value) {
        assertNotClosed();
        configuration.setProperty(name, value);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass) {
        assertNotClosed();
        providers.register(componentClass);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, int priority) {
        assertNotClosed();
        providers.register(componentClass, priority);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, Class<?>... contracts) {
        assertNotClosed();
        providers.register(componentClass, contracts);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        assertNotClosed();
        providers.register(componentClass, contracts);
        return this;
    }

    @Override
    public Client register(Object component) {
        assertNotClosed();
        providers.register(component);
        return this;
    }

    @Override
    public Client register(Object component, int priority) {
        assertNotClosed();
        providers.register(component, priority);
        return this;
    }

    @Override
    public Client register(Object component, Class<?>... contracts) {
        assertNotClosed();
        providers.register(component, contracts);
        return this;
    }

    @Override
    public Client register(Object component, Map<Class<?>, Integer> contracts) {
        assertNotClosed();
        providers.register(component, contracts);
        return this;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            onCloseTasks.forEach(Runnable::run);
            onCloseTasks.clear();
        }
    }

    public boolean isClosed() {
        return closed.get();
    }

    private void assertNotClosed() {
        checkState(!closed.get(), "Client already closed");
    }

    @VisibleForTesting
    Supplier<ExecutorService> getExecutorProvider() {
        return executorProvider;
    }

    @VisibleForTesting
    ProviderBinder getProviders() {
        return providers;
    }
}
