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

import org.everrest.core.ProviderBinder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.OutputStream;
import java.lang.annotation.Annotation;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class EndpointWriterInterceptorTest {
    @Rule public ExpectedException thrown = ExpectedException.none();
    @Mock private ProviderBinder providers;
    @InjectMocks private EndpointWriterInterceptor writerInterceptor;

    @Mock private WriterInterceptorContext interceptorContext;
    @Mock private MessageBodyWriter writer;

    @Before
    public void setUp() throws Exception {
        mockInterceptorContext();
    }

    private void mockInterceptorContext() {
        when(interceptorContext.getType()).thenReturn((Class) String.class);
        when(interceptorContext.getGenericType()).thenReturn(String.class);
        when(interceptorContext.getAnnotations()).thenReturn(new Annotation[0]);
        when(interceptorContext.getMediaType()).thenReturn(TEXT_PLAIN_TYPE);
        when(interceptorContext.getEntity()).thenReturn("to be or not to be");
    }

    @Test
    public void throwsExceptionIfProviderBinderDoesNotProvideSuitableMessageBodyWriter() throws Exception {
        thrown.expect(MessageBodyWriterNotFoundException.class);
        writerInterceptor.aroundWriteTo(interceptorContext);
        verify(providers).getMessageBodyWriter(String.class, String.class, new Annotation[0], TEXT_PLAIN_TYPE);
    }

    @Test
    public void writesEntityWithMessageBodyWriterProvidedByProviderBinder() throws Exception {
        OutputStream entityStream = mock(OutputStream.class);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(interceptorContext.getOutputStream()).thenReturn(entityStream);
        when(interceptorContext.getHeaders()).thenReturn(headers);
        when(providers.getMessageBodyWriter(String.class, String.class, new Annotation[0], TEXT_PLAIN_TYPE)).thenReturn(writer);

        writerInterceptor.aroundWriteTo(interceptorContext);
        verify(writer).writeTo(eq("to be or not to be"), eq(String.class), eq(String.class), aryEq(new Annotation[0]), eq(TEXT_PLAIN_TYPE), isA(MultivaluedHashMap.class), eq(entityStream));
    }

    @Test
    public void setupsContentLengthHeaderIfMessageBodyWriterProvidesLength() throws Exception {
        OutputStream entityStream = mock(OutputStream.class);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(interceptorContext.getOutputStream()).thenReturn(entityStream);
        when(interceptorContext.getHeaders()).thenReturn(headers);
        when(providers.getMessageBodyWriter(String.class, String.class, new Annotation[0], TEXT_PLAIN_TYPE)).thenReturn(writer);

        when(writer.getSize(eq("to be or not to be"), eq(String.class), eq(String.class), aryEq(new Annotation[0]), eq(TEXT_PLAIN_TYPE)))
                .thenReturn((long) "to be or not to be".length());

        writerInterceptor.aroundWriteTo(interceptorContext);

        assertEquals(Long.valueOf("to be or not to be".length()), headers.getFirst(CONTENT_LENGTH));
    }
}