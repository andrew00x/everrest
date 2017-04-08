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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultTypeProducerTest {
    @Mock private ParamConverter<Integer> paramConverter;
    @InjectMocks private DefaultTypeProducer typeProducer;

    @Test
    public void createsObject() throws Exception {
        when(paramConverter.fromString("1")).thenReturn(1);
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.addAll("a", "1", "2");

        assertEquals(1, typeProducer.createValue("a", params, null));
    }

    @Test
    public void createsObjectFromDefaultValue() throws Exception {
        when(paramConverter.fromString("10")).thenReturn(10);
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        assertEquals(10, typeProducer.createValue("a", params, "10"));
    }

    @Test
    public void returnsNullWhenParametersAndDefaultValueAreNotSpecified() throws Exception {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        assertNull(typeProducer.createValue("a", params, null));
    }
}