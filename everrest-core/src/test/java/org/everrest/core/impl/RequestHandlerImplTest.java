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

import org.everrest.core.$matchers.ExceptionMatchers;
import org.everrest.core.ProviderBinder;
import org.everrest.core.UnhandledException;
import org.everrest.core.tools.ErrorPages;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.serverError;
import static org.everrest.core.$matchers.ExceptionMatchers.exceptionSameInstance;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RequestHandlerImplTest {
    @Rule public ExpectedException thrown = ExpectedException.none();

    private final String requestPath = "/a/b/c";

    private ContainerRequest   request;
    private ContainerResponse  response;
    private RequestDispatcher  requestDispatcher;
    private ProviderBinder     providers;
    private EnvironmentContext environmentContext;

    private RequestHandlerImpl requestHandler;

    @Before
    public void setUp() throws Exception {
        environmentContext = mock(EnvironmentContext.class);

        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getPath()).thenReturn(requestPath);
        when(applicationContext.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(applicationContext.getEnvironmentContext()).thenReturn(environmentContext);
        ApplicationContext.setCurrent(applicationContext);

        request = mock(ContainerRequest.class);

        response = mock(ContainerResponse.class);
        when(response.getHeaders()).thenReturn(new MultivaluedHashMap<>());

        requestDispatcher = mock(RequestDispatcher.class);

        providers = mock(DefaultProviderBinder.class);

        requestHandler = new RequestHandlerImpl(requestDispatcher, providers);
    }

    @After
    public void tearDown() throws Exception {
        ApplicationContext.setCurrent(null);
    }

    @Test
    public void executesRequestWithRequestDispatcher() throws Exception {
        requestHandler.handleRequest(request, response);

        verify(requestDispatcher).dispatch(request, response);
    }

    @Test
    public void wrapsUnexpectedExceptionWithUnhandledException() throws Exception {
        RuntimeException exception = new RuntimeException();
        doThrow(exception).when(requestDispatcher).dispatch(request, response);

        thrown.expect(UnhandledException.class);
        thrown.expectCause(exceptionSameInstance(exception));
        requestHandler.handleRequest(request, response);
    }

    @Test
    public void sendsResponseFromThrownWebApplicationExceptionWhenResponseHasEntity() throws Exception {
        WebApplicationException exception = new WebApplicationException(serverError().entity("Some Error").build());
        doThrow(exception).when(requestDispatcher).dispatch(request, response);

        requestHandler.handleRequest(request, response);

        ArgumentCaptor<Response> argumentCaptor = ArgumentCaptor.forClass(Response.class);
        verify(response).setResponse(argumentCaptor.capture());
        assertEquals(INTERNAL_SERVER_ERROR, argumentCaptor.getValue().getStatusInfo());
        assertEquals("Some Error", argumentCaptor.getValue().getEntity());
    }

    @Test
    public void throwsUnhandledExceptionWithCauseOfWebApplicationExceptionWhenJspErrorPageIsAvailableForErrorStatus() throws Exception {
        ErrorPages errorPages = mock(ErrorPages.class);
        when(errorPages.hasErrorPage(403)).thenReturn(true);
        when(environmentContext.get(ErrorPages.class)).thenReturn(errorPages);

        WebApplicationException webApplicationException = new WebApplicationException(403);
        doThrow(webApplicationException).when(requestDispatcher).dispatch(request, response);

        thrown.expect(UnhandledException.class);
        thrown.expect(ExceptionMatchers.unhandledExceptionWithStatus(403));
        requestHandler.handleRequest(request, response);
    }

    @Test
    public void throwsUnhandledExceptionWithCauseOfWebApplicationExceptionWhenJspErrorPageForCauseIsAvailable() throws Exception {
        ErrorPages errorPages = mock(ErrorPages.class);
        Exception exception = new Exception();
        when(errorPages.hasErrorPage(exception)).thenReturn(true);
        when(environmentContext.get(ErrorPages.class)).thenReturn(errorPages);

        WebApplicationException webApplicationException = new WebApplicationException(exception);
        doThrow(webApplicationException).when(requestDispatcher).dispatch(request, response);

        thrown.expect(UnhandledException.class);
        thrown.expectCause(exceptionSameInstance(exception));
        requestHandler.handleRequest(request, response);
    }

    @Test
    public void throwsUnhandledExceptionWithWebApplicationExceptionWhenJspErrorPageForWebApplicationExceptionIsAvailable() throws Exception {
        ErrorPages errorPages = mock(ErrorPages.class);
        WebApplicationException webApplicationException = new WebApplicationException();
        when(errorPages.hasErrorPage(webApplicationException)).thenReturn(true);
        when(environmentContext.get(ErrorPages.class)).thenReturn(errorPages);

        doThrow(webApplicationException).when(requestDispatcher).dispatch(request, response);

        thrown.expect(UnhandledException.class);
        thrown.expectCause(exceptionSameInstance(webApplicationException));
        requestHandler.handleRequest(request, response);
    }

    @Test
    public void usesExceptionMapperToConvertWebApplicationExceptionToResponseWhenResponseFromWebApplicationExceptionDoesNotHaveEntity() throws Exception {
        WebApplicationException webApplicationException = new WebApplicationException(serverError().build());
        doThrow(webApplicationException).when(requestDispatcher).dispatch(request, response);
        ExceptionMapper<WebApplicationException> exceptionMapper = mock(ExceptionMapper.class);
        when(exceptionMapper.toResponse(webApplicationException)).thenReturn(serverError().entity("response from exception mapper").build());
        when(providers.getExceptionMapper(WebApplicationException.class)).thenReturn(exceptionMapper);

        requestHandler.handleRequest(request, response);

        ArgumentCaptor<Response> argumentCaptor = ArgumentCaptor.forClass(Response.class);
        verify(response).setResponse(argumentCaptor.capture());
        assertEquals(INTERNAL_SERVER_ERROR, argumentCaptor.getValue().getStatusInfo());
        assertEquals("response from exception mapper", argumentCaptor.getValue().getEntity());
    }

    @Test
    public void usesMessageFromCauseOfWebApplicationExceptionForResponseEntityWhenResponseFromWebApplicationExceptionDoesNotHaveEntity() throws Exception {
        Exception exception = new Exception("Error message");
        WebApplicationException webApplicationException = new WebApplicationException(exception);
        doThrow(webApplicationException).when(requestDispatcher).dispatch(request, response);

        requestHandler.handleRequest(request, response);

        ArgumentCaptor<Response> argumentCaptor = ArgumentCaptor.forClass(Response.class);
        verify(response).setResponse(argumentCaptor.capture());
        assertEquals(INTERNAL_SERVER_ERROR, argumentCaptor.getValue().getStatusInfo());
        assertEquals("Error message", argumentCaptor.getValue().getEntity());
    }

    @Test
    public void usesMessageFromWebApplicationExceptionForResponseEntityWhenResponseFromWebApplicationExceptionDoesNotHaveEntity() throws Exception {
        WebApplicationException webApplicationException = new WebApplicationException("Error message");
        doThrow(webApplicationException).when(requestDispatcher).dispatch(request, response);

        requestHandler.handleRequest(request, response);

        ArgumentCaptor<Response> argumentCaptor = ArgumentCaptor.forClass(Response.class);
        verify(response).setResponse(argumentCaptor.capture());
        assertEquals(INTERNAL_SERVER_ERROR, argumentCaptor.getValue().getStatusInfo());
        assertEquals("Error message", argumentCaptor.getValue().getEntity());
    }

    @Test
    public void usesToStringOfCauseOfWebApplicationExceptionForResponseEntityWhenResponseFromWebApplicationExceptionDoesNotHaveEntityAndCauseDoesNotHaveMessage() throws Exception {
        Exception exception = new Exception();
        WebApplicationException webApplicationException = new WebApplicationException(exception);
        doThrow(webApplicationException).when(requestDispatcher).dispatch(request, response);

        requestHandler.handleRequest(request, response);

        ArgumentCaptor<Response> argumentCaptor = ArgumentCaptor.forClass(Response.class);
        verify(response).setResponse(argumentCaptor.capture());
        assertEquals(INTERNAL_SERVER_ERROR, argumentCaptor.getValue().getStatusInfo());
        assertEquals(exception.toString(), argumentCaptor.getValue().getEntity());
    }

    @Test
    public void throwsUnhandledExceptionWithCauseOfInternalExceptionWhenJspErrorPageForCauseIsAvailable() throws Exception {
        ErrorPages errorPages = mock(ErrorPages.class);
        Exception exception = new Exception();
        when(errorPages.hasErrorPage(exception)).thenReturn(true);
        when(environmentContext.get(ErrorPages.class)).thenReturn(errorPages);

        InternalException internalException = new InternalException(exception);
        doThrow(internalException).when(requestDispatcher).dispatch(request, response);

        thrown.expect(UnhandledException.class);
        thrown.expectCause(exceptionSameInstance(exception));
        requestHandler.handleRequest(request, response);
    }

    @Test
    public void throwsUnhandledExceptionWithInternalExceptionWhenJspErrorPageForInternalExceptionIsAvailable() throws Exception {
        ErrorPages errorPages = mock(ErrorPages.class);
        InternalException internalException = new InternalException(new Exception());
        when(errorPages.hasErrorPage(internalException)).thenReturn(true);
        when(environmentContext.get(ErrorPages.class)).thenReturn(errorPages);

        doThrow(internalException).when(requestDispatcher).dispatch(request, response);

        thrown.expect(UnhandledException.class);
        thrown.expectCause(exceptionSameInstance(internalException));
        requestHandler.handleRequest(request, response);
    }

    @Test
    public void usesExceptionMapperToConvertCauseOfInternalExceptionToResponse() throws Exception {
        Exception exception = new Exception();
        InternalException internalException = new InternalException(exception);
        doThrow(internalException).when(requestDispatcher).dispatch(request, response);

        ExceptionMapper<Exception> exceptionMapper = mock(ExceptionMapper.class);
        when(exceptionMapper.toResponse(exception)).thenReturn(serverError().entity("response from exception mapper").build());
        when(providers.getExceptionMapper(Exception.class)).thenReturn(exceptionMapper);

        requestHandler.handleRequest(request, response);

        ArgumentCaptor<Response> argumentCaptor = ArgumentCaptor.forClass(Response.class);
        verify(response).setResponse(argumentCaptor.capture());
        assertEquals(INTERNAL_SERVER_ERROR, argumentCaptor.getValue().getStatusInfo());
        assertEquals("response from exception mapper", argumentCaptor.getValue().getEntity());
    }

    @Test
    public void throwsUnhandledExceptionWithCauseOfInternalExceptionWhenExceptionMapperForCauseIsNotAvailable() throws Exception {
        Exception exception = new Exception();
        InternalException internalException = new InternalException(exception);
        doThrow(internalException).when(requestDispatcher).dispatch(request, response);

        when(providers.getExceptionMapper(Exception.class)).thenReturn(null);

        thrown.expect(UnhandledException.class);
        thrown.expectCause(exceptionSameInstance(exception));
        requestHandler.handleRequest(request, response);
    }
}