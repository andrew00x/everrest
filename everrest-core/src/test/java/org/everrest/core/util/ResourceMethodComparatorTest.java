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
package org.everrest.core.util;

import org.everrest.core.resource.ResourceMethodDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.MediaType;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResourceMethodComparatorTest {
    @Mock private MediaTypeComparator mediaTypeComparator;
    @Mock private ResourceMethodDescriptor methodOne;
    @Mock private ResourceMethodDescriptor methodTwo;
    @InjectMocks private ResourceMethodComparator resourceMethodComparator;

    @Test
    public void comparesResourceMethodsByLastMediaTypeInListOfTypesConsumedByMethod() {
        when(methodOne.consumes()).thenReturn(newArrayList(new MediaType("application", "xml"), new MediaType("text", "plain")));
        when(methodTwo.consumes()).thenReturn(newArrayList(new MediaType("application", "json"), new MediaType("text", "*")));

        when(mediaTypeComparator.compare(new MediaType("text", "plain"), new MediaType("text", "*"))).thenReturn(-1);

        assertEquals(-1, resourceMethodComparator.compare(methodOne, methodTwo));
    }

    @Test
    public void comparesResourceMethodsByLastMediaTypeInListOfTypesProducedByMethod() {
        when(methodOne.consumes()).thenReturn(newArrayList(new MediaType("application", "xml")));
        when(methodTwo.consumes()).thenReturn(newArrayList(new MediaType("application", "json")));
        when(methodOne.produces()).thenReturn(newArrayList(new MediaType("application", "xml"), new MediaType("text", "plain")));
        when(methodTwo.produces()).thenReturn(newArrayList(new MediaType("application", "json"), new MediaType("text", "*")));

        when(mediaTypeComparator.compare(new MediaType("application", "xml"), new MediaType("application", "json"))).thenReturn(0);
        when(mediaTypeComparator.compare(new MediaType("text", "plain"), new MediaType("text", "*"))).thenReturn(-1);

        assertEquals(-1, resourceMethodComparator.compare(methodOne, methodTwo));
    }

    @Test
    public void comparesResourceMethodsBySizeOfConsumedMediaTypesListWhenMediaTypeComparatorReturnsZero() {
        when(methodOne.consumes()).thenReturn(newArrayList(new MediaType("application", "xml"), new MediaType("application", "*")));
        when(methodTwo.consumes()).thenReturn(newArrayList(new MediaType("application", "*")));
        when(methodOne.produces()).thenReturn(newArrayList(new MediaType("application", "xml"), new MediaType("text", "plain")));
        when(methodTwo.produces()).thenReturn(newArrayList(new MediaType("application", "json"), new MediaType("text", "plain")));

        when(mediaTypeComparator.compare(isA(MediaType.class), isA(MediaType.class))).thenReturn(0);
        when(mediaTypeComparator.compare(isA(MediaType.class), isA(MediaType.class))).thenReturn(0);

        assertEquals(1, resourceMethodComparator.compare(methodOne, methodTwo));
    }

    @Test
    public void comparesResourceMethodsBySizeOfProducedMediaTypesListWhenMediaTypeComparatorReturnsZero() {
        when(methodOne.consumes()).thenReturn(newArrayList(new MediaType("application", "xml")));
        when(methodTwo.consumes()).thenReturn(newArrayList(new MediaType("application", "xml")));
        when(methodOne.produces()).thenReturn(newArrayList(new MediaType("application", "xml"), new MediaType("text", "plain")));
        when(methodTwo.produces()).thenReturn(newArrayList(new MediaType("text", "plain")));

        when(mediaTypeComparator.compare(isA(MediaType.class), isA(MediaType.class))).thenReturn(0);
        when(mediaTypeComparator.compare(isA(MediaType.class), isA(MediaType.class))).thenReturn(0);

        assertEquals(1, resourceMethodComparator.compare(methodOne, methodTwo));
    }
}