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

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import org.everrest.core.ContainerResponseWriter;
import org.everrest.core.GenericContainerResponse;
import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.header.HeaderHelper;
import org.everrest.core.impl.provider.MessageBodyWriterNotFoundException;
import org.everrest.core.impl.provider.StringEntityProvider;
import org.everrest.core.util.CaselessMultivaluedMap;
import org.everrest.core.util.NotifierOutputStream;
import org.everrest.core.util.NotifierOutputStream.EntityStreamListener;
import org.everrest.core.util.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Date;
import java.util.EventObject;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LANGUAGE;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.DATE;
import static javax.ws.rs.core.HttpHeaders.ETAG;
import static javax.ws.rs.core.HttpHeaders.LAST_MODIFIED;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.HttpHeaders.SET_COOKIE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static org.everrest.core.impl.ResponseImpl.aStatus;
import static org.everrest.core.impl.header.HeaderHelper.convertToString;
import static org.everrest.core.impl.header.HeaderHelper.getContentLength;
import static org.everrest.core.impl.header.HeaderHelper.getFistHeader;
import static org.everrest.core.impl.header.HeaderHelper.getHeader;
import static org.everrest.core.impl.header.HeaderHelper.getHeaderAsStrings;
import static org.everrest.core.impl.header.HeaderHelper.getHeadersView;
import static org.everrest.core.impl.provider.DefaultWriterInterceptorContext.aWriterInterceptorContext;

/**
 * @author andrew00x
 */
public class ContainerResponse implements GenericContainerResponse {
    private static final Logger LOG = LoggerFactory.getLogger(ContainerResponse.class);

    /** HTTP status. */
    private StatusType status;
    /** Entity type. */
    private Type entityType;
    /** Entity. */
    private Object entity;
    /** Annotations attached to the entity. */
    private Annotation[] entityAnnotations = new Annotation[0];
    /** HTTP response headers. */
    private MultivaluedMap<String, Object> headers;
    /** See {@link ContainerResponseWriter}. */
    private ContainerResponseWriter responseWriter;
    private OutputStream entityStream;

    public ContainerResponse(ContainerResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void setResponse(Response response) {
        if (response == null) {
            status = null;
            entity = null;
            entityType = null;
            headers = null;
        } else {
            status = response.getStatusInfo();
            headers = response.getHeaders();
            setEntity(response.getEntity());
        }
    }

    @Override
    public Response getResponse() {
        if (status == null) {
            return null;
        }
        return new ResponseImpl(getStatus(), getEntity(), getEntityAnnotations(), getHeaders());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeResponse() throws IOException {
        if (entity == null) {
            writeResponseWithoutEntity();
            return;
        }

        ApplicationContext context = ApplicationContext.getCurrent();
        ProviderBinder providers = context.getProviders();
        MediaType contentType = getMediaType();

        if (isNullOrWildcard(contentType)) {
            List<MediaType> acceptableWriterMediaTypes = providers.getAcceptableWriterMediaTypes(getEntityClass(), entityType, entityAnnotations);
            if (isEmptyOrContainsSingleWildcardMediaType(acceptableWriterMediaTypes)) {
                contentType = context.getContainerRequest().getAcceptableMediaTypes().get(0);
            } else {
                contentType = context.getContainerRequest().getAcceptableMediaType(acceptableWriterMediaTypes);
            }

            if (isNullOrWildcard(contentType)) {
                contentType = APPLICATION_OCTET_STREAM_TYPE;
            }

            getHeaders().putSingle(CONTENT_TYPE, contentType);
        }

        OutputStream entityStream = getEntityStream();
        if (entityStream == null) {
            entityStream = responseWriter.getOutputStream();
        }

        WriterInterceptorContext writerInterceptorContext = aWriterInterceptorContext(providers, context.getContainerRequest().getProperties())
                .withEntityStream(new NotifierOutputStream(entityStream, new HeadersWriter()))
                .withEntity(entity)
                .withType(getEntityClass())
                .withGenericType(entityType)
                .withAnnotations(entityAnnotations)
                .withMediaType(getMediaType())
                .withHeaders(getHeaders())
                .build();
        try {
            writerInterceptorContext.proceed();
        } catch (MessageBodyWriterNotFoundException e) {
            if (HEAD.equals(context.getContainerRequest().getMethod())) {
                getHeaders().putSingle(CONTENT_LENGTH, -1);
                writeResponseWithoutEntity();
            } else {
                LOG.warn(e.getMessage());
                setResponse(Response.status(NOT_ACCEPTABLE)
                                    .entity(e.getMessage())
                                    .type(TEXT_PLAIN)
                                    .build());
                writeResponseWithEntity(new StringEntityProvider());
            }
        } catch (Exception e) {
            if (Throwables.getCausalChain(e).stream().anyMatch(throwable -> "org.apache.catalina.connector.ClientAbortException".equals(throwable.getClass().getName()))) {
                LOG.warn("Client has aborted connection. Response writing omitted");
            } else {
                throw e;
            }
        }
    }

    private void writeResponseWithoutEntity() throws IOException {
        if (Tracer.isTracingEnabled()) {
            Tracer.addTraceHeaders(this);
        }
        responseWriter.writeHeaders(this);
    }

    private void writeResponseWithEntity(MessageBodyWriter<?> entityWriter) throws IOException {
        writeResponseWithoutEntity();
        responseWriter.writeBody(this, entityWriter);
    }

    private boolean isEmptyOrContainsSingleWildcardMediaType(List<MediaType> acceptableWriterMediaTypes) {
        if (acceptableWriterMediaTypes.isEmpty()) {
            return true;
        }
        if (acceptableWriterMediaTypes.size() == 1) {
            MediaType mediaType = acceptableWriterMediaTypes.get(0);
            if (mediaType.isWildcardType() && mediaType.isWildcardSubtype()) {
                return true;
            }
        }
        return false;
    }

    private boolean isNullOrWildcard(MediaType contentType) {
        return contentType == null || contentType.isWildcardType() || contentType.isWildcardSubtype();
    }

    @Override
    public int getStatus() {
        return getStatusInfo().getStatusCode();
    }

    @Override
    public void setStatus(int code) {
        status = aStatus(code);
    }

    @Override
    public StatusType getStatusInfo() {
        if (status == null) {
            return aStatus(0);
        }
        return status;
    }

    @Override
    public void setStatusInfo(StatusType status) {
        this.status = status;
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        if (headers == null) {
            headers = new CaselessMultivaluedMap<>();
        }
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
    public Set<String> getAllowedMethods() {
        return getHeaderAsStrings(getHeaders().get(ALLOW)).stream().map(String::toUpperCase).collect(toSet());
    }

    @Override
    public Date getDate() {
        return getFistHeader(DATE, getHeaders(), Date.class);
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
    public MediaType getMediaType() {
        return getFistHeader(CONTENT_TYPE, getHeaders(), MediaType.class);
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

    @Override
    public boolean hasEntity() {
        return entity != null;
    }

    @Override
    public Class<?> getEntityClass() {
        return entityType == null ? null : entity.getClass();
    }

    @Override
    public Type getEntityType() {
        return entityType;
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public void setEntity(Object entity) {
        if (entity instanceof GenericEntity) {
            GenericEntity genericEntity = (GenericEntity)entity;
            this.entity = genericEntity.getEntity();
            entityType = genericEntity.getType();
        } else if (entity != null) {
            this.entity = entity;
            entityType = entity.getClass();
        } else {
            this.entity = null;
            entityType = null;
        }
    }

    @Override
    public void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) {
        getHeaders().putSingle(CONTENT_TYPE, mediaType);
        entityAnnotations = annotations;
        setEntity(entity);
    }

    @Override
    public Annotation[] getEntityAnnotations() {
        return entityAnnotations;
    }

    @Override
    public OutputStream getEntityStream() {
        return entityStream;
    }

    @Override
    public void setEntityStream(OutputStream entityStream) {
        if (this.entityStream != null) {
            try {
                this.entityStream.close();
            } catch (IOException e) {
                throw new ProcessingException(e.getMessage(), e);
            }
        }
        this.entityStream = entityStream;
    }

    private Optional<Link> findLink(String relation) {
        return getLinks().stream()
                .filter(l -> l.getRels().contains(relation))
                .findFirst();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("Status", status)
                          .add("Content type", getMediaType())
                          .add("Entity type", entityType)
                          .omitNullValues()
                          .toString();
    }

    private class HeadersWriter implements EntityStreamListener {
        boolean done;

        @Override
        public void onChange(EventObject event) throws IOException {
            if (done) {
                return;
            }
            writeResponseWithoutEntity();
            done = true;
        }
    }
}
