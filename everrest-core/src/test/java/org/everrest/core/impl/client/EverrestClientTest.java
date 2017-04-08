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
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.everrest.core.ConfigurationProperties;
import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.DefaultProviderBinder;
import org.everrest.core.impl.provider.StringEntityProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class EverrestClientTest {
    @Rule public ExpectedException thrown = ExpectedException.none();

    private SSLContext sslContext;
    private HostnameVerifier hostnameVerifier;
    private Supplier<ExecutorService> executorProvider;
    private ProviderBinder providers;
    private InvocationPipeline requestInvocationPipeline;
    private ConfigurationProperties properties;

    private EverrestClient client;

    @Before
    public void setUp() throws Exception {
        sslContext = mock(SSLContext.class);
        hostnameVerifier = mock(HostnameVerifier.class);
        executorProvider = mock(Supplier.class);
        providers = mock(DefaultProviderBinder.class);
        requestInvocationPipeline = mock(InvocationPipeline.class);
        properties = mock(ConfigurationProperties.class);
        client = new EverrestClient(sslContext, hostnameVerifier, executorProvider, providers, requestInvocationPipeline, properties);
    }

    @Test
    public void getsSslContext() {
        assertEquals(sslContext, client.getSslContext());
    }

    @Test
    public void getsHostnameVerifier() {
        assertEquals(hostnameVerifier, client.getHostnameVerifier());
    }

    @Test
    public void getsConfiguration() {
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));
        when(providers.isRegistered(StringEntityProvider.class)).thenReturn(true);
        Configuration configuration = client.getConfiguration();

        assertEquals(ImmutableMap.of("name", "value"), configuration.getProperties());
        assertTrue(configuration.isRegistered(StringEntityProvider.class));
    }

    @Test
    public void setsProperty() {
        client.property("name", "value");
        verify(properties).setProperty("name", "value");
    }

    @Test
    public void registersComponentClass() {
        client.register(StringEntityProvider.class);
        verify(providers).register(StringEntityProvider.class);
    }

    @Test
    public void registersComponentClassWithPriority() {
        client.register(StringEntityProvider.class, 99);
        verify(providers).register(StringEntityProvider.class, 99);
    }

    @Test
    public void registersComponentClassWithContracts() {
        client.register(StringEntityProvider.class, MessageBodyReader.class, MessageBodyWriter.class);
        verify(providers).register(StringEntityProvider.class, MessageBodyReader.class, MessageBodyWriter.class);
    }

    @Test
    public void registersComponentClassWithContractAndPriorities() {
        client.register(StringEntityProvider.class, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 2));
        verify(providers).register(StringEntityProvider.class, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 2));
    }

    @Test
    public void registersComponent() {
        StringEntityProvider component = new StringEntityProvider();
        client.register(component);
        verify(providers).register(component);
    }

    @Test
    public void registersComponentWithPriority() {
        StringEntityProvider component = new StringEntityProvider();
        client.register(component, 99);
        verify(providers).register(component, 99);
    }

    @Test
    public void registersComponentWithContracts() {
        StringEntityProvider component = new StringEntityProvider();
        client.register(component, MessageBodyReader.class, MessageBodyWriter.class);
        verify(providers).register(component, MessageBodyReader.class, MessageBodyWriter.class);
    }

    @Test
    public void registersComponentWithContractAndPriorities() {
        StringEntityProvider component = new StringEntityProvider();
        client.register(component, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 2));
        verify(providers).register(component, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 2));
    }

    @Test
    public void createsWebTargetFromUriTemplate() {
        String uriTemplate = "http://localhost:8080/a/b/c/{foo}/{bar}";
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));
        EverrestWebTarget target = (EverrestWebTarget) client.target(uriTemplate);

        assertNotNull(target);
        assertEquals(uriTemplate, target.getUriBuilder().toTemplate());
        assertSame(executorProvider, target.getExecutorProvider());
        assertSame(client, target.getClient());
        assertEquals(ImmutableMap.of("name", "value"), target.getConfiguration().getProperties());
    }

    @Test
    public void createsWebTargetFromUri() {
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));
        URI uri = URI.create("http://localhost:8080/a/b/c");
        EverrestWebTarget target = (EverrestWebTarget) client.target(uri);

        assertNotNull(target);
        assertEquals(uri, target.getUri());
        assertSame(executorProvider, target.getExecutorProvider());
        assertSame(client, target.getClient());
        assertEquals(ImmutableMap.of("name", "value"), target.getConfiguration().getProperties());
    }

    @Test
    public void createsWebTargetFromUriBuilder() {
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));
        URI uri = URI.create("http://localhost:8080/a/b/c");
        UriBuilder uriBuilder = mockUriBuilder(uri);

        EverrestWebTarget target = (EverrestWebTarget) client.target(uriBuilder);

        assertNotNull(target);
        assertEquals(uri, target.getUri());
        assertSame(executorProvider, target.getExecutorProvider());
        assertSame(client, target.getClient());
        assertEquals(ImmutableMap.of("name", "value"), target.getConfiguration().getProperties());
    }

    @Test
    public void createsWebTargetFromLink() {
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));
        URI uri = URI.create("http://localhost:8080/a/b/c");
        Link link = mockLink(uri, null);

        EverrestWebTarget target = (EverrestWebTarget) client.target(link);

        assertNotNull(target);
        assertEquals(uri, target.getUri());
        assertSame(executorProvider, target.getExecutorProvider());
        assertSame(client, target.getClient());
        assertEquals(ImmutableMap.of("name", "value"), target.getConfiguration().getProperties());
    }

    @Test
    public void createsInvocationBuilder() {
        URI uri = URI.create("http://localhost:8080/a/b/c");
        String type = "application/json";
        Link link = mockLink(uri, type);
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));

        InvocationBuilder invocationBuilder = (InvocationBuilder) client.invocation(link);

        assertNotNull(invocationBuilder);
        assertSame(executorProvider, invocationBuilder.getExecutorProvider());
        ClientRequest request = invocationBuilder.getRequest();
        assertEquals(uri, request.getUri());
        assertEquals(newArrayList("application/json"), request.getHeaders().get(ACCEPT));
        assertSame(client, request.getClient());
        assertSame(providers, request.getProviders());
        assertEquals(ImmutableMap.of("name", "value"), request.getConfiguration().getProperties());
    }

    @Test
    public void closesRelatedWebTargets() {
        Runnable task = mock(Runnable.class);
        client.addOnCloseTask(task);
        WebTarget target = client.target("http://localhost:8080/a/b/c/{foo}/{bar}");
        client.close();
        verify(task).run();
        assertTrue(((EverrestWebTarget) target).isClosed());
    }

    @Test
    public void subsequentCallsOfCloseAreIgnored() {
        Runnable task = mock(Runnable.class);
        client.addOnCloseTask(task);
        client.close();
        client.close();
        verify(task).run();
    }

    private static Link mockLink(URI uri, String type) {
        Link link = mock(Link.class);
        when(link.getType()).thenReturn(type);
        when(link.getUri()).thenReturn(uri);
        return link;
    }

    private UriBuilder mockUriBuilder(URI uri) {
        UriBuilder uriBuilder = mock(UriBuilder.class);
        UriBuilder clonedUriBuilder = mock(UriBuilder.class);
        when(uriBuilder.clone()).thenReturn(clonedUriBuilder);
        when(clonedUriBuilder.build()).thenReturn(uri);
        return uriBuilder;
    }

    @Test
    @UseDataProvider("dataForThrowsIllegalStateExceptionWhenCallMethodOnClosedInstance")
    public void throwsIllegalStateExceptionWhenCallMethodOnClosedInstance(Consumer<EverrestClient> consumer) {
        client.close();
        thrown.expect(IllegalStateException.class);
        consumer.accept(client);
    }

    @DataProvider
    public static Object[][] dataForThrowsIllegalStateExceptionWhenCallMethodOnClosedInstance() {
        return new Object[][] {
                {(Consumer<EverrestClient>) Configurable::getConfiguration},
                {(Consumer<EverrestClient>) Client::getSslContext},
                {(Consumer<EverrestClient>) Client::getHostnameVerifier},
                {(Consumer<EverrestClient>) c -> c.property("x", "y")},
                {(Consumer<EverrestClient>) c -> c.register(StringEntityProvider.class)},
                {(Consumer<EverrestClient>) c -> c.register(StringEntityProvider.class, 3)},
                {(Consumer<EverrestClient>) c -> c.register(StringEntityProvider.class, MessageBodyReader.class, MessageBodyWriter.class)},
                {(Consumer<EverrestClient>) c -> c.register(StringEntityProvider.class, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 2))},
                {(Consumer<EverrestClient>) c -> c.register(new StringEntityProvider())},
                {(Consumer<EverrestClient>) c -> c.register(new StringEntityProvider(), 3)},
                {(Consumer<EverrestClient>) c -> c.register(new StringEntityProvider(), MessageBodyReader.class, MessageBodyWriter.class)},
                {(Consumer<EverrestClient>) c -> c.register(new StringEntityProvider(), ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 2))},
                {(Consumer<EverrestClient>) c -> c.target("http://test.com")},
                {(Consumer<EverrestClient>) c -> c.target(URI.create("http://test.com"))},
                {(Consumer<EverrestClient>) c -> c.target(mock(UriBuilder.class))},
                {(Consumer<EverrestClient>) c -> c.target(mockLink(URI.create("http://test.com"), null))},
                {(Consumer<EverrestClient>) c -> c.invocation(mock(Link.class))}
        };
    }
}