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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import org.everrest.core.ConfigurationProperties;
import org.everrest.core.ExtMultivaluedMap;
import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.header.HeaderHelper;
import org.everrest.core.impl.provider.DefaultReaderInterceptorContext;
import org.everrest.core.util.CaselessMultivaluedMap;
import org.everrest.core.util.CaselessStringWrapper;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.io.ByteStreams.toByteArray;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.ACCEPT_ENCODING;
import static javax.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;
import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.HttpHeaders.CONTENT_ENCODING;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LANGUAGE;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LOCATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.DATE;
import static javax.ws.rs.core.HttpHeaders.ETAG;
import static javax.ws.rs.core.HttpHeaders.EXPIRES;
import static javax.ws.rs.core.HttpHeaders.LAST_MODIFIED;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.HttpHeaders.SET_COOKIE;
import static javax.ws.rs.core.HttpHeaders.VARY;
import static org.everrest.core.impl.header.HeaderHelper.convertToString;
import static org.everrest.core.impl.header.HeaderHelper.getContentLength;
import static org.everrest.core.impl.header.HeaderHelper.getFistHeader;
import static org.everrest.core.impl.header.HeaderHelper.getHeader;
import static org.everrest.core.impl.header.HeaderHelper.getHeaderAsStrings;
import static org.everrest.core.impl.header.HeaderHelper.getHeadersView;
import static org.everrest.core.impl.provider.DefaultReaderInterceptorContext.aReaderInterceptorContext;
import static org.everrest.core.impl.provider.IOHelper.isEmpty;

/**
 * @author andrew00x
 */
public final class ResponseImpl extends Response {
    private final StatusType status;
    private final Annotation[] entityAnnotations;
    private final MultivaluedMap<String, Object> headers;
    private final ProviderBinder providers;
    private final ConfigurationProperties properties;
    private Object entity;
    private InputStream entityStream;
    private boolean entityStreamBuffered;
    private boolean closed;

    /**
     * Construct Response with supplied status, entity and headers.
     *
     * @param status  HTTP status
     * @param entity  an entity
     * @param headers HTTP headers
     */
    public ResponseImpl(int status, Object entity, Annotation[] entityAnnotations, MultivaluedMap<String, Object> headers) {
        this.status = aStatus(status);
        this.entity = entity;
        this.entityAnnotations = entityAnnotations;
        this.headers = headers == null ? new CaselessMultivaluedMap<>() : headers;
        providers = null;
        properties = null;
    }

    public ResponseImpl(int status, InputStream entityStream, Annotation[] entityAnnotations, MultivaluedMap<String, Object> headers, ProviderBinder providers, ConfigurationProperties properties) {
        this.status = aStatus(status);
        this.entityStream = entityStream;
        this.entityAnnotations = entityAnnotations;
        this.headers = headers == null ? new CaselessMultivaluedMap<>() : headers;
        this.providers = providers;
        this.properties = properties;
    }

    @VisibleForTesting
    Annotation[] getEntityAnnotations() {
        return entityAnnotations;
    }

    @VisibleForTesting
    InputStream getEntityStream() {
        return entityStream;
    }

    @VisibleForTesting
    boolean isEntityStreamBuffered() {
        return entityStreamBuffered;
    }

    @Override
    public Object getEntity() {
        checkState(isNotClosed(), "Response already closed");
        return entity;
    }

    @Override
    public <T> T readEntity(Class<T> entityType) {
        checkState(isNotClosed(), "Response already closed");
        return readEntity(entityType, entityType, entityAnnotations);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readEntity(GenericType<T> entityType) {
        checkState(isNotClosed(), "Response already closed");
        return readEntity((Class<T>) entityType.getRawType(), entityType.getType(), entityAnnotations);
    }

    @Override
    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
        checkState(isNotClosed(), "Response already closed");
        return readEntity(entityType, entityType, annotations);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
        checkState(isNotClosed(), "Response already closed");
        return readEntity((Class<T>) entityType.getRawType(), entityType.getType(), annotations);
    }

    private <T> T readEntity(Class<T> entityType, Type genericType, Annotation[] annotations) {
        if (entityStreamBuffered) {
            try {
                entityStream.reset();
            } catch (IOException e) {
                throw new ProcessingException(e.getMessage(), e);
            }
        }
        if (isEmpty(entityStream)) {
            throw new IllegalStateException("Response does not have entity stream or has already been consumed without buffering");
        }
        DefaultReaderInterceptorContext readerInterceptorContext = aReaderInterceptorContext(providers, properties)
                .withType(entityType)
                .withGenericType(genericType)
                .withAnnotations(annotations)
                .withMediaType(getMediaType())
                .withHeaders(getStringHeaders())
                .withEntityStream(entityStream)
                .build();
        try {
            entity = readerInterceptorContext.proceed();
        } catch (IOException e) {
            throw new ProcessingException(e.getMessage(), e);
        }
        return entityType.cast(entity);
    }

    @Override
    public boolean hasEntity() {
        checkState(isNotClosed(), "Response already closed");
        return entity != null;
    }

    @Override
    public boolean bufferEntity() {
        checkState(isNotClosed(), "Response already closed");
        if (entityStreamBuffered) {
            return true;
        }
        if (!isEmpty(entityStream)) {
            InputStream buffered;
            try (InputStream in = entityStream) {
                buffered = new ByteArrayInputStream(toByteArray(in));
            } catch (IOException e) {
                throw new ProcessingException(e.getMessage(), e);
            }
            entityStreamBuffered = true;
            entityStream = buffered;
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        this.closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    private boolean isNotClosed() {
        return !closed;
    }

    @Override
    public MediaType getMediaType() {
        return getFistHeader(CONTENT_TYPE, getHeaders(), MediaType.class);
    }

    @Override
    public Locale getLanguage() {
        return getFistHeader(CONTENT_LANGUAGE, getHeaders(), Locale.class);
    }

    @Override
    public int getLength() {
        return getContentLength(getHeaders());
    }

    @Override
    public Set<String> getAllowedMethods() {
        return getHeaderAsStrings(getHeaders().get(ALLOW)).stream().map(String::toUpperCase).collect(toSet());
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return getHeader(SET_COOKIE, getHeaders(), NewCookie.class).stream()
                .collect(toMap(NewCookie::getName, identity()));
    }

    @Override
    public EntityTag getEntityTag() {
        return getFistHeader(ETAG, getHeaders(), EntityTag.class);
    }

    @Override
    public Date getDate() {
        return getFistHeader(DATE, getHeaders(), Date.class);
    }

    @Override
    public Date getLastModified() {
        return getFistHeader(LAST_MODIFIED, getHeaders(), Date.class);
    }

    @Override
    public URI getLocation() {
        return getFistHeader(LOCATION, getHeaders(), URI.class);
    }

    @Override
    public Set<Link> getLinks() {
        return getHeader(LINK, getHeaders(), Link.class).stream().collect(toSet());
    }

    @Override
    public boolean hasLink(String relation) {
        return findLink(relation).isPresent();
    }

    @Override
    public Link getLink(String relation) {
        return findLink(relation).orElse(null);
    }

    @Override
    public Link.Builder getLinkBuilder(String relation) {
        return findLink(relation).map(Link::fromLink).orElse(null);
    }

    private Optional<Link> findLink(String relation) {
        return getLinks().stream()
                .filter(link -> link.getRels().contains(relation))
                .findFirst();
    }

    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        return headers;
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return getHeadersView(getHeaders(), HeaderHelper::getHeaderAsStrings);
    }

    @Override
    public String getHeaderString(String name) {
        return convertToString(getHeaders().get(name));
    }

    @Override
    public int getStatus() {
        return status.getStatusCode();
    }

    @Override
    public StatusType getStatusInfo() {
        return status;
    }

    public static StatusType aStatus(int code) {
        final Status statusInstance = Status.fromStatusCode(code);
        if (statusInstance != null) {
            return statusInstance;
        }
        return new StatusType() {
            @Override
            public int getStatusCode() {
                return code;
            }

            @Override
            public Status.Family getFamily() {
                return Status.Family.familyOf(code);
            }

            @Override
            public String getReasonPhrase() {
                return "Unknown";
            }
        };
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("Status", status)
                          .add("Content type", getMediaType())
                          .add("Entity type", entity == null ? null : entity.getClass())
                          .omitNullValues()
                          .toString();
    }

    /** @see ResponseBuilder */
    public static final class ResponseBuilderImpl extends ResponseBuilder {

        /** HTTP headers which can't be multivalued. */
        static final Set<CaselessStringWrapper> SINGLE_VALUE_HEADERS =
                newHashSet(new CaselessStringWrapper(CACHE_CONTROL),
                           new CaselessStringWrapper(CONTENT_LANGUAGE),
                           new CaselessStringWrapper(CONTENT_LOCATION),
                           new CaselessStringWrapper(CONTENT_TYPE),
                           new CaselessStringWrapper(CONTENT_LENGTH),
                           new CaselessStringWrapper(ETAG),
                           new CaselessStringWrapper(LAST_MODIFIED),
                           new CaselessStringWrapper(LOCATION),
                           new CaselessStringWrapper(EXPIRES));

        /** Default HTTP status, No-content, 204. */
        private static final int DEFAULT_HTTP_STATUS = Response.Status.NO_CONTENT.getStatusCode();

        /** Default HTTP status. */
        private int status = DEFAULT_HTTP_STATUS;

        /** Entity. Entity will be written as response message body. */
        private Object entity;

        private Annotation[] entityAnnotations;

        /** HTTP headers. */
        private final ExtMultivaluedMap<String, Object> headers = new CaselessMultivaluedMap<>();

        /** HTTP cookies, Set-Cookie header. */
        private final Map<String, NewCookie> cookies = new HashMap<>();

        /** See {@link ResponseBuilder}. */
        ResponseBuilderImpl() {
        }

        /**
         * Useful for clone method.
         *
         * @param other
         *         other ResponseBuilderImpl
         * @see #clone()
         */
        private ResponseBuilderImpl(ResponseBuilderImpl other) {
            this.status = other.status;
            this.entity = other.entity;
            this.headers.putAll(other.headers);
            this.cookies.putAll(other.cookies);
            if (other.entityAnnotations != null) {
                this.entityAnnotations = new Annotation[other.entityAnnotations.length];
                System.arraycopy(other.entityAnnotations, 0, this.entityAnnotations, 0, this.entityAnnotations.length);
            }
        }


        @Override
        public Response build() {
            MultivaluedMap<String, Object> httpHeaders = new CaselessMultivaluedMap<>(headers);
            if (!cookies.isEmpty()) {
                for (NewCookie c : cookies.values()) {
                    httpHeaders.add(SET_COOKIE, c);
                }
            }
            Response response = new ResponseImpl(status, entity, entityAnnotations, httpHeaders);
            reset();
            return response;
        }

        /** Set ResponseBuilder to default state. */
        private void reset() {
            status = DEFAULT_HTTP_STATUS;
            entity = null;
            entityAnnotations = null;
            headers.clear();
            cookies.clear();
        }


        @Override
        public ResponseBuilder cacheControl(CacheControl cacheControl) {
            if (cacheControl == null) {
                headers.remove(CACHE_CONTROL);
            } else {
                headers.putSingle(CACHE_CONTROL, cacheControl);
            }
            return this;
        }

        @Override
        public ResponseBuilder encoding(String encoding) {
            if (encoding == null) {
                headers.remove(CONTENT_ENCODING);
            } else {
                headers.putSingle(CONTENT_ENCODING, encoding);
            }
            return this;
        }

        @Override
        public ResponseBuilder clone() {
            return new ResponseBuilderImpl(this);
        }

        @Override
        public ResponseBuilder contentLocation(URI location) {
            if (location == null) {
                headers.remove(CONTENT_LOCATION);
            } else {
                headers.putSingle(CONTENT_LOCATION, location);
            }
            return this;
        }

        @Override
        public ResponseBuilder cookie(NewCookie... cookies) {
            if (cookies == null) {
                this.cookies.clear();
                this.headers.remove(SET_COOKIE);
            } else {
                for (NewCookie cookie : cookies) {
                    this.cookies.put(cookie.getName(), cookie);
                }
            }
            return this;
        }

        @Override
        public ResponseBuilder entity(Object entity) {
            this.entity = entity;
            return this;
        }

        @Override
        public ResponseBuilder entity(Object entity, Annotation[] annotations) {
            this.entity = entity;
            this.entityAnnotations = annotations;
            return this;
        }

        @Override
        public ResponseBuilder allow(String... methods) {
            if (methods == null) {
                headers.remove(ALLOW);
            } else {
                headers.addAll(ALLOW, methods);
            }
            return this;
        }

        @Override
        public ResponseBuilder allow(Set<String> methods) {
            if (methods == null) {
                headers.remove(ALLOW);
            } else {
                headers.getList(ALLOW).addAll(methods);
            }
            return this;
        }


        @Override
        public ResponseBuilder expires(Date expires) {
            if (expires == null) {
                headers.remove(EXPIRES);
            } else {
                headers.putSingle(EXPIRES, expires);
            }
            return this;
        }


        @Override
        public ResponseBuilder header(String name, Object value) {
            if (value == null) {
                headers.remove(name);
            } else {
                if (SINGLE_VALUE_HEADERS.contains(new CaselessStringWrapper(name))) {
                    headers.putSingle(name, value);
                } else {
                    headers.add(name, value);
                }
            }
            return this;
        }

        @Override
        public ResponseBuilder replaceAll(MultivaluedMap<String, Object> headers) {
            this.headers.clear();
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }

        @Override
        public ResponseBuilder language(String language) {
            if (language == null) {
                headers.remove(CONTENT_LANGUAGE);
            } else {
                headers.putSingle(CONTENT_LANGUAGE, language);
            }
            return this;
        }

        @Override
        public ResponseBuilder language(Locale language) {
            if (language == null) {
                headers.remove(CONTENT_LANGUAGE);
            } else {
                headers.putSingle(CONTENT_LANGUAGE, language);
            }
            return this;
        }

        @Override
        public ResponseBuilder lastModified(Date lastModified) {
            if (lastModified == null) {
                headers.remove(LAST_MODIFIED);
            } else {
                headers.putSingle(LAST_MODIFIED, lastModified);
            }
            return this;
        }

        @Override
        public ResponseBuilder location(URI location) {
            if (location == null) {
                headers.remove(LOCATION);
            } else {
                headers.putSingle(LOCATION, location);
            }
            return this;
        }

        @Override
        public ResponseBuilder status(int status) {
            this.status = status;
            return this;
        }

        @Override
        public ResponseBuilder tag(EntityTag tag) {
            if (tag == null) {
                headers.remove(ETAG);
            } else {
                headers.putSingle(ETAG, tag);
            }
            return this;
        }

        @Override
        public ResponseBuilder tag(String tag) {
            if (tag == null) {
                headers.remove(ETAG);
            } else {
                headers.putSingle(ETAG, tag);
            }
            return this;
        }

        @Override
        public ResponseBuilder type(MediaType type) {
            if (type == null) {
                headers.remove(CONTENT_TYPE);
            } else {
                headers.putSingle(CONTENT_TYPE, type);
            }
            return this;
        }

        @Override
        public ResponseBuilder type(String type) {
            if (type == null) {
                headers.remove(CONTENT_TYPE);
            } else {
                headers.putSingle(CONTENT_TYPE, type);
            }
            return this;
        }

        @Override
        public ResponseBuilder variant(Variant variant) {
            if (variant == null) {
                type((String)null);
                language((String)null);
                encoding(null);
            } else {
                type(variant.getMediaType());
                language(variant.getLanguage());
                encoding(variant.getEncoding());
            }
            return this;
        }

        @Override
        public ResponseBuilder variants(Variant... variants) {
            return variants(variants == null ? null : Arrays.asList(variants));
        }

        @Override
        public ResponseBuilder variants(List<Variant> variants) {
            if (variants == null) {
                headers.remove(VARY);
                return this;
            }
            if (variants.isEmpty()) {
                return this;
            }

            boolean acceptMediaType = variants.get(0).getMediaType() != null;
            boolean acceptLanguage = variants.get(0).getLanguage() != null;
            boolean acceptEncoding = variants.get(0).getEncoding() != null;

            for (Variant variant : variants) {
                acceptMediaType |= variant.getMediaType() != null;
                acceptLanguage |= variant.getLanguage() != null;
                acceptEncoding |= variant.getEncoding() != null;
            }

            List<String> varyHeader = new ArrayList<>();
            if (acceptMediaType) {
                varyHeader.add(ACCEPT);
            }
            if (acceptLanguage) {
                varyHeader.add(ACCEPT_LANGUAGE);
            }
            if (acceptEncoding) {
                varyHeader.add(ACCEPT_ENCODING);
            }

            if (varyHeader.size() > 0) {
                header(VARY, Joiner.on(',').join(varyHeader));
            }
            return this;
        }

        @Override
        public ResponseBuilder links(Link... links) {
            if (links == null) {
               headers.remove(LINK);
            } else {
                headers.addAll(LINK, links);
            }
            return this;
        }

        @Override
        public ResponseBuilder link(URI uri, String rel) {
            headers.getList(LINK).add(Link.fromUri(uri).rel(rel).build());
            return this;
        }

        @Override
        public ResponseBuilder link(String uri, String rel) {
            return link(URI.create(uri), rel);
        }
    }
}
