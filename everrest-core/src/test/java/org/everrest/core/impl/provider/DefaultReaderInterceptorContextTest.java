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
import javax.ws.rs.ext.ReaderInterceptor;
import javax.xml.transform.dom.DOMSource;
import java.io.InputStream;
import java.lang.annotation.Annotation;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.everrest.core.impl.provider.DefaultReaderInterceptorContext.aReaderInterceptorContext;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultReaderInterceptorContextTest {
    private ProviderBinder providers;
    private ConfigurationProperties properties;
    private ReaderInterceptor endpointInterceptor;
    private ReaderInterceptor userInterceptor;

    private DefaultReaderInterceptorContext readerInterceptorContext;

    @Before
    public void setUp() throws Exception {
        providers = mock(DefaultProviderBinder.class);
        properties = mock(ConfigurationProperties.class);
        userInterceptor = mock(ReaderInterceptor.class);
        endpointInterceptor = mock(ReaderInterceptor.class);
        when(providers.getReaderInterceptors()).thenReturn(newArrayList(userInterceptor));
        readerInterceptorContext = aReaderInterceptorContext(providers, properties, endpointInterceptor)
                .withMediaType(TEXT_PLAIN_TYPE)
                .withType(String.class)
                .withGenericType(String.class)
                .withAnnotations(new Annotation[0])
                .withEntityStream(mock(InputStream.class))
                .withHeaders(new MultivaluedHashMap<>())
                .build();
    }

    @Test
    public void invokesUsersReaderInterceptor() throws Exception {
        readerInterceptorContext.proceed();
        verify(userInterceptor).aroundReadFrom(readerInterceptorContext);
    }

    @Test
    public void invokesEndpointReaderInterceptor() throws Exception {
        when(userInterceptor.aroundReadFrom(readerInterceptorContext)).then(invocation -> readerInterceptorContext.proceed());
        readerInterceptorContext.proceed();
        verify(endpointInterceptor).aroundReadFrom(readerInterceptorContext);
    }

    @Test
    public void setsInputStream() {
        InputStream entityStream = mock(InputStream.class);
        readerInterceptorContext.setInputStream(entityStream);
        assertEquals(entityStream, readerInterceptorContext.getInputStream());
    }

    @Test
    public void setsMediaType() {
        readerInterceptorContext.setMediaType(APPLICATION_XML_TYPE);
        assertEquals(APPLICATION_XML_TYPE, readerInterceptorContext.getMediaType());
    }

    @Test
    public void setsType() {
        readerInterceptorContext.setType(DOMSource.class);
        assertEquals(DOMSource.class, readerInterceptorContext.getType());
    }

    @Test
    public void setsGenericType() {
        readerInterceptorContext.setGenericType(DOMSource.class);
        assertEquals(DOMSource.class, readerInterceptorContext.getGenericType());
    }

    @Test
    public void setsAnnotations() {
        Annotation[] annotations = {new B2Impl()};
        readerInterceptorContext.setAnnotations(annotations);
        assertArrayEquals(annotations, readerInterceptorContext.getAnnotations());
    }

    @Test(expected = NullPointerException.class)
    public void throwsExceptionWhenSetNullAnnotations() {
        readerInterceptorContext.setAnnotations(null);
    }

    @Test
    public void setsProperty() {
        readerInterceptorContext.setProperty("name", "value");
        verify(properties).setProperty("name", "value");
    }

    @Test
    public void getsProperty() {
        when(properties.getProperty("name")).thenReturn("value");
        assertEquals("value", readerInterceptorContext.getProperty("name"));
    }

    @Test
    public void getsPropertyNames() {
        when(properties.getPropertyNames()).thenReturn(newArrayList("name1", "name2"));
        assertEquals(newArrayList("name1", "name2"), readerInterceptorContext.getPropertyNames());
    }

    @Test
    public void removesProperty() {
        readerInterceptorContext.removeProperty("name");
        verify(properties).removeProperty("name");
    }

    @Test
    public void updatesHeaders() {
        readerInterceptorContext.getHeaders().putSingle(CONTENT_LENGTH, "99");
        assertEquals("99", readerInterceptorContext.getHeaders().getFirst(CONTENT_LENGTH));
    }

    @interface B2 {
    }

    static class B2Impl implements B2 {
        @Override
        public Class<? extends Annotation> annotationType() {
            return B2.class;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof B2;
        }
    }
}