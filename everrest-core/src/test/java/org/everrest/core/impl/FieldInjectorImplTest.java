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
import org.everrest.core.FieldInjector;
import org.everrest.core.Parameter;
import org.everrest.core.method.ParameterResolver;
import org.everrest.core.method.ParameterResolverFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatcher;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MultivaluedHashMap;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.everrest.core.$matchers.ExceptionMatchers.webApplicationExceptionWithStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FieldInjectorImplTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ParameterResolverFactory       parameterResolverFactory;
    private ParameterResolver<PathParam>   pathParameterResolver;
    private ParameterResolver<QueryParam>  queryParameterResolver;
    private ParameterResolver<MatrixParam> matrixParameterResolver;
    private ParameterResolver<CookieParam> cookieParameterResolver;
    private ParameterResolver<HeaderParam> headerParameterResolver;
    private ApplicationContext             applicationContext;
    private DependencySupplier             dependencySupplier;

    @Before
    public void setUp() throws Exception {
        mockParameterResolverFactory();
        mockApplicationContext();
    }

    @After
    public void tearDown() throws Exception {
        ApplicationContext.setCurrent(null);
    }

    private void mockApplicationContext() {
        dependencySupplier = mock(DependencySupplier.class);
        applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(applicationContext.getDependencySupplier()).thenReturn(dependencySupplier);
        when(applicationContext.getParameterResolverFactory()).thenReturn(parameterResolverFactory);
        ApplicationContext.setCurrent(applicationContext);
    }

    @SuppressWarnings("unchecked")
    private void mockParameterResolverFactory() {
        parameterResolverFactory = mock(ParameterResolverFactory.class);
        pathParameterResolver = mock(ParameterResolver.class);
        queryParameterResolver = mock(ParameterResolver.class);
        matrixParameterResolver = mock(ParameterResolver.class);
        cookieParameterResolver = mock(ParameterResolver.class);
        headerParameterResolver = mock(ParameterResolver.class);

        when(parameterResolverFactory.createParameterResolver(isA(PathParam.class))).thenReturn(pathParameterResolver);
        when(parameterResolverFactory.createParameterResolver(isA(QueryParam.class))).thenReturn(queryParameterResolver);
        when(parameterResolverFactory.createParameterResolver(isA(MatrixParam.class))).thenReturn(matrixParameterResolver);
        when(parameterResolverFactory.createParameterResolver(isA(CookieParam.class))).thenReturn(cookieParameterResolver);
        when(parameterResolverFactory.createParameterResolver(isA(HeaderParam.class))).thenReturn(headerParameterResolver);
    }

    @Test
    public void createsFieldInjectorForField() throws Exception {
        FieldInjectorImpl fieldInjector = new FieldInjectorImpl(Resource.class.getDeclaredField("pathParam"));

        assertEquals("pathParam", fieldInjector.getName());
        assertEquals(PathParam.class, fieldInjector.getAnnotation().annotationType());
        assertEquals(String.class, fieldInjector.getParameterClass());
        assertEquals(1, fieldInjector.getAnnotations().length);
        assertNull(fieldInjector.getDefaultValue());
        assertFalse(fieldInjector.isEncoded());
    }

    @Test
    public void createsFieldInjectorForEncodedField() throws Exception {
        FieldInjectorImpl fieldInjector = new FieldInjectorImpl(Resource.class.getDeclaredField("queryParam"));

        assertEquals("queryParam", fieldInjector.getName());
        assertEquals(QueryParam.class, fieldInjector.getAnnotation().annotationType());
        assertEquals(String.class, fieldInjector.getParameterClass());
        assertEquals(2, fieldInjector.getAnnotations().length);
        assertNull(fieldInjector.getDefaultValue());
        assertTrue(fieldInjector.isEncoded());
    }

    @Test
    public void createsFieldInjectorForFieldWIthDefaultValue() throws Exception {
        FieldInjectorImpl fieldInjector = new FieldInjectorImpl(Resource.class.getDeclaredField("headerParam"));

        assertEquals("headerParam", fieldInjector.getName());
        assertEquals(HeaderParam.class, fieldInjector.getAnnotation().annotationType());
        assertEquals(String.class, fieldInjector.getParameterClass());
        assertEquals(2, fieldInjector.getAnnotations().length);
        assertEquals("default", fieldInjector.getDefaultValue());
        assertFalse(fieldInjector.isEncoded());
    }

    @Test
    public void injectsField() throws Exception {
        when(pathParameterResolver.resolve(isA(Parameter.class), eq(applicationContext))).thenReturn("path parameter");

        FieldInjectorImpl fieldInjector = new FieldInjectorImpl(Resource.class.getDeclaredField("pathParam"));
        Resource instance = new Resource();
        fieldInjector.inject(instance, applicationContext);

        assertEquals("path parameter", instance.pathParam);
    }

    @Test
    public void usesSetterForSettingField() throws Exception {
        when(headerParameterResolver.resolve(isA(Parameter.class), eq(applicationContext))).thenReturn("header parameter");

        FieldInjectorImpl fieldInjector = new FieldInjectorImpl(Resource.class.getDeclaredField("headerParam"));
        Resource instance = spy(new Resource());
        fieldInjector.inject(instance, applicationContext);

        assertEquals("header parameter", instance.headerParam);
        verify(instance).setHeaderParam("header parameter");
    }

    @Test
    public void injectsExternalDependencyToField() throws Exception {
        Dependency dependency = new Dependency();
        when(dependencySupplier.getInstance(argThat(new ArgumentMatcher<ConstructorParameter>() {
            @Override
            public boolean matches(Object argument) {
                return ((FieldInjector)argument).getParameterClass() == Dependency.class;
            }
        }))).thenReturn(dependency);

        FieldInjectorImpl fieldInjector = new FieldInjectorImpl(Resource.class.getDeclaredField("dependency"));
        Resource instance = new Resource();
        fieldInjector.inject(instance, applicationContext);

        assertSame(dependency, instance.dependency);
    }

    @Test
    public void throwsWebApplicationExceptionWithStatus_NOT_FOUND_WhenFieldAnnotatedWithPathParamAnnotationCanNotBeResolved() throws Exception {
        when(pathParameterResolver.resolve(isA(Parameter.class), eq(applicationContext))).thenThrow(new Exception());

        FieldInjectorImpl fieldInjector = new FieldInjectorImpl(Resource.class.getDeclaredField("pathParam"));
        Resource instance = new Resource();

        thrown.expect(webApplicationExceptionWithStatus(NOT_FOUND));
        fieldInjector.inject(instance, applicationContext);
    }

    @Test
    public void throwsWebApplicationExceptionWithStatus_NOT_FOUND_WhenFieldAnnotatedWithQueryParamAnnotationCanNotBeResolved() throws Exception {
        when(queryParameterResolver.resolve(isA(Parameter.class), eq(applicationContext))).thenThrow(new Exception());

        FieldInjectorImpl fieldInjector = new FieldInjectorImpl(Resource.class.getDeclaredField("queryParam"));
        Resource instance = new Resource();

        thrown.expect(webApplicationExceptionWithStatus(NOT_FOUND));
        fieldInjector.inject(instance, applicationContext);
    }

    @Test
    public void throwsWebApplicationExceptionWithStatus_NOT_FOUND_WhenFieldAnnotatedWithMatrixParamAnnotationCanNotBeResolved() throws Exception {
        when(matrixParameterResolver.resolve(isA(Parameter.class), eq(applicationContext))).thenThrow(new Exception());

        FieldInjectorImpl fieldInjector = new FieldInjectorImpl(Resource.class.getDeclaredField("matrixParam"));
        Resource instance = new Resource();

        thrown.expect(webApplicationExceptionWithStatus(NOT_FOUND));
        fieldInjector.inject(instance, applicationContext);
    }

    @Test
    public void throwsWebApplicationExceptionWithStatus_BAD_REQUEST_WhenFieldAnnotatedWithHeaderParamAnnotationCanNotBeResolved() throws Exception {
        when(headerParameterResolver.resolve(isA(Parameter.class), eq(applicationContext))).thenThrow(new Exception());

        FieldInjectorImpl fieldInjector = new FieldInjectorImpl(Resource.class.getDeclaredField("headerParam"));
        Resource instance = new Resource();

        thrown.expect(webApplicationExceptionWithStatus(BAD_REQUEST));
        fieldInjector.inject(instance, applicationContext);
    }

    @Test
    public void throwsWebApplicationExceptionWithStatus_BAD_REQUEST_WhenFieldAnnotatedWithCookieParamAnnotationCanNotBeResolved() throws Exception {
        when(cookieParameterResolver.resolve(isA(Parameter.class), eq(applicationContext))).thenThrow(new Exception());

        FieldInjectorImpl fieldInjector = new FieldInjectorImpl(Resource.class.getDeclaredField("cookieParam"));
        Resource instance = new Resource();

        thrown.expect(webApplicationExceptionWithStatus(BAD_REQUEST));
        fieldInjector.inject(instance, applicationContext);
    }

    @Test
    public void throwsWebApplicationExceptionWithStatus_INTERNAL_SERVER_ERROR_WhenNotAnnotatedFieldCanNotBeResolved() throws Exception {
        when(dependencySupplier.getInstance(argThat(new ArgumentMatcher<ConstructorParameter>() {
            @Override
            public boolean matches(Object argument) {
                return ((FieldInjector)argument).getParameterClass() == Dependency.class;
            }
        }))).thenThrow(new RuntimeException());

        FieldInjectorImpl fieldInjector = new FieldInjectorImpl(Resource.class.getDeclaredField("dependency"));
        Resource instance = new Resource();

        thrown.expect(webApplicationExceptionWithStatus(INTERNAL_SERVER_ERROR));
        fieldInjector.inject(instance, applicationContext);
    }

    @Test
    public void throwsRuntimeExceptionWhenFieldHasTwoTypesOfAnnotation() throws Exception {
        thrown.expect(RuntimeException.class);
        new FieldInjectorImpl(InvalidResource.class.getDeclaredField("pathParam"));
    }

    @Path("/a/{x}")
    public static class Resource {
        @PathParam("x")
        private String pathParam;
        @Encoded
        @QueryParam("q")
        private String queryParam;
        @MatrixParam("m")
        private String matrixParam;
        @CookieParam("c")
        private String cookieParam;
        @DefaultValue("default")
        @HeaderParam("h")
        private String headerParam;

        public void setHeaderParam(String headerParam) {
            this.headerParam = headerParam;
        }

        private Dependency dependency;
    }

    @Path("/a/{x}")
    public static class InvalidResource {
        @PathParam("x")
        @HeaderParam("h")
        private String pathParam;
    }

    public static class Dependency {
    }
}