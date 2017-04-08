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

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class EverrestWebTargetTest {
    @Rule public ExpectedException thrown = ExpectedException.none();

    private final URI uri = URI.create("http://localhost:8080/a/b/c/");
    private UriBuilder uriBuilder;
    private UriBuilder clonedUriBuilder;
    private Supplier<ExecutorService> executorProvider;
    private EverrestClient client;
    private ProviderBinder providers;
    private InvocationPipeline requestInvocationPipeline;
    private ConfigurationProperties properties;

    private EverrestWebTarget webTarget;

    @Before
    public void setUp() {
        uriBuilder = mockUriBuilder(uri);
        clonedUriBuilder = mockUriBuilder(uri);
        when(uriBuilder.clone()).thenReturn(clonedUriBuilder);
        executorProvider = mock(Supplier.class);
        providers = mock(DefaultProviderBinder.class);
        requestInvocationPipeline = mock(InvocationPipeline.class);
        properties = mock(ConfigurationProperties.class);
        client = mock(EverrestClient.class);
        webTarget = new EverrestWebTarget(uriBuilder, executorProvider, client, providers, requestInvocationPipeline, properties);
    }

    private UriBuilder mockUriBuilder(URI uri) {
        UriBuilder uriBuilder = mock(UriBuilder.class);
        when(uriBuilder.build()).thenReturn(uri);
        return uriBuilder;
    }

    @Test
    public void getsConfiguration() {
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));
        when(providers.isRegistered(StringEntityProvider.class)).thenReturn(true);
        Configuration configuration = webTarget.getConfiguration();

        assertEquals(ImmutableMap.of("name", "value"), configuration.getProperties());
        assertTrue(configuration.isRegistered(StringEntityProvider.class));
    }

    @Test
    public void getsUri() {
        assertEquals(uri, webTarget.getUri());
    }

    @Test
    public void getsUriBuilder() {
        assertEquals(clonedUriBuilder, webTarget.getUriBuilder());
    }

    @Test
    public void createsNewWebTargetWithGivenPath() {
        UriBuilder appendedUriBuilder = mockUriBuilder(uri.resolve("d"));
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));
        when(clonedUriBuilder.path("d")).thenReturn(appendedUriBuilder);

        EverrestWebTarget newWebTarget = (EverrestWebTarget) webTarget.path("d");

        assertNotSame(webTarget, newWebTarget);
        assertEquals(uri.resolve("d"), newWebTarget.getUri());
        assertSame(executorProvider, newWebTarget.getExecutorProvider());
        assertSame(client, newWebTarget.getClient());
        assertEquals(ImmutableMap.of("name", "value"), newWebTarget.getConfiguration().getProperties());
    }

    @Test
    public void createsNewWebTargetByResolvingUriTemplate() {
        UriBuilder resolvedUriBuilder = mockUriBuilder(uri.resolve("value"));
        when(clonedUriBuilder.resolveTemplate("name", "value", false)).thenReturn(resolvedUriBuilder);
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));

        EverrestWebTarget newWebTarget = (EverrestWebTarget) webTarget.resolveTemplate("name", "value");

        assertNotSame(webTarget, newWebTarget);
        assertEquals(uri.resolve("value"), newWebTarget.getUri());
        assertSame(executorProvider, newWebTarget.getExecutorProvider());
        assertSame(client, newWebTarget.getClient());
        assertEquals(ImmutableMap.of("name", "value"), newWebTarget.getConfiguration().getProperties());
    }

    @Test
    public void createsNewWebTargetByResolvingUriTemplateWithEncodingSlashesInPath() {
        UriBuilder resolvedUriBuilder = mockUriBuilder(uri.resolve("va%2Flue"));
        when(clonedUriBuilder.resolveTemplate("name", "va/lue", true)).thenReturn(resolvedUriBuilder);
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));

        EverrestWebTarget newWebTarget = (EverrestWebTarget) webTarget.resolveTemplate("name", "va/lue", true);

        assertNotSame(webTarget, newWebTarget);
        assertEquals(uri.resolve("va%2Flue"), newWebTarget.getUri());
        assertSame(executorProvider, newWebTarget.getExecutorProvider());
        assertSame(client, newWebTarget.getClient());
        assertEquals(ImmutableMap.of("name", "value"), newWebTarget.getConfiguration().getProperties());
    }

    @Test
    public void createsNewWebTargetByResolvingUriTemplateFromEncoded() {
        UriBuilder resolvedUriBuilder = mockUriBuilder(uri.resolve("va%20lue"));
        when(clonedUriBuilder.resolveTemplateFromEncoded("name", "va%20lue")).thenReturn(resolvedUriBuilder);
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));

        EverrestWebTarget newWebTarget = (EverrestWebTarget) webTarget.resolveTemplateFromEncoded("name", "va%20lue");

        assertNotSame(webTarget, newWebTarget);
        assertEquals(uri.resolve("va%20lue"), newWebTarget.getUri());
        assertSame(executorProvider, newWebTarget.getExecutorProvider());
        assertSame(client, newWebTarget.getClient());
        assertEquals(ImmutableMap.of("name", "value"), newWebTarget.getConfiguration().getProperties());
    }

    @Test
    public void createsNewWebTargetByResolvingUriTemplates() {
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));
        UriBuilder resolvedUriBuilder = mockUriBuilder(uri.resolve("value"));
        when(clonedUriBuilder.resolveTemplates(ImmutableMap.of("name", "value"), false)).thenReturn(resolvedUriBuilder);

        EverrestWebTarget newWebTarget = (EverrestWebTarget) webTarget.resolveTemplates(ImmutableMap.of("name", "value"));

        assertNotSame(webTarget, newWebTarget);
        assertEquals(uri.resolve("value"), newWebTarget.getUri());
        assertSame(executorProvider, newWebTarget.getExecutorProvider());
        assertSame(client, newWebTarget.getClient());
        assertEquals(ImmutableMap.of("name", "value"), newWebTarget.getConfiguration().getProperties());
    }

    @Test
    public void createsNewWebTargetByResolvingUriTemplatesAndEncodingSlashesInPath() {
        UriBuilder resolvedUriBuilder = mockUriBuilder(uri.resolve("va%2Flue"));
        when(clonedUriBuilder.resolveTemplates(ImmutableMap.of("name", "va/lue"), true)).thenReturn(resolvedUriBuilder);
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));

        EverrestWebTarget newWebTarget = (EverrestWebTarget) webTarget.resolveTemplates(ImmutableMap.of("name", "va/lue"), true);

        assertNotSame(webTarget, newWebTarget);
        assertEquals(uri.resolve("va%2Flue"), newWebTarget.getUri());
        assertSame(executorProvider, newWebTarget.getExecutorProvider());
        assertSame(client, newWebTarget.getClient());
        assertEquals(ImmutableMap.of("name", "value"), newWebTarget.getConfiguration().getProperties());
    }

    @Test
    public void createsNewWebTargetByResolvingUriTemplatesFromEncoded() {
        UriBuilder resolvedUriBuilder = mockUriBuilder(uri.resolve("va%20lue"));
        when(clonedUriBuilder.resolveTemplatesFromEncoded(ImmutableMap.of("name", "va%20lue"))).thenReturn(resolvedUriBuilder);
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));

        EverrestWebTarget newWebTarget = (EverrestWebTarget) webTarget.resolveTemplatesFromEncoded(ImmutableMap.of("name", "va%20lue"));

        assertNotSame(webTarget, newWebTarget);
        assertEquals(uri.resolve("va%20lue"), newWebTarget.getUri());
        assertSame(executorProvider, newWebTarget.getExecutorProvider());
        assertSame(client, newWebTarget.getClient());
        assertEquals(ImmutableMap.of("name", "value"), newWebTarget.getConfiguration().getProperties());
    }

    @Test
    public void createsNewWebTargetWithMatrixParameters() {
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));
        UriBuilder updatedUriBuilder = mockUriBuilder(uri.resolve(";x=y;x=z"));
        when(clonedUriBuilder.matrixParam("x", "y", "z")).thenReturn(updatedUriBuilder);

        EverrestWebTarget newWebTarget = (EverrestWebTarget) webTarget.matrixParam("x", "y", "z");

        assertNotSame(webTarget, newWebTarget);
        assertEquals(uri.resolve(";x=y;x=z"), newWebTarget.getUri());
        assertSame(executorProvider, newWebTarget.getExecutorProvider());
        assertSame(client, newWebTarget.getClient());
        assertEquals(ImmutableMap.of("name", "value"), newWebTarget.getConfiguration().getProperties());
    }

    @Test
    public void createsNewWebTargetWithQueryParameters() {
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));
        UriBuilder updatedUriBuilder = mockUriBuilder(uri.resolve("?x=y&x=z"));
        when(clonedUriBuilder.queryParam("x", "y", "z")).thenReturn(updatedUriBuilder);

        EverrestWebTarget newWebTarget = (EverrestWebTarget) webTarget.queryParam("x", "y", "z");

        assertNotSame(webTarget, newWebTarget);
        assertEquals(uri.resolve("?x=y&x=z"), newWebTarget.getUri());
        assertSame(executorProvider, newWebTarget.getExecutorProvider());
        assertSame(client, newWebTarget.getClient());
        assertEquals(ImmutableMap.of("name", "value"), newWebTarget.getConfiguration().getProperties());
    }

    @Test
    public void setsProperty() {
        webTarget.property("name", "value");
        verify(properties).setProperty("name", "value");
    }

    @Test
    public void registersComponentClass() {
        webTarget.register(StringEntityProvider.class);
        verify(providers).register(StringEntityProvider.class);
    }

    @Test
    public void registersComponentClassWithPriority() {
        webTarget.register(StringEntityProvider.class, 99);
        verify(providers).register(StringEntityProvider.class, 99);
    }

    @Test
    public void registersComponentClassWithContracts() {
        webTarget.register(StringEntityProvider.class, MessageBodyReader.class, MessageBodyWriter.class);
        verify(providers).register(StringEntityProvider.class, MessageBodyReader.class, MessageBodyWriter.class);
    }

    @Test
    public void registersComponentClassWithContractAndPriorities() {
        webTarget.register(StringEntityProvider.class, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 2));
        verify(providers).register(StringEntityProvider.class, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 2));
    }

    @Test
    public void registersComponent() {
        StringEntityProvider component = new StringEntityProvider();
        webTarget.register(component);
        verify(providers).register(component);
    }

    @Test
    public void registersComponentWithPriority() {
        StringEntityProvider component = new StringEntityProvider();
        webTarget.register(component, 99);
        verify(providers).register(component, 99);
    }

    @Test
    public void registersComponentWithContracts() {
        StringEntityProvider component = new StringEntityProvider();
        webTarget.register(component, MessageBodyReader.class, MessageBodyWriter.class);
        verify(providers).register(component, MessageBodyReader.class, MessageBodyWriter.class);
    }

    @Test
    public void registersComponentWithContractAndPriorities() {
        StringEntityProvider component = new StringEntityProvider();
        webTarget.register(component, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 2));
        verify(providers).register(component, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 2));
    }

    @Test
    public void createsInvocationBuilder() {
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));
        InvocationBuilder invocationBuilder = (InvocationBuilder) webTarget.request();

        assertNotNull(invocationBuilder);
        assertSame(executorProvider, invocationBuilder.getExecutorProvider());
        ClientRequest request = invocationBuilder.getRequest();
        assertEquals(uri, request.getUri());
        assertSame(client, request.getClient());
        assertSame(providers, request.getProviders());
        assertEquals(ImmutableMap.of("name", "value"), request.getConfiguration().getProperties());
    }

    @Test
    public void createsInvocationBuilderWithAcceptedResponseTypesAsStrings() {
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));
        InvocationBuilder invocationBuilder = (InvocationBuilder) webTarget.request("application/json");

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
    public void createsInvocationBuilderWithAcceptedResponseTypesAsMediaTypes() {
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));
        InvocationBuilder invocationBuilder = (InvocationBuilder) webTarget.request(new MediaType("application", "json"));

        assertNotNull(invocationBuilder);
        assertSame(executorProvider, invocationBuilder.getExecutorProvider());
        ClientRequest request = invocationBuilder.getRequest();
        assertEquals(uri, request.getUri());
        assertEquals(newArrayList(new MediaType("application", "json")), request.getHeaders().get(ACCEPT));
        assertSame(client, request.getClient());
        assertSame(providers, request.getProviders());
        assertEquals(ImmutableMap.of("name", "value"), request.getConfiguration().getProperties());
    }

    @Test
    @UseDataProvider("dataForThrowsIllegalStateExceptionWhenCallMethodOnClosedInstance")
    public void throwsIllegalStateExceptionWhenCallMethodOnClosedInstance(Consumer<EverrestWebTarget> consumer) {
        when(client.isClosed()).thenReturn(true);
        thrown.expect(IllegalStateException.class);
        consumer.accept(webTarget);
    }

    @DataProvider
    public static Object[][] dataForThrowsIllegalStateExceptionWhenCallMethodOnClosedInstance() {
        return new Object[][] {
                {(Consumer<WebTarget>) WebTarget::getUri},
                {(Consumer<WebTarget>) WebTarget::getUriBuilder},
                {(Consumer<WebTarget>) wt -> wt.path("x")},
                {(Consumer<WebTarget>) wt -> wt.resolveTemplate("x", "y")},
                {(Consumer<WebTarget>) wt -> wt.resolveTemplate("x", "y", true)},
                {(Consumer<WebTarget>) wt -> wt.resolveTemplateFromEncoded("x", "y")},
                {(Consumer<WebTarget>) wt -> wt.resolveTemplates(ImmutableMap.of("x", "y"))},
                {(Consumer<WebTarget>) wt -> wt.resolveTemplates(ImmutableMap.of("x", "y"), true)},
                {(Consumer<WebTarget>) wt -> wt.resolveTemplatesFromEncoded(ImmutableMap.of("x", "y"))},
                {(Consumer<WebTarget>) wt -> wt.matrixParam("x", "y")},
                {(Consumer<WebTarget>) wt -> wt.queryParam("x", "y")},
                {(Consumer<WebTarget>) wt -> wt.property("x", "y")},
                {(Consumer<WebTarget>) Configurable::getConfiguration},
                {(Consumer<WebTarget>) wt -> wt.register(StringEntityProvider.class)},
                {(Consumer<WebTarget>) wt -> wt.register(StringEntityProvider.class, 3)},
                {(Consumer<WebTarget>) wt -> wt.register(StringEntityProvider.class, MessageBodyReader.class, MessageBodyWriter.class)},
                {(Consumer<WebTarget>) wt -> wt.register(StringEntityProvider.class, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 2))},
                {(Consumer<WebTarget>) wt -> wt.register(new StringEntityProvider())},
                {(Consumer<WebTarget>) wt -> wt.register(new StringEntityProvider(), 3)},
                {(Consumer<WebTarget>) wt -> wt.register(new StringEntityProvider(), MessageBodyReader.class, MessageBodyWriter.class)},
                {(Consumer<WebTarget>) wt -> wt.register(new StringEntityProvider(), ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 2))},
                {(Consumer<WebTarget>) WebTarget::request},
                {(Consumer<WebTarget>) wt -> wt.request("text/plain")},
                {(Consumer<WebTarget>) wt -> wt.request(new MediaType("text", "plain"))}
        };
    }

    @Test
    @UseDataProvider("dataForThrowsNullPointerExceptionWhenMethodParameterIsNull")
    public void throwsNullPointerExceptionWhenMethodParameterIsNull(Consumer<EverrestWebTarget> consumer) {
        thrown.expect(NullPointerException.class);
        consumer.accept(webTarget);
    }

    @DataProvider
    public static Object[][] dataForThrowsNullPointerExceptionWhenMethodParameterIsNull() {
        return new Object[][] {
                {(Consumer<WebTarget>) wt -> wt.path(null)},
                {(Consumer<WebTarget>) wt -> wt.resolveTemplate("name", null)},
                {(Consumer<WebTarget>) wt -> wt.resolveTemplate(null, "value")},
                {(Consumer<WebTarget>) wt -> wt.resolveTemplate("name", null, true)},
                {(Consumer<WebTarget>) wt -> wt.resolveTemplate(null, "value", true)},
                {(Consumer<WebTarget>) wt -> wt.resolveTemplateFromEncoded("name", null)},
                {(Consumer<WebTarget>) wt -> wt.resolveTemplateFromEncoded(null, "value")},
                {(Consumer<WebTarget>) wt -> wt.resolveTemplates(null)},
                {(Consumer<WebTarget>) wt -> {
                    Map<String, Object> templates = new HashMap<>();
                    templates.put("name1", "value1");
                    templates.put("name2", null);
                    wt.resolveTemplates(templates);
                }},
                {(Consumer<WebTarget>) wt -> {
                    Map<String, Object> templates = new HashMap<>();
                    templates.put("name1", "value1");
                    templates.put(null, "value2");
                    wt.resolveTemplates(templates);
                }},
                {(Consumer<WebTarget>) wt -> wt.resolveTemplates(null, true)},
                {(Consumer<WebTarget>) wt -> {
                    Map<String, Object> templates = new HashMap<>();
                    templates.put("name1", "value1");
                    templates.put("name2", null);
                    wt.resolveTemplates(templates, true);
                }},
                {(Consumer<WebTarget>) wt -> {
                    Map<String, Object> templates = new HashMap<>();
                    templates.put("name1", "value1");
                    templates.put(null, "value2");
                    wt.resolveTemplates(templates, true);
                }},
                {(Consumer<WebTarget>) wt -> wt.resolveTemplatesFromEncoded(null)},
                {(Consumer<WebTarget>) wt -> {
                    Map<String, Object> templates = new HashMap<>();
                    templates.put("name1", "value%201");
                    templates.put("name2", null);
                    wt.resolveTemplatesFromEncoded(templates);
                }},
                {(Consumer<WebTarget>) wt -> {
                    Map<String, Object> templates = new HashMap<>();
                    templates.put("name1", "value%201");
                    templates.put(null, "value%202");
                    wt.resolveTemplatesFromEncoded(templates);
                }},
                {(Consumer<WebTarget>) wt -> wt.matrixParam("name", "value", null)},
                {(Consumer<WebTarget>) wt -> wt.matrixParam(null, "value")},
                {(Consumer<WebTarget>) wt -> wt.queryParam("name", "value", null)},
                {(Consumer<WebTarget>) wt -> wt.queryParam(null, "value")}
        };
    }
}