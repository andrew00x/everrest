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

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class EverrestWebTarget implements WebTarget {
    private final UriBuilder uriBuilder;
    private final EverrestConfiguration configuration;
    private final Supplier<ExecutorService> executorProvider;
    private final ProviderBinder providers;
    private final InvocationPipeline requestInvocationPipeline;
    private final ConfigurationProperties properties;
    private final EverrestClient client;

    EverrestWebTarget(UriBuilder uriBuilder,
                      Supplier<ExecutorService> executorProvider,
                      EverrestClient client,
                      ProviderBinder providers,
                      InvocationPipeline requestInvocationPipeline,
                      ConfigurationProperties properties) {
        this.uriBuilder = uriBuilder;
        this.executorProvider = executorProvider;
        this.providers = providers;
        this.requestInvocationPipeline = requestInvocationPipeline;
        this.properties = properties;
        this.client = client;
        this.configuration = new EverrestConfiguration(providers, properties);
    }

    @Override
    public URI getUri() {
        assertNotClosed();
        try {
            return uriBuilder.build();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public UriBuilder getUriBuilder() {
        assertNotClosed();
        return uriBuilder.clone();
    }

    @Override
    public WebTarget path(String path) {
        assertNotClosed();
        requireNonNull(path);
        ConfigurationProperties runtimeProperties = new RuntimeConfigurationProperties(properties);
        ProviderBinder runtimeProviders = new RuntimeProviderBinder(providers, runtimeProperties);
        return new EverrestWebTarget(uriBuilder.clone().path(path),
                                     executorProvider,
                                     client,
                                     runtimeProviders,
                                     requestInvocationPipeline,
                                     runtimeProperties);
    }

    @Override
    public WebTarget resolveTemplate(String name, Object value) {
        return resolveTemplate(name, value, false);
    }

    @Override
    public WebTarget resolveTemplate(String name, Object value, boolean encodeSlashInPath) {
        assertNotClosed();
        requireNonNull(name);
        requireNonNull(value);
        ConfigurationProperties runtimeProperties = new RuntimeConfigurationProperties(properties);
        ProviderBinder runtimeProviders = new RuntimeProviderBinder(providers, runtimeProperties);
        return new EverrestWebTarget(uriBuilder.clone().resolveTemplate(name, value, encodeSlashInPath),
                                     executorProvider,
                                     client,
                                     runtimeProviders,
                                     requestInvocationPipeline,
                                     runtimeProperties);
    }

    @Override
    public WebTarget resolveTemplateFromEncoded(String name, Object value) {
        assertNotClosed();
        requireNonNull(name);
        requireNonNull(value);
        ConfigurationProperties runtimeProperties = new RuntimeConfigurationProperties(properties);
        ProviderBinder runtimeProviders = new RuntimeProviderBinder(providers, runtimeProperties);
        return new EverrestWebTarget(uriBuilder.clone().resolveTemplateFromEncoded(name, value),
                                     executorProvider,
                                     client,
                                     runtimeProviders,
                                     requestInvocationPipeline,
                                     runtimeProperties);
    }

    @Override
    public WebTarget resolveTemplates(Map<String, Object> templateValues) {
        return resolveTemplates(templateValues, false);
    }

    @Override
    public WebTarget resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath) {
        assertNotClosed();
        templateValues.entrySet().forEach(e -> {
            requireNonNull(e.getKey(), "Null key");
            requireNonNull(e.getValue(), "Null value");
        });
        ConfigurationProperties runtimeProperties = new RuntimeConfigurationProperties(properties);
        ProviderBinder runtimeProviders = new RuntimeProviderBinder(providers, runtimeProperties);
        return new EverrestWebTarget(uriBuilder.clone().resolveTemplates(templateValues, encodeSlashInPath),
                                     executorProvider,
                                     client,
                                     runtimeProviders,
                                     requestInvocationPipeline,
                                     runtimeProperties);
    }

    @Override
    public WebTarget resolveTemplatesFromEncoded(Map<String, Object> templateValues) {
        assertNotClosed();
        templateValues.entrySet().forEach(e -> {
            requireNonNull(e.getKey(), "Null key");
            requireNonNull(e.getValue(), "Null value");
        });
        ConfigurationProperties runtimeProperties = new RuntimeConfigurationProperties(properties);
        ProviderBinder runtimeProviders = new RuntimeProviderBinder(providers, runtimeProperties);
        return new EverrestWebTarget(uriBuilder.clone().resolveTemplatesFromEncoded(templateValues),
                                     executorProvider,
                                     client,
                                     runtimeProviders,
                                     requestInvocationPipeline,
                                     runtimeProperties);
    }

    @Override
    public WebTarget matrixParam(String name, Object... values) {
        assertNotClosed();
        requireNonNull(name);
        Arrays.stream(values).forEach(Objects::requireNonNull);
        ConfigurationProperties runtimeProperties = new RuntimeConfigurationProperties(properties);
        ProviderBinder runtimeProviders = new RuntimeProviderBinder(providers, runtimeProperties);
        return new EverrestWebTarget(uriBuilder.clone().matrixParam(name, values),
                                     executorProvider,
                                     client,
                                     runtimeProviders,
                                     requestInvocationPipeline,
                                     runtimeProperties);
    }

    @Override
    public WebTarget queryParam(String name, Object... values) {
        assertNotClosed();
        requireNonNull(name);
        Arrays.stream(values).forEach(Objects::requireNonNull);
        ConfigurationProperties runtimeProperties = new RuntimeConfigurationProperties(properties);
        ProviderBinder runtimeProviders = new RuntimeProviderBinder(providers, runtimeProperties);
        return new EverrestWebTarget(uriBuilder.clone().queryParam(name, values),
                                     executorProvider,
                                     client,
                                     runtimeProviders,
                                     requestInvocationPipeline,
                                     runtimeProperties);
    }

    @Override
    public Invocation.Builder request() {
        assertNotClosed();
        ClientRequest request = new ClientRequest(uriBuilder.build(), client, providers, new RuntimeConfigurationProperties(properties));
        return new InvocationBuilder(executorProvider, requestInvocationPipeline, request);
    }

    @Override
    public Invocation.Builder request(String... acceptedResponseTypes) {
        return request().accept(acceptedResponseTypes);
    }

    @Override
    public Invocation.Builder request(MediaType... acceptedResponseTypes) {
        return request().accept(acceptedResponseTypes);
    }

    @Override
    public Configuration getConfiguration() {
        assertNotClosed();
        return configuration;
    }

    @Override
    public WebTarget property(String name, Object value) {
        assertNotClosed();
        configuration.setProperty(name, value);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass) {
        assertNotClosed();
        providers.register(componentClass);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass, int priority) {
        assertNotClosed();
        providers.register(componentClass, priority);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass, Class<?>... contracts) {
        assertNotClosed();
        providers.register(componentClass, contracts);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        assertNotClosed();
        providers.register(componentClass, contracts);
        return this;
    }

    @Override
    public WebTarget register(Object component) {
        assertNotClosed();
        providers.register(component);
        return this;
    }

    @Override
    public WebTarget register(Object component, int priority) {
        assertNotClosed();
        providers.register(component, priority);
        return this;
    }

    @Override
    public WebTarget register(Object component, Class<?>... contracts) {
        assertNotClosed();
        providers.register(component, contracts);
        return this;
    }

    @Override
    public WebTarget register(Object component, Map<Class<?>, Integer> contracts) {
        assertNotClosed();
        providers.register(component, contracts);
        return this;
    }

    private void assertNotClosed() {
        checkState(!isClosed(), "Client already closed");
    }

    @VisibleForTesting
    boolean isClosed() {
        return client.isClosed();
    }

    @VisibleForTesting
    Supplier<ExecutorService> getExecutorProvider() {
        return executorProvider;
    }

    @VisibleForTesting
    ProviderBinder getProviders() {
        return providers;
    }

    @VisibleForTesting
    EverrestClient getClient() {
        return client;
    }
}
