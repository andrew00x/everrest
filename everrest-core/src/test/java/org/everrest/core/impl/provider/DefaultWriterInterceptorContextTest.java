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
package org.everrest.core.impl.provider;

import org.everrest.core.ConfigurationProperties;
import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.DefaultProviderBinder;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.ext.WriterInterceptor;
import javax.xml.transform.dom.DOMSource;
import java.io.OutputStream;
import java.lang.annotation.Annotation;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.everrest.core.impl.provider.DefaultWriterInterceptorContext.aWriterInterceptorContext;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultWriterInterceptorContextTest {
    private ProviderBinder providers;
    private ConfigurationProperties properties;
    private WriterInterceptor endpointInterceptor;
    private WriterInterceptor userInterceptor;

    private DefaultWriterInterceptorContext writerInterceptorContext;

    @Before
    public void setUp() throws Exception {
        providers = mock(DefaultProviderBinder.class);
        properties = mock(ConfigurationProperties.class);
        userInterceptor = mock(WriterInterceptor.class);
        endpointInterceptor = mock(WriterInterceptor.class);
        when(providers.getWriterInterceptors()).thenReturn(newArrayList(userInterceptor));
        writerInterceptorContext = aWriterInterceptorContext(providers, properties, endpointInterceptor)
                .withMediaType(TEXT_PLAIN_TYPE)
                .withType(String.class)
                .withGenericType(String.class)
                .withAnnotations(new Annotation[0])
                .withEntityStream(mock(OutputStream.class))
                .withEntity("none")
                .withHeaders(new MultivaluedHashMap<>())
                .build();
    }

    @Test
    public void invokesUsersWriterInterceptor() throws Exception {
        writerInterceptorContext.proceed();
        verify(userInterceptor).aroundWriteTo(writerInterceptorContext);
    }

    @Test
    public void invokesEndpointWriterInterceptor() throws Exception {
        doAnswer(invocation -> {
            writerInterceptorContext.proceed();
            return null;
        }).when(userInterceptor).aroundWriteTo(writerInterceptorContext);
        writerInterceptorContext.proceed();
        verify(endpointInterceptor).aroundWriteTo(writerInterceptorContext);
    }

    @Test
    public void setsEntity() {
        writerInterceptorContext.setEntity("hello");
        assertEquals("hello", writerInterceptorContext.getEntity());
    }

    @Test
    public void setsOutputStream() {
        OutputStream entityStream = mock(OutputStream.class);
        writerInterceptorContext.setOutputStream(entityStream);
        assertEquals(entityStream, writerInterceptorContext.getOutputStream());
    }

    @Test
    public void setsMediaType() {
        writerInterceptorContext.setMediaType(APPLICATION_XML_TYPE);
        assertEquals(APPLICATION_XML_TYPE, writerInterceptorContext.getMediaType());
    }

    @Test
    public void setsType() {
        writerInterceptorContext.setType(DOMSource.class);
        assertEquals(DOMSource.class, writerInterceptorContext.getType());
    }

    @Test
    public void setsGenericType() {
        writerInterceptorContext.setGenericType(DOMSource.class);
        assertEquals(DOMSource.class, writerInterceptorContext.getGenericType());
    }

    @Test
    public void setsAnnotations() {
        Annotation[] annotations = {new A2Impl()};
        writerInterceptorContext.setAnnotations(annotations);
        assertArrayEquals(annotations, writerInterceptorContext.getAnnotations());
    }

    @Test(expected = NullPointerException.class)
    public void throwsExceptionWhenSetNullAnnotations() {
        writerInterceptorContext.setAnnotations(null);
    }

    @Test
    public void setsProperty() {
        writerInterceptorContext.setProperty("name", "value");
        verify(properties).setProperty("name", "value");
    }

    @Test
    public void getsProperty() {
        when(properties.getProperty("name")).thenReturn("value");
        assertEquals("value", writerInterceptorContext.getProperty("name"));
    }

    @Test
    public void getsPropertyNames() {
        when(properties.getPropertyNames()).thenReturn(newArrayList("name1", "name2"));
        assertEquals(newArrayList("name1", "name2"), writerInterceptorContext.getPropertyNames());
    }

    @Test
    public void removesProperty() {
        writerInterceptorContext.removeProperty("name");
        verify(properties).removeProperty("name");
    }

    @Test
    public void updatesHeaders() {
        writerInterceptorContext.getHeaders().putSingle(CONTENT_LENGTH, "99");
        assertEquals("99", writerInterceptorContext.getHeaders().getFirst(CONTENT_LENGTH));
    }

    @interface A2 {
    }

    static class A2Impl implements A2 {
        @Override
        public Class<? extends Annotation> annotationType() {
            return A2.class;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof A2;
        }
    }
}