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
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.InputStream;
import java.lang.annotation.Annotation;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class EndpointReaderInterceptorTest {
    @Rule public ExpectedException thrown = ExpectedException.none();
    @Mock private ProviderBinder providers;
    @InjectMocks private EndpointReaderInterceptor readerInterceptor;

    @Mock private ReaderInterceptorContext interceptorContext;
    @Mock private MessageBodyReader reader;

    @Before
    public void setUp() throws Exception {
        mockInterceptorContext();
    }

    private void mockInterceptorContext() {
        when(interceptorContext.getType()).thenReturn((Class) String.class);
        when(interceptorContext.getGenericType()).thenReturn(String.class);
        when(interceptorContext.getAnnotations()).thenReturn(new Annotation[0]);
        when(interceptorContext.getMediaType()).thenReturn(TEXT_PLAIN_TYPE);
    }

    @Test
    public void throwsExceptionIfProviderBinderDoesNotProvideSuitableMessageBodyReader() throws Exception {
        thrown.expect(MessageBodyReaderNotFoundException.class);
        readerInterceptor.aroundReadFrom(interceptorContext);
        verify(providers).getMessageBodyReader(String.class, String.class, new Annotation[0], TEXT_PLAIN_TYPE);
    }

    @Test
    public void readsEntityWithMessageBodyReaderProvidedByProviderBinder() throws Exception {
        InputStream entityStream = mock(InputStream.class);
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        when(interceptorContext.getInputStream()).thenReturn(entityStream);
        when(interceptorContext.getHeaders()).thenReturn(headers);
        when(providers.getMessageBodyReader(String.class, String.class, new Annotation[0], TEXT_PLAIN_TYPE)).thenReturn(reader);
        when(reader.readFrom(String.class, String.class, new Annotation[0], TEXT_PLAIN_TYPE, headers, entityStream)).thenReturn("to be or not to be");

        assertEquals("to be or not to be", readerInterceptor.aroundReadFrom(interceptorContext));
    }

    @Test
    public void closesEntityStreamAfterReadingEntity() throws Exception {
        InputStream entityStream = mock(InputStream.class);
        when(interceptorContext.getInputStream()).thenReturn(entityStream);
        when(providers.getMessageBodyReader(String.class, String.class, new Annotation[0], TEXT_PLAIN_TYPE)).thenReturn(reader);

        readerInterceptor.aroundReadFrom(interceptorContext);

        verify(entityStream).close();
    }

    @Test
    public void doesNotCloseEntityStreamIfResultOfReadingEntityIsOriginalEntityStream() throws Exception {
        InputStream entityStream = mock(InputStream.class);
        when(interceptorContext.getInputStream()).thenReturn(entityStream);
        when(providers.getMessageBodyReader(String.class, String.class, new Annotation[0], TEXT_PLAIN_TYPE)).thenReturn(reader);
        when(reader.readFrom(String.class, String.class, new Annotation[0], TEXT_PLAIN_TYPE, null, entityStream)).thenReturn(entityStream);

        assertEquals(entityStream, readerInterceptor.aroundReadFrom(interceptorContext));

        verify(entityStream, never()).close();
    }
}