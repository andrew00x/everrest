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

import com.google.common.collect.ImmutableMap;
import org.everrest.core.ConfigurationProperties;
import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.DefaultProviderBinder;
import org.everrest.core.impl.provider.ByteEntityProvider;
import org.everrest.core.impl.provider.StringEntityProvider;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.security.KeyStore;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static javax.ws.rs.RuntimeType.CLIENT;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EverrestClientBuilderTest {
    private ProviderBinder providers;
    private ConfigurationProperties properties;

    private EverrestClientBuilder clientBuilder;

    @Before
    public void setUp() throws Exception {
        providers = mock(DefaultProviderBinder.class);
        InvocationPipeline requestInvocationPipeline = mock(InvocationPipeline.class);
        properties = mock(ConfigurationProperties.class);
        clientBuilder = new EverrestClientBuilder(providers, requestInvocationPipeline, properties);
    }

    @Test
    public void updatesStateFromGivenConfiguration() {
        Map<String, Object> properties = ImmutableMap.of("key", "value");
        StringEntityProvider stringEntityProvider = new StringEntityProvider();
        Set<Object> instances = newHashSet(stringEntityProvider);
        Set<Class<?>> classes = newHashSet(StringEntityProvider.class, ByteEntityProvider.class);
        Map<Class<?>, Integer> stringEntityProviderContracts = ImmutableMap.of(MessageBodyReader.class, 1);
        Map<Class<?>, Integer> byteEntityProviderContracts = ImmutableMap.of(MessageBodyWriter.class, 2);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getProperties()).thenReturn(properties);
        when(configuration.getInstances()).thenReturn(instances);
        when(configuration.getClasses()).thenReturn(classes);
        when(configuration.getContracts(StringEntityProvider.class)).thenReturn(stringEntityProviderContracts);
        when(configuration.getContracts(ByteEntityProvider.class)).thenReturn(byteEntityProviderContracts);

        clientBuilder.withConfig(configuration);

        assertEquals(properties, clientBuilder.getConfiguration().getProperties());
        verify(providers).register(stringEntityProvider, stringEntityProviderContracts);
        verify(providers).register(ByteEntityProvider.class, byteEntityProviderContracts);
    }

    @Test
    public void setsSslContext() throws Exception {
        SSLContext sslContext = SSLContext.getDefault();
        clientBuilder.sslContext(sslContext);
        assertEquals(sslContext, clientBuilder.getSslContext());
    }

    @Test(expected = NullPointerException.class)
    public void throwsExceptionWhenTrySetNullSslContext() {
        clientBuilder.sslContext(null);
    }

    @Test
    public void resetsKeyStoreItsPasswordAndTrustStoreWhenSslContextIsSet() throws Exception {
        clientBuilder.keyStore(KeyStore.getInstance(KeyStore.getDefaultType()), "password".toCharArray())
                .trustStore(KeyStore.getInstance(KeyStore.getDefaultType()));

        clientBuilder.sslContext(SSLContext.getDefault());

        assertNull(clientBuilder.getKeyStore());
        assertNull(clientBuilder.getKeyStorePassword());
        assertNull(clientBuilder.getTrustStore());
    }

    @Test
    public void setsKeyStoreAndPassword() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "password".toCharArray();
        clientBuilder.keyStore(keyStore, password);
        assertEquals(keyStore, clientBuilder.getKeyStore());
        assertArrayEquals(password, clientBuilder.getKeyStorePassword());
    }

    @Test(expected = NullPointerException.class)
    public void throwsExceptionWhenTrySetNullKeyStore() {
        clientBuilder.keyStore(null, "password".toCharArray());
    }

    @Test(expected = NullPointerException.class)
    public void throwsExceptionWhenTrySetNullKeyStorePassword() throws Exception {
        clientBuilder.keyStore(KeyStore.getInstance(KeyStore.getDefaultType()), (char[])null);
    }

    @Test
    public void resetsSslContextWhenKeyStoreIsSet() throws Exception {
        clientBuilder.sslContext(SSLContext.getDefault());
        clientBuilder.keyStore(KeyStore.getInstance(KeyStore.getDefaultType()), "password".toCharArray());
        assertNull(clientBuilder.getSslContext());
    }

    @Test
    public void setsTrustStore() throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        clientBuilder.trustStore(trustStore);
        assertEquals(trustStore, clientBuilder.getTrustStore());
    }

    @Test(expected = NullPointerException.class)
    public void throwsExceptionWhenTrySetNullTrustStore() {
        clientBuilder.trustStore(null);
    }

    @Test
    public void resetsSslContextWhenTrustStoreIsSet() throws Exception {
        clientBuilder.sslContext(SSLContext.getDefault());
        clientBuilder.trustStore(KeyStore.getInstance(KeyStore.getDefaultType()));
        assertNull(clientBuilder.getSslContext());
    }

    @Test
    public void setsHostNameVerifier() {
        HostnameVerifier hostnameVerifier = mock(HostnameVerifier.class);
        clientBuilder.hostnameVerifier(hostnameVerifier);
        assertEquals(hostnameVerifier, clientBuilder.getHostNameVerifier());
    }

    @Test
    public void setsProperty() {
        clientBuilder.property("name", "value");
        verify(properties).setProperty("name", "value");
    }

    @Test
    public void registersComponentClass() {
        clientBuilder.register(StringEntityProvider.class);
        verify(providers).register(StringEntityProvider.class);
    }

    @Test
    public void registersComponentClassWithPriority() {
        clientBuilder.register(StringEntityProvider.class, 99);
        verify(providers).register(StringEntityProvider.class, 99);
    }

    @Test
    public void registersComponentClassWithContracts() {
        clientBuilder.register(StringEntityProvider.class, MessageBodyReader.class, MessageBodyWriter.class);
        verify(providers).register(StringEntityProvider.class, MessageBodyReader.class, MessageBodyWriter.class);
    }

    @Test
    public void registersComponentClassWithContractAndPriorities() {
        clientBuilder.register(StringEntityProvider.class, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 2));
        verify(providers).register(StringEntityProvider.class, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 2));
    }

    @Test
    public void registersComponent() {
        StringEntityProvider component = new StringEntityProvider();
        clientBuilder.register(component);
        verify(providers).register(component);
    }

    @Test
    public void registersComponentWithPriority() {
        StringEntityProvider component = new StringEntityProvider();
        clientBuilder.register(component, 99);
        verify(providers).register(component, 99);
    }

    @Test
    public void registersComponentWithContracts() {
        StringEntityProvider component = new StringEntityProvider();
        clientBuilder.register(component, MessageBodyReader.class, MessageBodyWriter.class);
        verify(providers).register(component, MessageBodyReader.class, MessageBodyWriter.class);
    }

    @Test
    public void registersComponentWithContractAndPriorities() {
        StringEntityProvider component = new StringEntityProvider();
        clientBuilder.register(component, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 2));
        verify(providers).register(component, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 2));
    }

    @Test
    public void buildsClient() throws Exception {
        SSLContext sslContext = SSLContext.getDefault();
        HostnameVerifier verifier = mock(HostnameVerifier.class);
        EverrestClient client = (EverrestClient) clientBuilder
                .sslContext(sslContext)
                .hostnameVerifier(verifier)
                .property("name", "value")
                .build();
        assertNotNull(client);
        assertNotNull(client.getExecutorProvider());
        assertSame(sslContext, client.getSslContext());
        assertSame(verifier, client.getHostnameVerifier());
        verify(properties).setProperty("name", "value");
    }

    @Test
    public void buildsClientUsesTrustStoreAndKeyStoreToBuildSSLContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        char[] keyStorePassword = "secret".toCharArray();
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        EverrestClient client = (EverrestClient) clientBuilder
                .trustStore(trustStore)
                .keyStore(keyStore, keyStorePassword)
                .build();
        assertNotNull(client.getSslContext());
    }

    @Test
    public void initializesClientBuilderWithDefaultProvidersBinder() {
        EverrestClientBuilder clientBuilder = (EverrestClientBuilder) ClientBuilder.newBuilder();
        assertNotNull(clientBuilder.getProviders());
        assertEquals(CLIENT, clientBuilder.getProviders().getRuntimeType());
    }
}
