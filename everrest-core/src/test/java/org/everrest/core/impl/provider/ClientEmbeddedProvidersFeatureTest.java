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

import org.everrest.core.ConfigurationProperties;
import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.ServerConfigurationProperties;
import org.everrest.core.impl.provider.multipart.CollectionMultipartFormDataMessageBodyWriter;
import org.everrest.core.impl.provider.multipart.ListMultipartFormDataMessageBodyReader;
import org.everrest.core.impl.provider.multipart.MapMultipartFormDataMessageBodyReader;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.FeatureContext;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientEmbeddedProvidersFeatureTest {
    private FeatureContext featureContext;

    private ClientEmbeddedProvidersFeature embeddedProvidersFeature;

    @Before
    public void setUp() {
        ConfigurationProperties clientProperties = mock(ServerConfigurationProperties.class);
        EverrestConfiguration everrestConfiguration = mock(EverrestConfiguration.class);
        featureContext = mock(FeatureContext.class);

        when(everrestConfiguration.getConfigurationProperties()).thenReturn(clientProperties);
        when(featureContext.getConfiguration()).thenReturn(everrestConfiguration);

        embeddedProvidersFeature = new ClientEmbeddedProvidersFeature();
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
}