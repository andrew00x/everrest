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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ParamConverter;
import java.util.List;
import java.util.function.Supplier;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CollectionTypeProducerTest {
    @Mock private Supplier<List<Long>> collectionFactory;
    @Mock private ParamConverter<Long> paramConverter;
    @InjectMocks private CollectionTypeProducer collectionTypeProducer;

    @Test
    public void createCollectionWithSingleItem() throws Exception {
        when(paramConverter.fromString("1")).thenReturn(1L);
        when(collectionFactory.get()).thenReturn(newArrayList());
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.addAll("a", "1");

        assertEquals(newArrayList(1L), collectionTypeProducer.createValue("a", params, null));
    }

    @Test
    public void createCollectionWithMultipleItems() throws Exception {
        when(paramConverter.fromString("1")).thenReturn(1L);
        when(paramConverter.fromString("2")).thenReturn(2L);
        when(collectionFactory.get()).thenReturn(newArrayList());
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.addAll("a", "1", "2");

        assertEquals(newArrayList(1L, 2L), collectionTypeProducer.createValue("a", params, null));
    }

    @Test
    public void createCollectionWithSingleDefaultValue() throws Exception {
        when(paramConverter.fromString("10")).thenReturn(10L);
        when(collectionFactory.get()).thenReturn(newArrayList());
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

        assertEquals(newArrayList(10L), collectionTypeProducer.createValue("a", params, "10"));
    }

    @Test
    public void returnsNullWhenParameterListAndDefaultValueAreNotSpecified() throws Exception {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

        assertNull(collectionTypeProducer.createValue("a", params, null));
    }

    @Test
    public void returnsEmptyCollectionWhenParameterListIsEmpty() throws Exception {
        when(collectionFactory.get()).thenReturn(newArrayList());
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.put("a", newArrayList());

        assertTrue(collectionTypeProducer.createValue("a", params, "10").isEmpty());
    }
}