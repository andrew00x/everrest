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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.InterceptorContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

public abstract class BaseInterceptorContext implements InterceptorContext {
    private Class<?> type;
    private Type genericType;
    private Annotation[] annotations;
    private MediaType mediaType;
    private final ConfigurationProperties properties;

    protected BaseInterceptorContext(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, ConfigurationProperties properties) {
        this.type = type;
        this.genericType = genericType;
        this.annotations = annotations;
        this.mediaType = mediaType;
        this.properties = properties;
    }

    @Override
    public Object getProperty(String name) {
        return properties.getProperty(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return properties.getPropertyNames();
    }

    @Override
    public void setProperty(String name, Object value) {
        properties.setProperty(name, value);
    }

    @Override
    public void removeProperty(String name) {
        properties.removeProperty(name);
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotations;
    }

    @Override
    public void setAnnotations(Annotation[] annotations) {
        requireNonNull(annotations);
        this.annotations = annotations;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public void setType(Class<?> type) {
        this.type = type;
    }

    @Override
    public Type getGenericType() {
        return genericType;
    }

    @Override
    public void setGenericType(Type genericType) {
        this.genericType = genericType;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }
}
