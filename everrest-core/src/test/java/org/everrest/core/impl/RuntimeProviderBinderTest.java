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
package org.everrest.core.impl;

import com.google.common.collect.ImmutableMap;
import org.everrest.core.ConfigurationProperties;
import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.provider.ServerEmbeddedProvidersFeature;
import org.everrest.core.impl.provider.StringEntityProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Feature;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;
import java.lang.annotation.Annotation;
import java.util.HashSet;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static javax.ws.rs.RuntimeType.SERVER;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RuntimeProviderBinderTest {
    @Mock private ProviderBinder originalProviders;
    @Mock private ConfigurationProperties configuration;
    @InjectMocks private RuntimeProviderBinder runtimeProviders;

    @Before
    public void setUp() throws Exception {
        mockOriginalProviderToBeingCopiedInRuntime();
    }

    private void mockOriginalProviderToBeingCopiedInRuntime() {
        when(originalProviders.getRuntimeType()).thenReturn(SERVER);
        when(originalProviders.getClasses()).thenReturn(newHashSet(StringEntityProvider.class));
        when(originalProviders.getContracts(StringEntityProvider.class))
                .thenReturn(ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 1));
    }

    @Test
    public void getsRuntimeType() {
        assertEquals(SERVER, runtimeProviders.getRuntimeType());
    }

    @Test
    public void registersComponentClassDoesNotUpdateOriginalProviderBinder() {
        runtimeProviders.register(StringEntityProvider.class);
        assertTrue(runtimeProviders.isRegistered(StringEntityProvider.class));
        verify(originalProviders, never()).register(StringEntityProvider.class);
    }

    @Test
    public void registersComponentClassWithPriorityDoesNotUpdateOriginalProviderBinder() {
        runtimeProviders.register(StringEntityProvider.class, 1);
        assertTrue(runtimeProviders.isRegistered(StringEntityProvider.class));
        verify(originalProviders, never()).register(StringEntityProvider.class, 1);
    }

    @Test
    public void registersComponentClassWithContractsDoesNotUpdateOriginalProviderBinder() {
        runtimeProviders.register(StringEntityProvider.class, MessageBodyReader.class, MessageBodyWriter.class);
        assertTrue(runtimeProviders.isRegistered(StringEntityProvider.class));
        verify(originalProviders, never()).register(StringEntityProvider.class, MessageBodyReader.class, MessageBodyWriter.class);
    }

    @Test
    public void registersComponentClassWithPriorityAndContractsDoesNotUpdateOriginalProviderBinder() {
        runtimeProviders.register(StringEntityProvider.class, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 1));
        assertTrue(runtimeProviders.isRegistered(StringEntityProvider.class));
        verify(originalProviders, never()).register(StringEntityProvider.class, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 1));
    }

    @Test
    public void registersComponentDoesNotUpdateOriginalProviderBinder() {
        Object provider = new StringEntityProvider();
        when(originalProviders.getInstances()).thenReturn(newHashSet(provider));
        runtimeProviders.register(provider);
        assertTrue(runtimeProviders.isRegistered(provider));
        verify(originalProviders, never()).register(provider);
    }

    @Test
    public void registersComponentWithPriorityDoesNotUpdateOriginalProviderBinder() {
        Object provider = new StringEntityProvider();
        when(originalProviders.getInstances()).thenReturn(newHashSet(provider));
        runtimeProviders.register(provider, 1);
        assertTrue(runtimeProviders.isRegistered(provider));
        verify(originalProviders, never()).register(provider, 1);
    }

    @Test
    public void registersComponentWithContractsDoesNotUpdateOriginalProviderBinder() {
        Object provider = new StringEntityProvider();
        when(originalProviders.getInstances()).thenReturn(newHashSet(provider));

        runtimeProviders.register(provider, MessageBodyReader.class, MessageBodyWriter.class);
        assertTrue(runtimeProviders.isRegistered(provider));
        verify(originalProviders, never()).register(provider, MessageBodyReader.class, MessageBodyWriter.class);
    }

    @Test
    public void registersComponentWithPriorityAndContractsDoesNotUpdateOriginalProviderBinder() {
        Object provider = new StringEntityProvider();
        when(originalProviders.getInstances()).thenReturn(newHashSet(provider));
        runtimeProviders.register(provider, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 1));
        assertTrue(runtimeProviders.isRegistered(provider));
        verify(originalProviders, never()).register(provider, ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 1));
    }

    @Test
    public void checksComponentClassRegistered() {
        when(originalProviders.isRegistered(StringEntityProvider.class)).thenReturn(true);
        assertTrue(runtimeProviders.isRegistered(StringEntityProvider.class));
    }

    @Test
    public void checksComponentRegistered() {
        Object provider = new StringEntityProvider();
        when(originalProviders.isRegistered(provider)).thenReturn(true);
        assertTrue(runtimeProviders.isRegistered(provider));
    }

    @Test
    public void getsComponentContracts() {
        ImmutableMap<Class<?>, Integer> contracts = ImmutableMap.of(MessageBodyReader.class, 1, MessageBodyWriter.class, 1);
        when(originalProviders.getContracts(StringEntityProvider.class)).thenReturn(contracts);
        assertEquals(contracts, runtimeProviders.getContracts(StringEntityProvider.class));
    }

    @Test
    public void getsInstances() {
        Object provider = new StringEntityProvider();
        HashSet<Object> instances = newHashSet(provider);
        when(originalProviders.getInstances()).thenReturn(instances);
        assertEquals(instances, runtimeProviders.getInstances());
    }

    @Test
    public void getsClasses() {
        HashSet<Class<?>> classes = newHashSet(StringEntityProvider.class);
        when(originalProviders.getClasses()).thenReturn(classes);
        assertEquals(classes, runtimeProviders.getClasses());
    }

    @Test
    public void checksFeatureClassEnabled() {
        when(originalProviders.isEnabled(ServerEmbeddedProvidersFeature.class)).thenReturn(true);
        assertTrue(runtimeProviders.isEnabled(ServerEmbeddedProvidersFeature.class));
    }

    @Test
    public void checksFeatureEnabled() {
        Feature feature = new ServerEmbeddedProvidersFeature();
        when(originalProviders.isEnabled(feature)).thenReturn(true);
        assertTrue(runtimeProviders.isEnabled(feature));
    }

    @Test
    public void getsAcceptableWriterMediaTypes() {
        when(originalProviders.getAcceptableWriterMediaTypes(String.class, String.class, new Annotation[0])).thenReturn(newArrayList(TEXT_PLAIN_TYPE));
        assertEquals(newArrayList(TEXT_PLAIN_TYPE), runtimeProviders.getAcceptableWriterMediaTypes(String.class, String.class, new Annotation[0]));
    }

    @Test
    public void getsReaderInterceptors() {
        ReaderInterceptor interceptor = mock(ReaderInterceptor.class);
        when(originalProviders.getReaderInterceptors()).thenReturn(newArrayList(interceptor));
        assertEquals(newArrayList(interceptor), runtimeProviders.getReaderInterceptors());
    }

    @Test
    public void getsWriterInterceptors() {
        WriterInterceptor interceptor = mock(WriterInterceptor.class);
        when(originalProviders.getWriterInterceptors()).thenReturn(newArrayList(interceptor));
        assertEquals(newArrayList(interceptor), runtimeProviders.getWriterInterceptors());
    }

    @Test
    public void getsClientRequestFilters() {
        ClientRequestFilter filter = mock(ClientRequestFilter.class);
        when(originalProviders.getClientRequestFilters()).thenReturn(newArrayList(filter));
        assertEquals(newArrayList(filter), runtimeProviders.getClientRequestFilters());
    }

    @Test
    public void getsClientResponseFilters() {
        ClientResponseFilter filter = mock(ClientResponseFilter.class);
        when(originalProviders.getClientResponseFilters()).thenReturn(newArrayList(filter));
        assertEquals(newArrayList(filter), runtimeProviders.getClientResponseFilters());
    }

    @Test
    public void getsContainerRequestFilters() {
        ContainerRequestFilter filter = mock(ContainerRequestFilter.class);
        when(originalProviders.getContainerRequestFilters(new Annotation[0], true)).thenReturn(newArrayList(filter));
        assertEquals(newArrayList(filter), runtimeProviders.getContainerRequestFilters(new Annotation[0], true));
    }

    @Test
    public void getsContainerResponseFilters() {
        ContainerResponseFilter filter = mock(ContainerResponseFilter.class);
        when(originalProviders.getContainerResponseFilters(new Annotation[0])).thenReturn(newArrayList(filter));
        assertEquals(newArrayList(filter), runtimeProviders.getContainerResponseFilters(new Annotation[0]));
    }

    @Test
    public void getsDynamicFeatures() {
        DynamicFeature feature = mock(DynamicFeature.class);
        when(originalProviders.getDynamicFeatures()).thenReturn(newArrayList(feature));
        assertEquals(newArrayList(feature), runtimeProviders.getDynamicFeatures());
    }

    @Test
    public void clearsProvidersDoesNotUpdateOriginalProviderBinder() {
        runtimeProviders.clear();
        assertFalse(runtimeProviders.isRegistered(StringEntityProvider.class));
        verify(originalProviders, never()).clear();
    }

    @Test
    public void getsMessageBodyReader() {
        MessageBodyReader reader = mock(MessageBodyReader.class);
        when(originalProviders.getMessageBodyReader(String.class, String.class, new Annotation[0], TEXT_PLAIN_TYPE)).thenReturn(reader);
        assertEquals(reader, runtimeProviders.getMessageBodyReader(String.class, String.class, new Annotation[0], TEXT_PLAIN_TYPE));
    }

    @Test
    public void getsMessageBodyWriter() {
        MessageBodyWriter writer = mock(MessageBodyWriter.class);
        when(originalProviders.getMessageBodyWriter(String.class, String.class, new Annotation[0], TEXT_PLAIN_TYPE)).thenReturn(writer);
        assertEquals(writer, runtimeProviders.getMessageBodyWriter(String.class, String.class, new Annotation[0], TEXT_PLAIN_TYPE));
    }

    @Test
    public void getsExceptionMapper() {
        ExceptionMapper mapper = mock(ExceptionMapper.class);
        when(originalProviders.getExceptionMapper(RuntimeException.class)).thenReturn(mapper);
        assertEquals(mapper, runtimeProviders.getExceptionMapper(RuntimeException.class));
    }

    @Test
    public void getsContextResolver() {
        ContextResolver resolver = mock(ContextResolver.class);
        when(originalProviders.getContextResolver(String.class, TEXT_PLAIN_TYPE)).thenReturn(resolver);
        assertEquals(resolver, runtimeProviders.getContextResolver(String.class, TEXT_PLAIN_TYPE));
    }

    @Test
    public void getsConverter() {
        ParamConverter converter = mock(ParamConverter.class);
        when(originalProviders.getConverter(String.class, String.class, new Annotation[0])).thenReturn(converter);
        assertEquals(converter, runtimeProviders.getConverter(String.class, String.class, new Annotation[0]));
    }

}