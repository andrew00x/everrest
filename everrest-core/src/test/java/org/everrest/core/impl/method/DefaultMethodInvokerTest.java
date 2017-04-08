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
package org.everrest.core.impl.method;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.everrest.core.Parameter;
import org.everrest.core.impl.ApplicationContext;
import org.everrest.core.impl.InternalException;
import org.everrest.core.method.ParameterResolver;
import org.everrest.core.method.ParameterResolverFactory;
import org.everrest.core.resource.GenericResourceMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static org.everrest.core.$matchers.ExceptionMatchers.exceptionSameInstance;
import static org.everrest.core.$matchers.ExceptionMatchers.webApplicationExceptionWithStatus;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(DataProviderRunner.class)
public class DefaultMethodInvokerTest {
    private static final String ARGUMENT_VALUE = "to be or not to be";
    private static final Class  ARGUMENT_CLASS = String.class;
    private static final String RESOURCE_PATH  = "/a/b";

    public static class EchoResource {
        private Exception thrownException;

        public EchoResource(Exception thrownException) {
            this.thrownException = thrownException;
        }

        public EchoResource() {
            this(null);
        }

        @SuppressWarnings("unused")
        public String echo(String phrase) throws Exception {
            if (thrownException != null) {
                throw thrownException;
            }
            return phrase;
        }
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ParameterResolverFactory  parameterResolverFactory;
    private GenericResourceMethod     resourceMethod;
    private Parameter                 entityParameter;
    private Annotation                parameterAnnotation;
    private Parameter                 annotatedParameter;
    private ParameterResolver         parameterResolver;
    private ApplicationContext        applicationContext;
    private MessageBodyReader<String> messageBodyReader;

    private DefaultMethodInvoker      methodInvoker;

    @Before
    public void setUp() throws Exception {
        mockEntityParameter();
        mockParameterAnnotation();
        mockAnnotatedParameter();
        mockResourceMethod();
        mockApplicationContext();
        mockParameterResolverFactory();
        mockEntityReader();

        methodInvoker = new DefaultMethodInvoker(parameterResolverFactory);
    }

    @After
    public void tearDown() throws Exception {
        ApplicationContext.setCurrent(null);
    }

    @Test
    public void invokesMethodWithEntityParameter() throws Exception {
        when(resourceMethod.getMethodParameters()).thenReturn(newArrayList(entityParameter));

        Object invocationResult = methodInvoker.invokeMethod(new EchoResource(), resourceMethod, applicationContext);

        assertEquals(ARGUMENT_VALUE, invocationResult);
    }

    @Test
    public void invokesMethodWithNullEntityParameterWhenEntityStreamDoesNotPresent() throws Exception {
        when(applicationContext.getContainerRequest().getEntityStream()).thenReturn(null);
        when(resourceMethod.getMethodParameters()).thenReturn(newArrayList(entityParameter));

        Object invocationResult = methodInvoker.invokeMethod(new EchoResource(), resourceMethod, applicationContext);

        assertEquals(null, invocationResult);
    }

    @Test
    public void throwsWebApplicationExceptionWhenMessageBodyReaderForEntityParameterIsNotAvailable() throws Exception {
        when(applicationContext.getProviders()
                               .getMessageBodyReader((Class)entityParameter.getParameterClass(),
                                                     entityParameter.getGenericType(),
                                                     entityParameter.getAnnotations(),
                                                     applicationContext.getContainerRequest().getMediaType()))
                .thenReturn(null);
        when(resourceMethod.getMethodParameters()).thenReturn(newArrayList(entityParameter));

        thrown.expect(webApplicationExceptionWithStatus(UNSUPPORTED_MEDIA_TYPE));
        methodInvoker.invokeMethod(new EchoResource(), resourceMethod, applicationContext);
    }

    @Test
    public void invokesMethodWithNullEntityParameterWhenMessageBodyReaderIsNotAvailableAndBothContentTypeAndContentLengthAreNotSet() throws Exception {
        when(applicationContext.getContainerRequest().getMediaType()).thenReturn(null);
        when(applicationContext.getProviders()
                               .getMessageBodyReader((Class)entityParameter.getParameterClass(),
                                                     entityParameter.getGenericType(),
                                                     entityParameter.getAnnotations(),
                                                     applicationContext.getContainerRequest().getMediaType()))
                .thenReturn(null);
        when(resourceMethod.getMethodParameters()).thenReturn(newArrayList(entityParameter));

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(CONTENT_LENGTH, "0");
        when(applicationContext.getContainerRequest().getRequestHeaders()).thenReturn(headers);

        Object invocationResult = methodInvoker.invokeMethod(new EchoResource(), resourceMethod, applicationContext);

        assertEquals(null, invocationResult);
    }

    @Test
    public void rethrowWebApplicationExceptionThatOccursWhenReadEntityParameter() throws Exception {
        WebApplicationException thrownByMessageBodyReader = new WebApplicationException();
        when(messageBodyReader.readFrom((Class)entityParameter.getParameterClass(),
                                        entityParameter.getGenericType(),
                                        entityParameter.getAnnotations(),
                                        applicationContext.getContainerRequest().getMediaType(),
                                        applicationContext.getContainerRequest().getRequestHeaders(),
                                        applicationContext.getContainerRequest().getEntityStream())).thenThrow(thrownByMessageBodyReader);

        when(resourceMethod.getMethodParameters()).thenReturn(newArrayList(entityParameter));

        thrown.expect(exceptionSameInstance(thrownByMessageBodyReader));

        methodInvoker.invokeMethod(new EchoResource(), resourceMethod, applicationContext);
    }

    @Test
    public void rethrowInternalExceptionThatOccursWhenReadEntityParameter() throws Exception {
        InternalException thrownByMessageBodyReader = new InternalException(new Exception());
        when(messageBodyReader.readFrom((Class)entityParameter.getParameterClass(),
                                        entityParameter.getGenericType(),
                                        entityParameter.getAnnotations(),
                                        applicationContext.getContainerRequest().getMediaType(),
                                        applicationContext.getContainerRequest().getRequestHeaders(),
                                        applicationContext.getContainerRequest().getEntityStream())).thenThrow(thrownByMessageBodyReader);

        when(resourceMethod.getMethodParameters()).thenReturn(newArrayList(entityParameter));

        thrown.expect(exceptionSameInstance(thrownByMessageBodyReader));

        methodInvoker.invokeMethod(new EchoResource(), resourceMethod, applicationContext);
    }

    @Test
    public void wrapsExceptionThatOccursWhenReadEntityParameterWithInternalException() throws Exception {
        Exception thrownByMessageBodyReader = new IOException();
        when(messageBodyReader.readFrom((Class)entityParameter.getParameterClass(),
                                        entityParameter.getGenericType(),
                                        entityParameter.getAnnotations(),
                                        applicationContext.getContainerRequest().getMediaType(),
                                        applicationContext.getContainerRequest().getRequestHeaders(),
                                        applicationContext.getContainerRequest().getEntityStream())).thenThrow(thrownByMessageBodyReader);

        when(resourceMethod.getMethodParameters()).thenReturn(newArrayList(entityParameter));

        thrown.expect(InternalException.class);
        thrown.expectCause(exceptionSameInstance(thrownByMessageBodyReader));

        methodInvoker.invokeMethod(new EchoResource(), resourceMethod, applicationContext);
    }

    @Test
    public void invokesMethodWithAnnotatedParameter() throws Exception {
        when(resourceMethod.getMethodParameters()).thenReturn(newArrayList(annotatedParameter));

        Object invocationResult = methodInvoker.invokeMethod(new EchoResource(), resourceMethod, applicationContext);

        assertEquals(ARGUMENT_VALUE, invocationResult);
    }

    @DataProvider
    public static Object[][] annotationTypesWhenNeedToThrowWebApplicationExceptionWithStatusNotFound() {
        return new Object[][]{
                {MatrixParam.class},
                {QueryParam.class},
                {PathParam.class}
        };
    }

    @UseDataProvider("annotationTypesWhenNeedToThrowWebApplicationExceptionWithStatusNotFound")
    @Test
    public <A extends Annotation> void throwsWebApplicationExceptionWithStatusNotFoundWhenFailResolveParameterAnnotatedWith(Class<A> annotationType) throws Exception {
        when(resourceMethod.getMethodParameters()).thenReturn(newArrayList(annotatedParameter));
        when(parameterAnnotation.annotationType()).thenReturn((Class)annotationType);
        Exception thrownByParameterResolver = new Exception();
        when(parameterResolver.resolve(annotatedParameter, applicationContext)).thenThrow(thrownByParameterResolver);

        thrown.expect(webApplicationExceptionWithStatus(NOT_FOUND));
        methodInvoker.invokeMethod(new EchoResource(), resourceMethod, applicationContext);
    }

    @DataProvider
    public static Object[][] annotationTypesWhenNeedToThrowWebApplicationExceptionWithStatusBadRequest() {
        return new Object[][]{
                {CookieParam.class},
                {Context.class},
                {FormParam.class},
                {HeaderParam.class}
        };
    }

    @UseDataProvider("annotationTypesWhenNeedToThrowWebApplicationExceptionWithStatusBadRequest")
    @Test
    public <A extends Annotation> void throwsWebApplicationExceptionWithStatusBadRequestWhenFailResolveParameterAnnotatedWith(Class<A> annotationType) throws Exception {
        when(resourceMethod.getMethodParameters()).thenReturn(newArrayList(annotatedParameter));
        when(parameterAnnotation.annotationType()).thenReturn((Class)annotationType);
        Exception thrownByParameterResolver = new Exception();
        when(parameterResolver.resolve(annotatedParameter, applicationContext)).thenThrow(thrownByParameterResolver);

        thrown.expect(webApplicationExceptionWithStatus(BAD_REQUEST));
        methodInvoker.invokeMethod(new EchoResource(), resourceMethod, applicationContext);
    }

    @Test
    public void rethrowWebApplicationExceptionThatThrownByInvocatedMethod() throws Exception {
        when(resourceMethod.getMethodParameters()).thenReturn(newArrayList(entityParameter));

        WebApplicationException thrownByMethod = new WebApplicationException();
        thrown.expect(exceptionSameInstance(thrownByMethod));

        methodInvoker.invokeMethod(new EchoResource(thrownByMethod), resourceMethod, applicationContext);
    }

    @Test
    public void rethrowInternalExceptionThatThrownByInvocatedMethod() throws Exception {
        when(resourceMethod.getMethodParameters()).thenReturn(newArrayList(entityParameter));

        InternalException thrownByMethod = new InternalException(new Exception());
        thrown.expect(exceptionSameInstance(thrownByMethod));

        methodInvoker.invokeMethod(new EchoResource(thrownByMethod), resourceMethod, applicationContext);
    }

    @Test
    public void wrapsExceptionThatThrownByInvocatedMethodWithInternalException() throws Exception {
        when(resourceMethod.getMethodParameters()).thenReturn(newArrayList(entityParameter));

        Exception thrownByMethod = new Exception();
        thrown.expect(InternalException.class);
        thrown.expectCause(exceptionSameInstance(thrownByMethod));

        methodInvoker.invokeMethod(new EchoResource(thrownByMethod), resourceMethod, applicationContext);
    }

    private void mockEntityParameter() {
        entityParameter = mock(Parameter.class);
        when(entityParameter.getParameterClass()).thenReturn(ARGUMENT_CLASS);
    }

    private void mockAnnotatedParameter() {
        annotatedParameter = mock(Parameter.class);
        when(annotatedParameter.getParameterClass()).thenReturn(ARGUMENT_CLASS);
        when(annotatedParameter.getAnnotation()).thenReturn(parameterAnnotation);
    }

    private void mockParameterAnnotation() {
        PathParam parameterAnnotation = mock(PathParam.class);
        when(parameterAnnotation.annotationType()).thenReturn((Class)PathParam.class);
        this.parameterAnnotation = parameterAnnotation;
    }

    private void mockParameterResolverFactory() throws Exception {
        parameterResolver = mock(ParameterResolver.class);
        when(parameterResolver.resolve(annotatedParameter, applicationContext)).thenReturn(ARGUMENT_VALUE);

        parameterResolverFactory = mock(ParameterResolverFactory.class);
        when(parameterResolverFactory.createParameterResolver(parameterAnnotation)).thenReturn(parameterResolver);
    }

    private void mockResourceMethod() throws Exception {
        resourceMethod = mock(GenericResourceMethod.class);
        when(resourceMethod.getMethod()).thenReturn(EchoResource.class.getMethod("echo", String.class));
        when(resourceMethod.getMethodParameters()).thenReturn(newArrayList());
    }

    private void mockApplicationContext() {
        applicationContext = mock(ApplicationContext.class, RETURNS_DEEP_STUBS);
        when(applicationContext.getPath()).thenReturn(RESOURCE_PATH);
        when(applicationContext.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(applicationContext.getContainerRequest().getRequestHeaders()).thenReturn(new MultivaluedHashMap<>());
        when(applicationContext.getContainerRequest().getMediaType()).thenReturn(TEXT_PLAIN_TYPE);
        when(applicationContext.getProviders().getReaderInterceptors()).thenReturn(new ArrayList<>());
        ApplicationContext.setCurrent(applicationContext);
    }

    private void mockEntityReader() throws java.io.IOException {
        messageBodyReader = mock(MessageBodyReader.class);
        when(messageBodyReader.readFrom((Class)entityParameter.getParameterClass(),
                                        entityParameter.getGenericType(),
                                        entityParameter.getAnnotations(),
                                        applicationContext.getContainerRequest().getMediaType(),
                                        applicationContext.getContainerRequest().getRequestHeaders(),
                                        applicationContext.getContainerRequest().getEntityStream())).thenReturn(ARGUMENT_VALUE);
        when(applicationContext.getProviders()
                .getMessageBodyReader((Class) entityParameter.getParameterClass(),
                        entityParameter.getGenericType(),
                        entityParameter.getAnnotations(),
                        applicationContext.getContainerRequest().getMediaType()))
                .thenReturn(messageBodyReader);
    }
}