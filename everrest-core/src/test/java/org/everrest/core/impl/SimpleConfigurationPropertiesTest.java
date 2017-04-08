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
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.everrest.core.SimpleConfigurationProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(DataProviderRunner.class)
public class SimpleConfigurationPropertiesTest {
    private SimpleConfigurationProperties properties;

    @Before
    public void setUp() throws Exception {
        Map<String, Object> map = ImmutableMap.of("name1", "value1",
                                                  "name2", "value2");
        properties = new SimpleConfigurationProperties(map);
    }

    @Test
    public void getsProperties() {
        assertEquals(ImmutableMap.of("name1", "value1", "name2", "value2"), properties.getProperties());
    }

    @Test
    public void getsProperty() {
        assertEquals("value1", properties.getProperty("name1"));
    }

    @Test
    public void getsPropertyNames() {
        assertEquals(newHashSet("name1", "name2"), newHashSet(properties.getPropertyNames()));
    }

    @Test
    public void setsProperty() {
        properties.setProperty("name1", "new_value1");
        assertEquals("new_value1", properties.getProperty("name1"));
    }

    @Test
    public void removesProperty() {
        properties.removeProperty("name1");
        assertNull(properties.getProperty("name1"));
    }

    @Test
    public void removesPropertyIfNullValueProvided() {
        properties.setProperty("name2", null);

        assertEquals(ImmutableMap.of("name1", "value1"), properties.getProperties());
    }

    @DataProvider
    public static Object[][] forBooleanPropertyTest() {
        return new Object[][] {
                {"1", true},
                {"yes", true},
                {"true", true},
                {"on", true},
                {"Yes", true},
                {"True", true},
                {"On", true},
                {"", false},
                {"0", false},
                {"Off", false},
                {null, false}
        };
    }

    @Test
    @UseDataProvider("forBooleanPropertyTest")
    public void testBooleanProperty(String booleanPropertyAsString, boolean expectedBooleanRepresentation) {
        properties.setProperty("foo", booleanPropertyAsString);
        assertEquals(expectedBooleanRepresentation, properties.getBooleanProperty("foo", false));
    }

    @Test
    public void ignoresDoublePropertiesWithInvalidFormat() {
        properties.setProperty("foo", "bar");
        assertEquals(11.0, properties.getDoubleProperty("foo", 11.0), 0.0);
    }

    @Test
    public void ignoresIntegerPropertiesWithInvalidFormat() {
        properties.setProperty("foo", "bar");
        assertEquals(123, properties.getIntegerProperty("foo", 123));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getsUnmodifiablePropertiesMap() {
        properties.getProperties().clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getsUnmodifiablePropertyNames() {
        properties.getPropertyNames().clear();
    }
}