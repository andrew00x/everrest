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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import org.everrest.core.impl.provider.DefaultWriterInterceptorContext;
import org.everrest.core.util.CaselessMultivaluedMap;
import org.everrest.core.util.InputStreamWrapper;
import org.everrest.core.util.NotifierOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.EventObject;
import java.util.Iterator;
import java.util.function.Supplier;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.everrest.core.impl.provider.DefaultWriterInterceptorContext.aWriterInterceptorContext;

class InvocationPipeline {
    private final HttpURLConnectionFactory urlConnectionFactory;

    InvocationPipeline() {
        this(new HttpURLConnectionFactory());
    }

    @VisibleForTesting
    InvocationPipeline(HttpURLConnectionFactory urlConnectionFactory) {
        this.urlConnectionFactory = urlConnectionFactory;
    }

    ClientResponse execute(ClientRequest request) {
        try {
            invokeRequestFiltersChain(request);

            ClientResponse clientResponse;
            if (request.getAbortResponse() == null) {
                clientResponse = sendRequest(request);
            } else {
                clientResponse = processAbortResponse(request);
            }

            invokeResponseFiltersChain(request, clientResponse);

            return clientResponse;
        } catch (IOException e) {
            throw new ProcessingException(e.getMessage(), e);
        }
    }

    private void invokeRequestFiltersChain(ClientRequest request) throws IOException {
        Iterator<ClientRequestFilter> requestFilters = request.getProviders().getClientRequestFilters().iterator();

        while (requestFilters.hasNext() && request.getAbortResponse() == null) {
            requestFilters.next().filter(request);
        }
    }

    private void invokeResponseFiltersChain(ClientRequest request, ClientResponse clientResponse) throws IOException {
        for (ClientResponseFilter responseFilter : request.getProviders().getClientResponseFilters()) {
            responseFilter.filter(request, clientResponse);
        }
    }

    @SuppressWarnings("unchecked")
    private ClientResponse processAbortResponse(ClientRequest request) {
        Response abortResponse = request.getAbortResponse();
        ClientResponse clientResponse = new ClientResponse(abortResponse.getStatus(), new CaselessMultivaluedMap<>(abortResponse.getStringHeaders()), request.getProviders(), request.getProperties());
        Object entity = abortResponse.getEntity();
        if (entity != null) {
            Class entityClass;
            Type entityType;
            if (entity instanceof GenericEntity) {
                GenericEntity genericEntity = (GenericEntity) entity;
                entityClass = genericEntity.getRawType();
                entityType = genericEntity.getType();
                entity = genericEntity.getEntity();
            } else {
                entityClass = entity.getClass();
                entityType = entity.getClass();
            }
            MessageBodyWriter<Object> writer = request.getProviders().getMessageBodyWriter(entityClass, entityType, new Annotation[0], abortResponse.getMediaType());
            if (writer == null) {
                throw new ResponseProcessingException(abortResponse, String.format("Unsupported entity type %s. There is no any message body writer that can serialize this type to %s media type.", entityClass.getSimpleName(), abortResponse.getMediaType()));
            }
            ByteArrayOutputStream entityOutput = new ByteArrayOutputStream();
            try {
                writer.writeTo(entity, entityClass, entityType, new Annotation[0], abortResponse.getMediaType(), abortResponse.getHeaders(), entityOutput);
            } catch (IOException e) {
                throw new ResponseProcessingException(abortResponse, e);
            }
            clientResponse.setEntityStream(new ByteArrayInputStream(entityOutput.toByteArray()));
        }
        return clientResponse;
    }

    private ClientResponse sendRequest(ClientRequest request) throws IOException {
        URL url = request.getUri().toURL();
        HttpURLConnection connection = urlConnectionFactory.openConnectionTo(url);
        if (connection instanceof HttpsURLConnection) {
            SSLSocketFactory sslSocketFactory = request.getClient().getSslContext().getSocketFactory();
            ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
        }
        if (request.hasEntity()) {
            connection.setDoOutput(true);
            HttpURLConnectionOutputStreamSupplier outputStreamSupplier = new HttpURLConnectionOutputStreamSupplier(connection);
            DefaultWriterInterceptorContext writerInterceptorContext = aWriterInterceptorContext(request.getProviders(), request.getProperties())
                    .withEntityStream(new NotifierOutputStream(outputStreamSupplier, new HeadersWriter(connection, request)))
                    .withEntity(request.getEntity())
                    .withType(request.getEntityClass())
                    .withGenericType(request.getEntityType())
                    .withAnnotations(request.getEntityAnnotations())
                    .withMediaType(request.getMediaType())
                    .withHeaders(request.getHeaders())
                    .build();
            try {
                writerInterceptorContext.proceed();
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }
        int status = connection.getResponseCode();
        MultivaluedMap<String, String> responseHeaders = new CaselessMultivaluedMap<>(connection.getHeaderFields());
        ClientResponse response = new ClientResponse(status, responseHeaders, request.getProviders(), request.getProperties());
        HttpURLConnectionInputStreamSupplier inputStreamSupplier = new HttpURLConnectionInputStreamSupplier(connection);
        response.setEntityStream(new InputStreamWrapper(inputStreamSupplier));
        return response;
    }

    private static class HttpURLConnectionOutputStreamSupplier implements Supplier<OutputStream> {
        final HttpURLConnection connection;
        OutputStream output;

        HttpURLConnectionOutputStreamSupplier(HttpURLConnection connection) {
            this.connection = connection;
        }

        @Override
        public OutputStream get() {
            if (output == null) {
                try {
                    output = connection.getOutputStream();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return output;
        }
    }

    private static class HeadersWriter implements NotifierOutputStream.EntityStreamListener {
        final HttpURLConnection connection;
        final ClientRequest request;
        boolean done;

        HeadersWriter(HttpURLConnection connection, ClientRequest request) {
            this.connection = connection;
            this.request = request;
        }

        @Override
        public void onChange(EventObject event) throws IOException {
            if (done) {
                return;
            }
            writeHeaders();
            done = true;
        }

        private void writeHeaders() throws IOException {
            connection.setRequestMethod(request.getMethod());
            MultivaluedMap<String, String> requestHeaders = request.getStringHeaders();
            requestHeaders.entrySet().forEach(e -> {
                if (e.getValue().size() > 1) {
                    connection.setRequestProperty(e.getKey(), Joiner.on(',').join(e.getValue()));
                } else if(!e.getValue().isEmpty()) {
                    connection.setRequestProperty(e.getKey(), e.getValue().get(0));
                }
            });
        }
    }

    private static class HttpURLConnectionInputStreamSupplier implements Supplier<InputStream> {
        final HttpURLConnection connection;
        InputStream input;

        HttpURLConnectionInputStreamSupplier(HttpURLConnection connection) {
            this.connection = connection;
        }

        @Override
        public InputStream get() {
            if (this.input == null) {
                try {
                    InputStream input;
                    if (connection.getResponseCode() >= BAD_REQUEST.getStatusCode()) {
                        input = connection.getErrorStream();
                        if (input == null) {
                            input = new ByteArrayInputStream(new byte[0]);
                        }
                    } else {
                        input = connection.getInputStream();
                    }
                    this.input = new FilterInputStream(input) {
                        @Override
                        public void close() throws IOException {
                            super.close();
                            connection.disconnect();
                        }
                    };
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return this.input;
        }
    }
}
