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

import org.everrest.core.DependencySupplier;
import org.everrest.core.ProviderBinder;
import org.everrest.core.RequestHandler;
import org.everrest.core.ResourceBinder;
import org.everrest.core.impl.method.MethodInvokerDecorator;
import org.everrest.core.impl.method.MethodInvokerDecoratorFactory;
import org.everrest.core.method.MethodInvoker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.annotation.PreDestroy;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Sets.newHashSet;
import static javax.ws.rs.RuntimeType.SERVER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EverrestProcessorTest {
    private String                         httpMethod     = "POST";
    private URI                            requestUri     = URI.create("http://localhost:8080/servlet/a/b/c");
    private URI                            baseUri        = URI.create("http://localhost:8080/servlet");
    private MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();

    private RequestHandler     requestHandler;
    private DependencySupplier dependencySupplier;
    private EverrestProcessor  everrestProcessor;

    @Before
    public void setUp() throws Exception {
        requestHandler = mock(RequestHandler.class);
        ProviderBinder providers = mock(ProviderBinder.class);
        when(providers.getRuntimeType()).thenReturn(SERVER);
        ResourceBinder resources = mock(ResourceBinder.class);
        dependencySupplier = mock(DependencySupplier.class);
        ServerConfigurationProperties configuration = mock(ServerConfigurationProperties.class, RETURNS_DEEP_STUBS);
        when(configuration.getStringProperty("org.everrest.core.impl.method.MethodInvokerDecoratorFactory", null))
                .thenReturn(FakeMethodInvokerDecoratorFactory.class.getName());

        everrestProcessor = new EverrestProcessor(configuration, dependencySupplier, requestHandler, resources, providers, null);
    }

    @After
    public void tearDown() throws Exception {
        requestHeaders.clear();
    }

    @Test
    public void initializeMethodInvokerDecoratorFactoryIfConfigured() {
        assertTrue(everrestProcessor.getMethodInvokerDecoratorFactory() instanceof FakeMethodInvokerDecoratorFactory);
    }

    @Test
    public void processesRequest() throws Exception {
        ContainerRequest containerRequest = mockContainerRequest();
        ContainerResponse containerResponse = mockContainerResponse();
        EnvironmentContext environmentContext = mock(EnvironmentContext.class);

        doAnswer(invocation -> {
            Response response = mock(Response.class);
            when(response.getStatus()).thenReturn(200);
            when(response.getEntity()).thenReturn("foo");
            containerResponse.setResponse(response);
            return null;
        }).when(requestHandler).handleRequest(containerRequest, containerResponse);

        everrestProcessor.process(containerRequest, containerResponse, environmentContext);

        ArgumentCaptor<Response> argumentCaptor = ArgumentCaptor.forClass(Response.class);
        verify(containerResponse).setResponse(argumentCaptor.capture());
        assertEquals(200, argumentCaptor.getValue().getStatus());
        assertEquals("foo", argumentCaptor.getValue().getEntity());
    }

    @Test
    public void setsUpApplicationContextBeforeCallToRequestHandler() throws Exception {
        ContainerRequest containerRequest = mockContainerRequest();
        ContainerResponse containerResponse = mockContainerResponse();
        EnvironmentContext environmentContext = mock(EnvironmentContext.class);

        ApplicationContext[] applicationContextThatRequestHandlerCalledWith = new ApplicationContext[1];
        doAnswer(invocation -> {
            applicationContextThatRequestHandlerCalledWith[0] = ApplicationContext.getCurrent();
            return null;
        }).when(requestHandler).handleRequest(containerRequest, containerResponse);

        everrestProcessor.process(containerRequest, containerResponse, environmentContext);
        assertNotNull(applicationContextThatRequestHandlerCalledWith[0]);
        assertSame(containerRequest, applicationContextThatRequestHandlerCalledWith[0].getContainerRequest());
        assertSame(containerResponse, applicationContextThatRequestHandlerCalledWith[0].getContainerResponse());
        assertSame(dependencySupplier, applicationContextThatRequestHandlerCalledWith[0].getDependencySupplier());
        assertSame(environmentContext, applicationContextThatRequestHandlerCalledWith[0].getEnvironmentContext());
    }

    @Test
    public void resetsApplicationContextAlterCallToRequestHandler() throws Exception {
        ContainerRequest containerRequest = mockContainerRequest();
        ContainerResponse containerResponse = mockContainerResponse();
        EnvironmentContext environmentContext = mock(EnvironmentContext.class);

        everrestProcessor.process(containerRequest, containerResponse, environmentContext);

        ApplicationContext applicationContextAfterCall = ApplicationContext.getCurrent();
        assertNull(applicationContextAfterCall);
    }

    @Test
    public void callsPreDestroyMethodsForSingletonComponentsOnStop() throws Exception {
        EchoResource resource = new EchoResource();
        Application application = mock(Application.class);
        when(application.getSingletons()).thenReturn(newHashSet(resource));

        everrestProcessor.addApplication(application);
        everrestProcessor.stop();

        assertEquals(1, resource.preDestroyActionInvocationsCounter.get());
    }

    private ContainerRequest mockContainerRequest() {
        ContainerRequest containerRequest = mock(ContainerRequest.class);
        when(containerRequest.getBaseUri()).thenReturn(baseUri);
        when(containerRequest.getRequestUri()).thenReturn(requestUri);
        when(containerRequest.getMethod()).thenReturn(httpMethod);
        when(containerRequest.getRequestHeaders()).thenReturn(requestHeaders);
        return containerRequest;
    }

    private ContainerResponse mockContainerResponse() {
        ContainerResponse containerResponse = mock(ContainerResponse.class);
        when(containerResponse.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        return containerResponse;
    }

    public static class FakeMethodInvokerDecoratorFactory implements MethodInvokerDecoratorFactory {
        @Override
        public MethodInvokerDecorator makeDecorator(MethodInvoker invoker) {
            return mock(MethodInvokerDecorator.class);
        }
    }

    public static class EchoResource {
        public String echo(String phrase) {
            return phrase;
        }

        private AtomicInteger preDestroyActionInvocationsCounter = new AtomicInteger();

        @PreDestroy
        private void preDestroyAction() {
            preDestroyActionInvocationsCounter.incrementAndGet();
        }
    }
}