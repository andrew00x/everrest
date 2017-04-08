package org.everrest.core.servlet;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.ServletRequest;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.enumeration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServletRequestAttributesWrapperTest {
    @Mock private ServletRequest servletRequest;
    @InjectMocks private ServletRequestAttributesWrapper attributesWrapper;

    @Test
    public void getsProperties() {
        when(servletRequest.getAttributeNames()).thenReturn(enumeration(newArrayList("name1", "name2")));
        when(servletRequest.getAttribute("name1")).thenReturn("value1");
        when(servletRequest.getAttribute("name2")).thenReturn("value2");

        assertEquals(ImmutableMap.of("name1", "value1", "name2", "value2"), attributesWrapper.getProperties());
    }

    @Test
    public void getsProperty() {
        when(servletRequest.getAttribute("name1")).thenReturn("value1");
        assertEquals("value1", attributesWrapper.getProperty("name1"));
    }

    @Test
    public void getsPropertyNames() {
        when(servletRequest.getAttributeNames()).thenReturn(enumeration(newArrayList("name1", "name2")));
        assertEquals(newHashSet("name1", "name2"), newHashSet(attributesWrapper.getPropertyNames()));
    }

    @Test
    public void setsProperty() {
        attributesWrapper.setProperty("name1", "value1");
        verify(servletRequest).setAttribute("name1", "value1");
    }

    @Test
    public void removesProperty() {
        attributesWrapper.removeProperty("name1");
        assertNull(attributesWrapper.getProperty("name1"));
    }
}
