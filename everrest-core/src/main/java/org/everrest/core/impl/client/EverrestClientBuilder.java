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
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.everrest.core.ConfigurationProperties;
import org.everrest.core.ProviderBinder;
import org.everrest.core.SimpleConfigurationProperties;
import org.everrest.core.impl.DefaultProviderBinder;
import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.provider.ClientEmbeddedProvidersFeature;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static javax.ws.rs.RuntimeType.CLIENT;

public class EverrestClientBuilder extends ClientBuilder {
    private final ProviderBinder providers;
    private final InvocationPipeline requestInvocationPipeline;
    private SSLContext sslContext;
    private KeyStore keyStore;
    private char[] keyStorePassword;
    private KeyStore trustStore;
    private HostnameVerifier hostNameVerifier;
    private EverrestConfiguration configuration;
    private ConfigurationProperties properties;

    public EverrestClientBuilder() {
        this.properties = new SimpleConfigurationProperties();
        this.providers = new DefaultProviderBinder(CLIENT, properties);
        this.providers.register(new ClientEmbeddedProvidersFeature());
        this.requestInvocationPipeline = new InvocationPipeline();
        this.configuration = new EverrestConfiguration(providers, properties);
    }

    EverrestClientBuilder(ProviderBinder providers, InvocationPipeline requestInvocationPipeline, ConfigurationProperties properties) {
        this.requestInvocationPipeline = requestInvocationPipeline;
        this.properties = properties;
        this.providers = providers;
        this.configuration = new EverrestConfiguration(providers, properties);
    }

    @Override
    public ClientBuilder withConfig(Configuration newConfiguration) {
        providers.clear();
        properties = new SimpleConfigurationProperties(newConfiguration.getProperties());
        Set<Class<?>> registeredAsInstances = new HashSet<>();
        newConfiguration.getInstances().forEach(component -> {
            providers.register(component, newConfiguration.getContracts(component.getClass()));
            registeredAsInstances.add(component.getClass());
        });
        newConfiguration.getClasses().stream()
                .filter(componentClass -> !registeredAsInstances.contains(componentClass))
                .forEach(componentClass -> providers.register(componentClass, newConfiguration.getContracts(componentClass)));
        configuration = new EverrestConfiguration(providers, properties);
        return this;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public ClientBuilder sslContext(SSLContext sslContext) {
        this.sslContext = requireNonNull(sslContext);
        keyStore = null;
        keyStorePassword = null;
        trustStore = null;
        return this;
    }

    @Override
    public ClientBuilder keyStore(KeyStore keyStore, char[] password) {
        this.keyStore = requireNonNull(keyStore);
        keyStorePassword = Arrays.copyOf(password, password.length);
        sslContext = null;
        return this;
    }

    @Override
    public ClientBuilder trustStore(KeyStore trustStore) {
        this.trustStore = requireNonNull(trustStore);
        sslContext = null;
        return this;
    }

    @Override
    public ClientBuilder hostnameVerifier(HostnameVerifier verifier) {
        this.hostNameVerifier = verifier;
        return this;
    }

    @Override
    public Client build() {
        SSLContext sslContext;
        try {
            sslContext = buildSslContextIfNeed();
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyManagementException | KeyStoreException e) {
            throw Throwables.propagate(e);
        }
        ExecutorServiceSupplier executorSupplier = new ExecutorServiceSupplier();
        ConfigurationProperties clientProperties = new SimpleConfigurationProperties(properties);
        ProviderBinder clientProviders = new DefaultProviderBinder(this.providers.getRuntimeType(), clientProperties);
        clientProviders.copyComponentsFrom(this.providers);
        EverrestClient client = new EverrestClient(sslContext,
                                                   hostNameVerifier,
                                                   executorSupplier,
                                                   clientProviders,
                                                   requestInvocationPipeline,
                                                   clientProperties);
        client.addOnCloseTask(executorSupplier.get()::shutdown);
        return client;
    }

    private static class ExecutorServiceSupplier implements Supplier<ExecutorService> {
        static ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("everrest.EverrestClient-%d").setDaemon(true).build();
        volatile ExecutorService executor;

        @Override
        public ExecutorService get() {
            ExecutorService theExecutor = executor;
            if (theExecutor == null) {
                synchronized(this) {
                    theExecutor = executor;
                    if (theExecutor == null) {
                        executor = theExecutor = newCachedThreadPool(threadFactory);
                    }
                }
            }
            return theExecutor;
        }
    }

    private SSLContext buildSslContextIfNeed() throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeyManagementException {
        SSLContext theSslContext = this.sslContext;
        if (theSslContext == null) {
            KeyManager[] keyManagers = null;
            TrustManager[] trustManagers = null;
            if (keyStore != null) {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, keyStorePassword);
                keyManagers = kmf.getKeyManagers();
            }
            if (trustStore != null) {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
                trustManagers = tmf.getTrustManagers();
            }
            theSslContext = SSLContext.getInstance("TLS");
            theSslContext.init(keyManagers, trustManagers, null);
        }
        return theSslContext;
    }

    @Override
    public ClientBuilder property(String name, Object value) {
        configuration.setProperty(name, value);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass) {
        providers.register(componentClass);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, int priority) {
        providers.register(componentClass, priority);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, Class<?>... contracts) {
        providers.register(componentClass, contracts);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        providers.register(componentClass, contracts);
        return this;
    }

    @Override
    public ClientBuilder register(Object component) {
        providers.register(component);
        return this;
    }

    @Override
    public ClientBuilder register(Object component, int priority) {
        providers.register(component, priority);
        return this;
    }

    @Override
    public ClientBuilder register(Object component, Class<?>... contracts) {
        providers.register(component, contracts);
        return this;
    }

    @Override
    public ClientBuilder register(Object component, Map<Class<?>, Integer> contracts) {
        providers.register(component, contracts);
        return this;
    }

    @VisibleForTesting
    SSLContext getSslContext() {
        return sslContext;
    }

    @VisibleForTesting
    KeyStore getKeyStore() {
        return keyStore;
    }

    @VisibleForTesting
    char[] getKeyStorePassword() {
        return keyStorePassword;
    }

    @VisibleForTesting
    KeyStore getTrustStore() {
        return trustStore;
    }

    @VisibleForTesting
    HostnameVerifier getHostNameVerifier() {
        return hostNameVerifier;
    }

    @VisibleForTesting
    ProviderBinder getProviders() {
        return providers;
    }
}
