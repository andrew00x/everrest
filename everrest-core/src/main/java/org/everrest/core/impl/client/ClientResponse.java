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

import org.everrest.core.ConfigurationProperties;
import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.ResponseImpl;
import org.everrest.core.impl.header.HeaderHelper;
import org.everrest.core.impl.provider.IOHelper;
import org.everrest.core.util.CaselessMultivaluedMap;

import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LANGUAGE;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.DATE;
import static javax.ws.rs.core.HttpHeaders.ETAG;
import static javax.ws.rs.core.HttpHeaders.LAST_MODIFIED;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.HttpHeaders.SET_COOKIE;
import static org.everrest.core.impl.header.HeaderHelper.getContentLength;
import static org.everrest.core.impl.header.HeaderHelper.getFistHeader;
import static org.everrest.core.impl.header.HeaderHelper.getHeader;

public class ClientResponse implements ClientResponseContext {
    private final ProviderBinder providers;
    private final ConfigurationProperties properties;
    private final MultivaluedMap<String, String> headers;

    private StatusType status;
    private InputStream entityStream;

    ClientResponse(int status, MultivaluedMap<String, String> headers, ProviderBinder providers, ConfigurationProperties properties) {
        this.status = Status.fromStatusCode(status);
        this.headers = headers;
        this.providers = providers;
        this.properties = properties;
    }

    @Override
    public int getStatus() {
        return status == null ? 0 : status.getStatusCode();
    }

    @Override
    public void setStatus(int code) {
        status = Status.fromStatusCode(code);
    }

    @Override
    public StatusType getStatusInfo() {
        return status;
    }

    @Override
    public void setStatusInfo(StatusType status) {
        this.status = status;
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String getHeaderString(String name) {
        return HeaderHelper.convertToString(headers.get(name));
    }

    @Override
    public Set<String> getAllowedMethods() {
        List<String> allow = headers.get(ALLOW);
        if (allow == null || allow.isEmpty()) {
            return emptySet();
        }
        return allow.stream().map(String::toUpperCase).collect(toSet());
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

    private Optional<Link> findLink(String relation) {
        return getLinks().stream()
                .filter(l -> l.getRels().contains(relation))
                .findFirst();
    }

    @Override
    public boolean hasEntity() {
        return !IOHelper.isEmpty(entityStream);
    }

    @Override
    public InputStream getEntityStream() {
        return entityStream;
    }

    @Override
    public void setEntityStream(InputStream entityStream) {
        if (this.entityStream != null) {
            try {
                this.entityStream.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e.getMessage(), e);
            }
        }
        this.entityStream = entityStream;
    }

    public Response getResponse() {
        MultivaluedMap<String, Object> headers = new CaselessMultivaluedMap<>();
        getHeaders().forEach((key, value) -> headers.put(key, newArrayList(value)));
        return new ResponseImpl(getStatus(), getEntityStream(), new Annotation[0], headers, providers, properties);
    }
}
