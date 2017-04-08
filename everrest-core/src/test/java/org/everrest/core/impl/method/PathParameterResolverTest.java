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

import org.everrest.core.Parameter;
import org.everrest.core.impl.ApplicationContext;
import org.everrest.core.method.TypeProducer;
import org.everrest.core.method.TypeProducerFactory;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.lang.annotation.Annotation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class PathParameterResolverTest {
    private MultivaluedMap<String, String> pathParameters;
    private ApplicationContext             applicationContext;
    private Parameter                      parameter;
    private TypeProducer                   typeProducer;

    private PathParameterResolver pathParameterResolver;

    @Before
    public void setUp() throws Exception {
        pathParameters = new MultivaluedHashMap<>();
        pathParameters.putSingle("foo", "aaa");
        pathParameters.putSingle("bar", "bbb");

        applicationContext = mock(ApplicationContext.class, RETURNS_DEEP_STUBS);
        when(applicationContext.getPathParameters(true)).thenReturn(pathParameters);

        PathParam pathParamAnnotation = mock(PathParam.class);
        when(pathParamAnnotation.value()).thenReturn("foo");

        parameter = mock(Parameter.class);
        when(parameter.getParameterClass()).thenReturn((Class)String.class);
        when(parameter.getGenericType()).thenReturn((Class)String.class);
        when(parameter.getAnnotations()).thenReturn(new Annotation[0]);

        typeProducer = mock(TypeProducer.class);
        TypeProducerFactory typeProducerFactory = mock(TypeProducerFactory.class);
        when(typeProducerFactory.createTypeProducer(String.class, String.class, new Annotation[0])).thenReturn(typeProducer);

        pathParameterResolver = new PathParameterResolver(pathParamAnnotation, typeProducerFactory);
    }

    @Test
    public void retrievesPathParameterFromRequest() throws Exception {
        when(typeProducer.createValue("foo", pathParameters, null)).thenReturn(pathParameters.getFirst("foo"));

        Object resolvedParam = pathParameterResolver.resolve(parameter, applicationContext);

        assertEquals(pathParameters.getFirst("foo"), resolvedParam);
    }

    @Test
    public void retrievesDefaultValueWhenPathParameterDoesNotPresentInRequest() throws Exception {
        when(parameter.getDefaultValue()).thenReturn("default");
        when(typeProducer.createValue("foo", pathParameters, "default")).thenReturn("default");

        Object resolvedParam = pathParameterResolver.resolve(parameter, applicationContext);

        assertEquals("default", resolvedParam);
    }
}