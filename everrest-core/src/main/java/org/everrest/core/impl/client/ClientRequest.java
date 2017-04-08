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
import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.header.HeaderHelper;
import org.everrest.core.util.CaselessMultivaluedMap;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LANGUAGE;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.COOKIE;
import static javax.ws.rs.core.HttpHeaders.DATE;
import static org.everrest.core.impl.header.HeaderHelper.getFistHeader;
import static org.everrest.core.impl.header.HeaderHelper.getHeader;
import static org.everrest.core.impl.header.HeaderHelper.getHeadersView;

public class ClientRequest implements ClientRequestContext {
    private final EverrestClient client;
    private final ProviderBinder providers;
    private final ConfigurationProperties properties;
    private final EverrestConfiguration configuration;
    private final MultivaluedMap<String, Object> headers;

    private URI uri;
    private String httpMethod;
    private Object entity;
    private Type entityType;
    private Annotation[] annotations = new Annotation[0];
    private OutputStream entityStream;
    private Response abortResponse;

    ClientRequest(URI uri, EverrestClient client, ProviderBinder providers, ConfigurationProperties properties) {
        this.uri = uri;
        this.client = client;
        this.providers = providers;
        this.properties = properties;
        this.configuration = new EverrestConfiguration(providers, properties);
        headers = new CaselessMultivaluedMap<>();
    }

    @Override
    public Object getProperty(String name) {
        return configuration.getProperty(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return configuration.getPropertyNames();
    }

    @Override
    public void setProperty(String name, Object value) {
        configuration.setProperty(name, value);
    }

    @Override
    public void removeProperty(String name) {
        configuration.removeProperty(name);
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Override
    public String getMethod() {
        return httpMethod;
    }

    @Override
    public void setMethod(String method) {
        this.httpMethod = method;
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return headers;
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return getHeadersView(headers, HeaderHelper::getHeaderAsStrings);
    }

    @Override
    public String getHeaderString(String name) {
        return HeaderHelper.convertToString(headers.get(name));
    }

    @Override
    public Date getDate() {
        return getFistHeader(DATE, headers, Date.class);
    }

    @Override
    public Locale getLanguage() {
        return getFistHeader(CONTENT_LANGUAGE, headers, Locale.class);
    }

    @Override
    public MediaType getMediaType() {
        return getFistHeader(CONTENT_TYPE, headers, MediaType.class);
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return getHeader(ACCEPT, headers, MediaType.class);
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return getHeader(ACCEPT_LANGUAGE, headers, Locale.class);
    }

    @Override
    public Map<String, Cookie> getCookies() {
        List<Cookie> cookies = getHeader(COOKIE, headers, Cookie.class);
        return cookies.stream().collect(toMap(Cookie::getName, identity()));
    }

    @Override
    public boolean hasEntity() {
        return entity != null;
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public Class<?> getEntityClass() {
        return entity == null ? null : entity.getClass();
    }

    @Override
    public Type getEntityType() {
        return entityType;
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
        headers.putSingle(CONTENT_TYPE, mediaType);
        this.annotations = annotations;
        setEntity(entity);
    }

    @Override
    public Annotation[] getEntityAnnotations() {
        return annotations;
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
                throw new UncheckedIOException(e.getMessage(), e);
            }
        }
        this.entityStream = entityStream;
    }

    @Override
    public Client getClient() {
        return client;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void abortWith(Response response) {
        abortResponse = response;
    }

    public Response getAbortResponse() {
        return abortResponse;
    }

    public ProviderBinder getProviders() {
        return providers;
    }

    public ConfigurationProperties getProperties() {
        return properties;
    }
}
