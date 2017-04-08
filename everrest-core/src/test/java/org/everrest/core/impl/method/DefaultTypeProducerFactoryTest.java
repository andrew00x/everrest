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

import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.ApplicationContext;
import org.everrest.core.impl.DefaultProviderBinder;
import org.everrest.core.method.TypeProducer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ParamConverter;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.everrest.core.util.ParameterizedTypeImpl.newParameterizedType;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultTypeProducerFactoryTest {
    private DefaultTypeProducerFactory typeProducerFactory;
    private ProviderBinder providers;

    @Before
    public void setUp() throws Exception {
        providers = mock(DefaultProviderBinder.class);
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getProviders()).thenReturn(providers);
        ApplicationContext.setCurrent(context);
        typeProducerFactory = new DefaultTypeProducerFactory();
    }

    @After
    public void tearDown() throws Exception {
        ApplicationContext.setCurrent(null);
    }

    @Test
    public void createsListProducer() throws Exception {
        ParamConverter<String> converter = mock(ParamConverter.class);
        when(converter.fromString(isA(String.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(providers.getConverter(String.class, String.class, new Annotation[0])).thenReturn(converter);

        TypeProducer<List> typeProducer = typeProducerFactory.createTypeProducer(List.class, newParameterizedType(List.class, String.class), new Annotation[0]);
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.addAll("a", "hello", "world");

        List list = typeProducer.createValue("a", params, null);
        assertEquals(newArrayList("hello", "world"), list);
    }

    @Test
    public void createsSetProducer() throws Exception {
        ParamConverter<String> converter = mock(ParamConverter.class);
        when(converter.fromString(isA(String.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(providers.getConverter(String.class, String.class, new Annotation[0])).thenReturn(converter);

        TypeProducer<Set> typeProducer = typeProducerFactory.createTypeProducer(Set.class, newParameterizedType(Set.class, String.class), new Annotation[0]);
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.addAll("a", "hello", "world");

        Set set = typeProducer.createValue("a", params, null);
        assertEquals(newHashSet("hello", "world"), set);
    }

    @Test
    public void createsSortedSetProducer() throws Exception {
        ParamConverter<String> converter = mock(ParamConverter.class);
        when(converter.fromString(isA(String.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(providers.getConverter(String.class, String.class, new Annotation[0])).thenReturn(converter);

        TypeProducer<SortedSet> typeProducer = typeProducerFactory.createTypeProducer(SortedSet.class, newParameterizedType(SortedSet.class, String.class), new Annotation[0]);
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.addAll("a", "hello", "world");

        SortedSet set = typeProducer.createValue("a", params, null);
        assertEquals(newHashSet("hello", "world"), set);
    }

    @Test
    public void createsTypeProducer() throws Exception {
        ParamConverter<String> converter = mock(ParamConverter.class);
        when(converter.fromString(isA(String.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(providers.getConverter(String.class, String.class, new Annotation[0])).thenReturn(converter);

        TypeProducer<String> typeProducer = typeProducerFactory.createTypeProducer(String.class, String.class, new Annotation[0]);
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.addAll("a", "hello", "world");

        assertEquals("hello", typeProducer.createValue("a", params, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionWhenParamConverterIsNotAvailable() throws Exception {
        typeProducerFactory.createTypeProducer(String.class, String.class, new Annotation[0]);
    }
}