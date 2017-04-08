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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class DefaultWriterInterceptorContext extends BaseInterceptorContext implements WriterInterceptorContext {
    public static class DefaultWriterInterceptorContextBuilder {
        private final ProviderBinder providers;
        private final WriterInterceptor endpointInterceptor;
        private final ConfigurationProperties properties;
        private Object entity;
        private Class type;
        private Type genericType;
        private Annotation[] annotations;
        private MediaType mediaType;
        private MultivaluedMap<String, Object> headers;
        private OutputStream entityStream;

        private DefaultWriterInterceptorContextBuilder(ProviderBinder providers, ConfigurationProperties properties, WriterInterceptor endpointInterceptor) {
            this.providers = requireNonNull(providers);
            this.properties = requireNonNull(properties);
            this.endpointInterceptor = requireNonNull(endpointInterceptor);
        }

        public DefaultWriterInterceptorContextBuilder withType(Class type) {
            this.type = type;
            return this;
        }

        public DefaultWriterInterceptorContextBuilder withGenericType(Type genericType) {
            this.genericType = genericType;
            return this;
        }

        public DefaultWriterInterceptorContextBuilder withAnnotations(Annotation[] annotations) {
            this.annotations = annotations;
            return this;
        }

        public DefaultWriterInterceptorContextBuilder withMediaType(MediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public DefaultWriterInterceptorContextBuilder withHeaders(MultivaluedMap<String, Object> headers) {
            this.headers = headers;
            return this;
        }

        public DefaultWriterInterceptorContextBuilder withEntityStream(OutputStream entityStream) {
            this.entityStream = entityStream;
            return this;
        }

        public DefaultWriterInterceptorContextBuilder withEntity(Object entity) {
            this.entity = entity;
            return this;
        }

        public DefaultWriterInterceptorContext build() {
            return new DefaultWriterInterceptorContext(entity, type, genericType, annotations, mediaType, headers, entityStream, createInterceptorsChain(), properties);
        }

        private Iterator<WriterInterceptor> createInterceptorsChain() {
            List<WriterInterceptor> interceptors = new ArrayList<>(providers.getWriterInterceptors());
            interceptors.add(endpointInterceptor);
            return interceptors.iterator();
        }
    }

    public static DefaultWriterInterceptorContextBuilder aWriterInterceptorContext(ProviderBinder providers, ConfigurationProperties properties, WriterInterceptor endpointInterceptor) {
        return new DefaultWriterInterceptorContextBuilder(providers, properties, endpointInterceptor);
    }

    public static DefaultWriterInterceptorContextBuilder aWriterInterceptorContext(ProviderBinder providers, ConfigurationProperties properties) {
        return aWriterInterceptorContext(providers, properties, new EndpointWriterInterceptor(providers));
    }

    private final MultivaluedMap<String, Object> headers;
    private final Iterator<WriterInterceptor> interceptors;
    private Object entity;
    private OutputStream entityStream;

    private DefaultWriterInterceptorContext(Object entity,
                                            Class<?> type,
                                            Type genericType,
                                            Annotation[] annotations,
                                            MediaType mediaType,
                                            MultivaluedMap<String, Object> headers,
                                            OutputStream entityStream,
                                            Iterator<WriterInterceptor> interceptors,
                                            ConfigurationProperties properties) {
        super(type, genericType, annotations, mediaType, properties);
        this.entity = entity;
        this.entityStream = entityStream;
        this.headers = headers;
        this.interceptors = interceptors;
    }

    @Override
    public void proceed() throws IOException, WebApplicationException {
        checkState(interceptors.hasNext());
        interceptors.next().aroundWriteTo(this);
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public void setEntity(Object entity) {
        this.entity = entity;
    }

    @Override
    public OutputStream getOutputStream() {
        return entityStream;
    }

    @Override
    public void setOutputStream(OutputStream entityStream) {
        this.entityStream = entityStream;
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return headers;
    }
}
