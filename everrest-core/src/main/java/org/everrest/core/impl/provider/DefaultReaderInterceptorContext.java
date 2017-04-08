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
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class DefaultReaderInterceptorContext extends BaseInterceptorContext implements ReaderInterceptorContext {
    public static class DefaultReaderInterceptorContextBuilder {
        private final ReaderInterceptor endpointInterceptor;
        private final ProviderBinder providers;
        private final ConfigurationProperties properties;
        private Class type;
        private Type genericType;
        private Annotation[] annotations;
        private MediaType mediaType;
        private MultivaluedMap<String, String> headers;
        private InputStream entityStream;

        private DefaultReaderInterceptorContextBuilder(ProviderBinder providers, ConfigurationProperties properties, ReaderInterceptor endpointInterceptor) {
            this.providers = requireNonNull(providers);
            this.properties = requireNonNull(properties);
            this.endpointInterceptor = requireNonNull(endpointInterceptor);
        }

        public DefaultReaderInterceptorContextBuilder withType(Class type) {
            this.type = type;
            return this;
        }

        public DefaultReaderInterceptorContextBuilder withGenericType(Type genericType) {
            this.genericType = genericType;
            return this;
        }

        public DefaultReaderInterceptorContextBuilder withAnnotations(Annotation[] annotations) {
            this.annotations = annotations;
            return this;
        }

        public DefaultReaderInterceptorContextBuilder withMediaType(MediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public DefaultReaderInterceptorContextBuilder withHeaders(MultivaluedMap<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public DefaultReaderInterceptorContextBuilder withEntityStream(InputStream entityStream) {
            this.entityStream = entityStream;
            return this;
        }

        public DefaultReaderInterceptorContext build() {
            return new DefaultReaderInterceptorContext(type, genericType, annotations, mediaType, headers, entityStream, createInterceptorsChain(), properties);
        }

        private Iterator<ReaderInterceptor> createInterceptorsChain() {
            List<ReaderInterceptor> interceptors = new ArrayList<>(providers.getReaderInterceptors());
            interceptors.add(endpointInterceptor);
            return interceptors.iterator();
        }
    }

    public static DefaultReaderInterceptorContextBuilder aReaderInterceptorContext(ProviderBinder providers, ConfigurationProperties properties, ReaderInterceptor endpointInterceptor) {
        return new DefaultReaderInterceptorContextBuilder(providers, properties, endpointInterceptor);
    }

    public static DefaultReaderInterceptorContextBuilder aReaderInterceptorContext(ProviderBinder providers, ConfigurationProperties properties) {
        return new DefaultReaderInterceptorContextBuilder(providers, properties, new EndpointReaderInterceptor(providers));
    }

    private final MultivaluedMap<String, String> headers;
    private final Iterator<ReaderInterceptor> interceptors;
    private InputStream entityStream;

    private DefaultReaderInterceptorContext(Class<?> type,
                                            Type genericType,
                                            Annotation[] annotations,
                                            MediaType mediaType,
                                            MultivaluedMap<String, String> headers,
                                            InputStream entityStream,
                                            Iterator<ReaderInterceptor> interceptors,
                                            ConfigurationProperties properties) {
        super(type, genericType, annotations, mediaType, properties);
        this.headers = headers;
        this.entityStream = entityStream;
        this.interceptors = interceptors;
    }

    @Override
    public Object proceed() throws IOException, WebApplicationException {
        checkState(interceptors.hasNext());
        return interceptors.next().aroundReadFrom(this);
    }

    @Override
    public InputStream getInputStream() {
        return entityStream;
    }

    @Override
    public void setInputStream(InputStream entityStream) {
        this.entityStream = entityStream;
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }
}
