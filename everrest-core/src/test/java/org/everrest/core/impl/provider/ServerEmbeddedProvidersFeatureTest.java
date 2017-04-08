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
package org.everrest.core.impl.provider;

import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.ServerConfigurationProperties;
import org.everrest.core.impl.async.AsynchronousProcessListWriter;
import org.everrest.core.impl.async.DefaultAsynchronousJobPool;
import org.everrest.core.impl.method.filter.SecurityConstraintDynamicFeature;
import org.everrest.core.impl.method.filter.UriNormalizeFilter;
import org.everrest.core.impl.method.filter.XHTTPMethodOverrideFilter;
import org.everrest.core.impl.provider.multipart.CollectionMultipartFormDataMessageBodyWriter;
import org.everrest.core.impl.provider.multipart.ListMultipartFormDataMessageBodyReader;
import org.everrest.core.impl.provider.multipart.MapMultipartFormDataMessageBodyReader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.core.FeatureContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServerEmbeddedProvidersFeatureTest {
    private EverrestConfiguration everrestConfiguration;
    private ServerConfigurationProperties serverProperties;
    private FeatureContext featureContext;

    private ServerEmbeddedProvidersFeature embeddedProvidersFeature;

    @Before
    public void setUp() {
        serverProperties = mock(ServerConfigurationProperties.class);
        everrestConfiguration = mock(EverrestConfiguration.class);
        featureContext = mock(FeatureContext.class);

        when(everrestConfiguration.getConfigurationProperties()).thenReturn(serverProperties);
        when(featureContext.getConfiguration()).thenReturn(everrestConfiguration);

        embeddedProvidersFeature = new ServerEmbeddedProvidersFeature();
    }

    @Test
    public void registersByteEntityProvider() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(isA(ByteEntityProvider.class));
    }

    @Test
    public void registersDataSourceEntityProvider() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(isA(DataSourceEntityProvider.class));
    }

    @Test
    public void registersDOMSourceEntityProvider() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(isA(DOMSourceEntityProvider.class));
    }

    @Test
    public void registersFileEntityProvider() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(isA(FileEntityProvider.class));
    }

    @Test
    public void registersMultivaluedMapEntityProvider() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(MultivaluedMapEntityProvider.class);
    }

    @Test
    public void registersInputStreamEntityProvider() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(isA(InputStreamEntityProvider.class));
    }

    @Test
    public void registersReaderEntityProvider() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(isA(ReaderEntityProvider.class));
    }

    @Test
    public void registersSAXSourceEntityProvider() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(isA(SAXSourceEntityProvider.class));
    }

    @Test
    public void registersStreamSourceEntityProvider() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(isA(StreamSourceEntityProvider.class));
    }

    @Test
    public void registersStringEntityProvider() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(isA(StringEntityProvider.class));
    }

    @Test
    public void registersStreamOutputEntityProvider() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(isA(StreamOutputEntityProvider.class));
    }

    @Test
    public void registersJsonEntityProvider() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(isA(JsonEntityProvider.class));
    }

    @Test
    public void registersJAXBElementEntityProvider() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(JAXBElementEntityProvider.class);
    }

    @Test
    public void registersJAXBObjectEntityProvider() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(JAXBObjectEntityProvider.class);
    }

    @Test
    public void registersMultipartFormDataEntityProvider() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(MultipartFormDataEntityProvider.class);
    }

    @Test
    public void registersListMultipartFormDataMessageBodyReader() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(ListMultipartFormDataMessageBodyReader.class);
    }

    @Test
    public void registersMapMultipartFormDataMessageBodyReader() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(MapMultipartFormDataMessageBodyReader.class);
    }

    @Test
    public void registersCollectionMultipartFormDataMessageBodyWriter() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(CollectionMultipartFormDataMessageBodyWriter.class);
    }

    @Test
    public void registersJAXBContextResolver() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(isA(JAXBContextResolver.class));
    }

    @Test
    public void registersDefaultExceptionMapper() {
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(isA(DefaultExceptionMapper.class));
    }

    @Test
    public void registersXHTTPMethodOverrideFilterIfFeatureEnabledInConfiguration() {
        when(serverProperties.isHttpMethodOverrideEnabled()).thenReturn(true);
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(isA(XHTTPMethodOverrideFilter.class));
    }

    @Test
    public void doesNotRegisterXHTTPMethodOverrideFilterIfFeatureDisabledInConfiguration() {
        when(serverProperties.isHttpMethodOverrideEnabled()).thenReturn(false);
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext, never()).register(isA(XHTTPMethodOverrideFilter.class));
    }

    @Test
    public void registersUriNormalizeFilterIfFeatureEnabledInConfiguration() {
        when(serverProperties.isUriNormalizationEnabled()).thenReturn(true);
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(isA(UriNormalizeFilter.class));
    }

    @Test
    public void doesNotRegisterUriNormalizeFilterIfFeatureDisabledInConfiguration() {
        when(serverProperties.isUriNormalizationEnabled()).thenReturn(false);
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext, never()).register(isA(UriNormalizeFilter.class));
    }

    @Test
    public void registersSecurityConstraintFeatureIfFeatureEnabledInConfiguration() {
        when(serverProperties.isCheckSecurityEnabled()).thenReturn(true);
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext).register(isA(SecurityConstraintDynamicFeature.class));
    }

    @Test
    public void doesNotRegisterSecurityConstraintFeatureIfFeatureDisabledInConfiguration() {
        when(serverProperties.isCheckSecurityEnabled()).thenReturn(false);
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext, never()).register(isA(SecurityConstraintDynamicFeature.class));
    }

    @Test
    public void registersAsynchronousComponentsIfFeatureEnabledInConfiguration() {
        when(serverProperties.isAsynchronousEnabled()).thenReturn(true);
        when(serverProperties.getAsynchronousCacheSize()).thenReturn(10);
        when(serverProperties.getAsynchronousJobTimeout()).thenReturn(100);
        when(serverProperties.getAsynchronousQueueSize()).thenReturn(512);
        when(serverProperties.getAsynchronousPoolSize()).thenReturn(60);

        embeddedProvidersFeature.configure(featureContext);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(featureContext, atLeastOnce()).register(captor.capture());
        verify(featureContext).register(isA(AsynchronousProcessListWriter.class));

        DefaultAsynchronousJobPool asynchronousPool = (DefaultAsynchronousJobPool) captor.getAllValues().stream()
                .filter(component -> component instanceof DefaultAsynchronousJobPool)
                .findFirst()
                .orElse(null);

        assertEquals(60, asynchronousPool.getThreadPoolSize());
        assertEquals(512, asynchronousPool.getMaxQueueSize());
        assertEquals(10, asynchronousPool.getMaxCacheSize());
        assertEquals(100, asynchronousPool.getJobTimeout());
    }

    @Test
    public void doesNotRegisterAsynchronousComponentsIfFeatureDisabledInConfiguration() {
        when(serverProperties.isAsynchronousEnabled()).thenReturn(false);
        embeddedProvidersFeature.configure(featureContext);
        verify(featureContext, never()).register(isA(DefaultAsynchronousJobPool.class));
        verify(featureContext, never()).register(isA(AsynchronousProcessListWriter.class));
    }
}