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
package org.everrest.core.impl.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import org.everrest.core.ConfigurationProperties;
import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.provider.StringEntityProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.everrest.core.$matchers.ExceptionMatchers.exceptionSameInstance;
import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InvocationPipelineTest {
    @Rule public ExpectedException thrown = ExpectedException.none();

    @Mock private HttpURLConnectionFactory connectionFactory;
    @InjectMocks private InvocationPipeline pipeline;

    private URL url;
    private MultivaluedMap<String, Object> requestHeaders;
    private MultivaluedMap<String, String> requestStringHeaders;
    private String entity = "to be or not to be";
    private MediaType requestMediaType = new MediaType("text", "plain");

    @Mock private ClientRequest request;
    @Mock private ProviderBinder providers;
    @Mock private HttpURLConnection connection;
    @Mock private Client client;
    @Mock private MessageBodyWriter writer;

    @Before
    public void setUp() throws Exception {
        url = new URL("http://test.com/a/b");
        requestHeaders = new MultivaluedHashMap<>();
        requestStringHeaders = new MultivaluedHashMap<>();
        mockClientRequest();
        mockHttpUrlConnection();
        when(providers.getMessageBodyWriter(String.class, String.class, new Annotation[0], requestMediaType)).thenReturn(writer);
    }

    private void mockClientRequest() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getUri()).thenReturn(url.toURI());
        when(request.getHeaders()).thenReturn(requestHeaders);
        when(request.getStringHeaders()).thenReturn(requestStringHeaders);
        when(request.hasEntity()).thenReturn(true);
        when(request.getEntity()).thenReturn(entity);
        when(request.getEntityClass()).thenReturn((Class) String.class);
        when(request.getEntityType()).thenReturn(String.class);
        when(request.getMediaType()).thenReturn(requestMediaType);
        when(request.getEntityAnnotations()).thenReturn(new Annotation[0]);
        when(request.getClient()).thenReturn(client);
        when(request.getProviders()).thenReturn(providers);
        ConfigurationProperties properties = mock(ConfigurationProperties.class);
        when(request.getProperties()).thenReturn(properties);
        requestHeaders.putSingle(CONTENT_TYPE, requestMediaType);
        requestStringHeaders.putSingle(CONTENT_TYPE, requestMediaType.toString());
    }

    private void mockHttpUrlConnection() throws Exception {
        connection = mock(HttpURLConnection.class);
        when(connection.getURL()).thenReturn(url);
        when(connection.getResponseCode()).thenReturn(200);
        when(connectionFactory.openConnectionTo(url)).thenReturn(connection);
    }


    @Test
    public void invokesThroughClientRequestFilters() throws Exception {
        ClientRequestFilter filter = mock(ClientRequestFilter.class);
        when(providers.getClientRequestFilters()).thenReturn(newArrayList(filter));

        pipeline.execute(request);

        verify(filter).filter(request);
    }

    @Test
    public void stopsProcessingThroughClientRequestFiltersOnceAbortResponseIsSet() throws Exception {
        ClientRequestFilter filterOne = mock(ClientRequestFilter.class);
        ClientRequestFilter filterTwo = mock(ClientRequestFilter.class);
        when(request.getAbortResponse()).thenReturn(null, Response.status(400).build());
        when(providers.getClientRequestFilters()).thenReturn(newArrayList(filterOne, filterTwo));

        pipeline.execute(request);

        verify(filterOne).filter(request);
        verify(filterTwo, never()).filter(request);
    }

    @Test
    public void invokesThroughClientResponseFilters() throws Exception {
        ClientResponseFilter filter = mock(ClientResponseFilter.class);
        when(providers.getClientResponseFilters()).thenReturn(newArrayList(filter));

        ClientResponse response = pipeline.execute(request);

        verify(filter).filter(request, response);
    }

    @Test
    public void setsUpSSLSocketFactoryForHttpsConnection() throws Exception {
        connection = mock(HttpsURLConnection.class);
        when(connectionFactory.openConnectionTo(url)).thenReturn(connection);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, null);
        when(client.getSslContext()).thenReturn(sslContext);

        pipeline.execute(request);

        verify((HttpsURLConnection) connection).setSSLSocketFactory(isA(SSLSocketFactory.class));
    }

    @Test
    public void invokesWithRequestEntity() throws Exception {
        ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
        when(connection.getOutputStream()).thenReturn(entityStream);
        when(providers.getMessageBodyWriter(String.class, String.class, new Annotation[0], requestMediaType)).thenReturn(new StringEntityProvider());

        ClientResponse response = pipeline.execute(request);

        assertEquals(200, response.getStatus());
        assertEquals(entity, entityStream.toString());
        verify(connection).setRequestMethod("POST");
        verify(connection).setRequestProperty(CONTENT_TYPE, "text/plain");
    }

    @Test
    public void invokesThroughWriterInterceptorWhenWriteRequestEntity() throws Exception {
        WriterInterceptor writerInterceptor = mock(WriterInterceptor.class);
        when(providers.getWriterInterceptors()).thenReturn(newArrayList(writerInterceptor));

        pipeline.execute(request);

        verify(writerInterceptor).aroundWriteTo(isA(WriterInterceptorContext.class));
    }

    @Test
    public void throwsProcessingExceptionWhenNotAbleToSerializeRequestEntity() throws Exception {
        when(providers.getMessageBodyWriter(String.class, String.class, new Annotation[0], new MediaType("text", "plain"))).thenReturn(null);

        thrown.expect(ProcessingException.class);
        thrown.expectMessage("Unsupported entity type String");
        pipeline.execute(request);
    }

    @Test
    public void throwsProcessingExceptionWhenIOErrorOccurWhileSendingRequest() throws Exception {
        IOException ioError = new IOException();
        doThrow(ioError)
                .when(writer)
                .writeTo(eq(entity), eq(String.class), eq(String.class), aryEq(new Annotation[0]), eq(new MediaType("text", "plain")), isA(MultivaluedMap.class), any(OutputStream.class));

        thrown.expect(ProcessingException.class);
        thrown.expectCause(exceptionSameInstance(ioError));
        pipeline.execute(request);
    }

    @Test
    public void processesAbortResponse() throws Exception {
        ClientRequestFilter filter = mock(ClientRequestFilter.class);
        Response abortResponse = Response.status(403).entity("Forbidden").type("text/plain").build();
        when(request.getAbortResponse()).thenReturn(abortResponse);
        when(providers.getClientRequestFilters()).thenReturn(newArrayList(filter));
        when(providers.getMessageBodyWriter(String.class, String.class, new Annotation[0], new MediaType("text", "plain"))).thenReturn(new StringEntityProvider());

        ClientResponse response = pipeline.execute(request);

        assertEquals(403, response.getStatus());
        assertEquals(new MediaType("text", "plain"), response.getMediaType());
        assertEquals("Forbidden", CharStreams.toString(new InputStreamReader(response.getEntityStream())));
    }

    @Test
    public void throwsResponseProcessingExceptionWhenNotAbleToSerializeAbortResponseEntity() throws Exception {
        ClientRequestFilter filter = mock(ClientRequestFilter.class);
        Response abortResponse = Response.status(403).entity("Forbidden").type("text/plain").build();
        when(request.getAbortResponse()).thenReturn(abortResponse);
        when(providers.getClientRequestFilters()).thenReturn(newArrayList(filter));
        when(providers.getMessageBodyWriter(String.class, String.class, new Annotation[0], new MediaType("text", "plain"))).thenReturn(null);

        thrown.expect(ResponseProcessingException.class);
        thrown.expectMessage("Unsupported entity type String");
        pipeline.execute(request);
    }

    @Test
    public void throwsResponseProcessingExceptionWhenIOErrorOccurWhileSerializingEntityOfAbortResponse() throws Exception {
        ClientRequestFilter filter = mock(ClientRequestFilter.class);
        Response abortResponse = Response.status(403).entity("Forbidden").type("text/plain").build();
        when(request.getAbortResponse()).thenReturn(abortResponse);
        when(providers.getClientRequestFilters()).thenReturn(newArrayList(filter));
        IOException ioError = new IOException();
        doThrow(ioError)
                .when(writer)
                .writeTo(eq("Forbidden"), eq(String.class), eq(String.class), aryEq(new Annotation[0]), eq(new MediaType("text", "plain")), isA(MultivaluedMap.class), isA(OutputStream.class));

        thrown.expect(ResponseProcessingException.class);
        thrown.expectCause(exceptionSameInstance(ioError));
        pipeline.execute(request);
    }

    @Test
    public void invokesWithResponseEntity() throws Exception {
        when(connection.getHeaderFields()).thenReturn(ImmutableMap.of(CONTENT_TYPE, newArrayList("text/plain")));
        ByteArrayInputStream in = new ByteArrayInputStream(entity.getBytes());
        when(connection.getInputStream()).thenReturn(in);

        ClientResponse response = pipeline.execute(request);

        assertEquals(200, response.getStatus());
        assertEquals(new MediaType("text", "plain"), response.getMediaType());
        assertEquals(entity, CharStreams.toString(new InputStreamReader(response.getEntityStream())));
    }

    @Test
    public void invokesWithErrorResponse() throws Exception {
        when(connection.getHeaderFields()).thenReturn(ImmutableMap.of(CONTENT_TYPE, newArrayList("text/plain")));
        when(connection.getResponseCode()).thenReturn(400);
        ByteArrayInputStream in = new ByteArrayInputStream("Failed".getBytes());
        when(connection.getErrorStream()).thenReturn(in);

        ClientResponse response = pipeline.execute(request);

        assertEquals(400, response.getStatus());
        assertEquals(new MediaType("text", "plain"), response.getMediaType());
        assertEquals("Failed", CharStreams.toString(new InputStreamReader(response.getEntityStream())));
    }
}