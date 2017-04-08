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
import org.everrest.core.ConfigurationProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collection;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RuntimeConfigurationPropertiesTest {
    @Mock private ConfigurationProperties originalProperties;
    @InjectMocks private RuntimeConfigurationProperties runtimeProperties;

    @Test
    public void getsProperties() {
        Map<String, Object> properties = ImmutableMap.of("name", "value");
        when(originalProperties.getProperties()).thenReturn(properties);
        assertEquals(properties, runtimeProperties.getProperties());
    }

    @Test
    public void getsProperty() {
        when(originalProperties.getProperty("name")).thenReturn("value");
        assertEquals("value", runtimeProperties.getProperty("name"));
    }

    @Test
    public void getsPropertyNames() {
        Collection<String> names = newArrayList("name1", "name2");
        when(originalProperties.getPropertyNames()).thenReturn(names);
        assertEquals(names, runtimeProperties.getPropertyNames());
    }

    @Test
    public void setsPropertyDoesNotUpdateOriginalPropertiesButSavesChangesInCopyOfOriginalProperties() {
        runtimeProperties.setProperty("name", "value");
        assertEquals("value", runtimeProperties.getProperty("name"));
        verify(originalProperties, never()).setProperty("name", "value");
    }

    @Test
    public void removesPropertyDoesNotUpdateOriginalPropertiesButSavesChangesInCopyOfOriginalProperties() {
        Map<String, Object> properties = ImmutableMap.of("name1", "value1", "name2", "value2");
        when(originalProperties.getProperties()).thenReturn(properties);

        runtimeProperties.removeProperty("name1");

        assertNull(runtimeProperties.getProperty("name1"));
        assertEquals("value2", runtimeProperties.getProperty("name2"));
        verify(originalProperties, never()).removeProperty("name1");
    }
}