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
import org.everrest.core.ProviderBinder;
import org.everrest.core.ResourceBinder;
import org.everrest.core.impl.provider.StringEntityProvider;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApplicationPublisherTest {
    private ResourceBinder resources;
    private ProviderBinder providers;

    private ApplicationPublisher publisher;

    @Before
    public void setUp() throws Exception {
        resources = mock(ResourceBinder.class);
        providers = mock(DefaultProviderBinder.class);

        publisher = new ApplicationPublisher(resources, providers);
    }

    @Test
    public void publishesPerRequestResource() {
        Application application = mock(Application.class);
        when(application.getClasses()).thenReturn(newHashSet(Resource.class));

        publisher.publish(application);

        verify(resources).addResource(Resource.class, null);
    }

    @Test
    public void publishesSingletonResource() {
        Resource resource = new Resource();
        Application application = mock(Application.class);
        when(application.getSingletons()).thenReturn(newHashSet(resource));

        publisher.publish(application);

        verify(resources).addResource(resource, null);
    }

    @Path("a")
    public static class Resource {
    }

    @Test
    public void publishesPerRequestExceptionMapper() {
        Application application = mock(Application.class);
        when(application.getClasses()).thenReturn(newHashSet(RuntimeExceptionMapper.class));

        publisher.publish(application);

        verify(providers).register(RuntimeExceptionMapper.class);
    }

    @Test
    public void publishesSingletonExceptionMapper() {
        ExceptionMapper exceptionMapper = new RuntimeExceptionMapper();
        Application application = mock(Application.class);
        when(application.getSingletons()).thenReturn(newHashSet(exceptionMapper));

        publisher.publish(application);

        verify(providers).register(exceptionMapper);
    }

    @Provider
    public static class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {
        @Override
        public Response toResponse(RuntimeException exception) {
            return null;
        }
    }

    @Test
    public void publishesPerRequestContextResolver() {
        Application application = mock(Application.class);
        when(application.getClasses()).thenReturn(newHashSet(ContextResolverText.class));

        publisher.publish(application);

        verify(providers).register(ContextResolverText.class);
    }

    @Test
    public void publishesSingletonContextResolver() {
        ContextResolver contextResolver = new ContextResolverText();
        Application application = mock(Application.class);
        when(application.getSingletons()).thenReturn(newHashSet(contextResolver));

        publisher.publish(application);

        verify(providers).register(contextResolver);
    }

    @Provider
    @Produces("text/plain")
    public static class ContextResolverText implements ContextResolver<String> {
        public String getContext(Class<?> type) {
            return null;
        }
    }

    @Test
    public void publishesPerRequestMessageBodyReader() {
        Application application = mock(Application.class);
        when(application.getClasses()).thenReturn(newHashSet(StringEntityProvider.class));

        publisher.publish(application);

        verify(providers).register(StringEntityProvider.class);
    }

    @Test
    public void publishesSingletonMessageBodyReader() {
        MessageBodyReader<String> messageBodyReader = new StringEntityProvider();
        Application application = mock(Application.class);
        when(application.getSingletons()).thenReturn(newHashSet(messageBodyReader));

        publisher.publish(application);

        verify(providers).register(messageBodyReader);
    }

    @Test
    public void publishesPerRequestMessageBodyWriter() {
        Application application = mock(Application.class);
        when(application.getClasses()).thenReturn(newHashSet(StringEntityProvider.class));

        publisher.publish(application);

        verify(providers).register(StringEntityProvider.class);
    }

    @Test
    public void publishesSingletonMessageBodyWriter() {
        MessageBodyWriter<String> messageBodyWriter = new StringEntityProvider();
        Application application = mock(Application.class);
        when(application.getSingletons()).thenReturn(newHashSet(messageBodyWriter));

        publisher.publish(application);

        verify(providers).register(messageBodyWriter);
    }

    @Test
    public void publishesPerRequestResourceWithSpecifiedPathThroughEverrestApplication() {
        EverrestApplication application = mock(EverrestApplication.class);
        when(application.getClasses()).thenReturn(newHashSet(Resource.class));
        when(application.getResourceClasses()).thenReturn(ImmutableMap.of("/x", Resource.class));

        publisher.publish(application);

        verify(resources).addResource(Resource.class, null);
        verify(resources).addResource("/x", Resource.class, null);
    }

    @Test
    public void publishesSingletonResourceWithSpecifiedPathThroughEverrestApplication() {
        Resource resource = new Resource();
        EverrestApplication application = mock(EverrestApplication.class);
        when(application.getSingletons()).thenReturn(newHashSet(resource));
        when(application.getResourceSingletons()).thenReturn(ImmutableMap.of("/x", resource));

        publisher.publish(application);

        verify(resources).addResource(resource, null);
        verify(resources).addResource("/x", resource, null);
    }
}