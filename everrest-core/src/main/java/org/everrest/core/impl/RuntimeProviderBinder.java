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

import org.everrest.core.ConfigurationProperties;
import org.everrest.core.ObjectFactoryProducer;
import org.everrest.core.ProviderBinder;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Implementation on ProviderBinder that is used on client and server side to avoid impact of registration providers
 * in runtime on Everrest embedded ProviderBinder. This ProviderBinder uses wrapped ProviderBinder for retrieving providers
 * but once we need register any new provider copy of wrapped ProviderBinder is created. Copy is created only once at
 * first attempt to register provider. Once copy is created wrapped ProviderBinder is replaced with this newly created copy.
 */
public class RuntimeProviderBinder implements ProviderBinder {
    private final ConfigurationProperties properties;
    private ProviderBinder providers;
    private volatile boolean copied;

    public RuntimeProviderBinder(ProviderBinder providers, ConfigurationProperties properties) {
        this.providers = requireNonNull(providers);
        this.properties = requireNonNull(properties);
    }

    @Override
    public RuntimeType getRuntimeType() {
        return get(false).getRuntimeType();
    }

    @Override
    public void register(Class<?> componentClass) {
        get(true).register(componentClass);
    }

    @Override
    public void register(Class<?> componentClass, int priority) {
        get(true).register(componentClass, priority);
    }

    @Override
    public void register(Class<?> componentClass, Class<?>... contracts) {
        get(true).register(componentClass, contracts);
    }

    @Override
    public void register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        get(true).register(componentClass, contracts);
    }

    @Override
    public void register(Object component) {
        get(true).register(component);
    }

    @Override
    public void register(Object component, int priority) {
        get(true).register(component, priority);
    }

    @Override
    public void register(Object component, Class<?>... contracts) {
        get(true).register(component, contracts);
    }

    @Override
    public void register(Object component, Map<Class<?>, Integer> contracts) {
        get(true).register(component, contracts);
    }

    @Override
    public boolean isRegistered(Class<?> componentClass) {
        return get(false).isRegistered(componentClass);
    }

    @Override
    public boolean isRegistered(Object component) {
        return get(false).isRegistered(component);
    }

    @Override
    public Map<Class<?>, Integer> getContracts(Class<?> componentClass) {
        return get(false).getContracts(componentClass);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return get(false).getClasses();
    }

    @Override
    public Set<Object> getInstances() {
        return get(false).getInstances();
    }

    @Override
    public boolean isEnabled(Class<? extends Feature> featureClass) {
        return get(false).isEnabled(featureClass);
    }

    @Override
    public boolean isEnabled(Feature feature) {
        return get(false).isEnabled(feature);
    }

    @Override
    public void setObjectFactoryProducer(ObjectFactoryProducer objectFactoryProducer) {
        get(true).setObjectFactoryProducer(objectFactoryProducer);
    }

    @Override
    public List<MediaType> getAcceptableWriterMediaTypes(Class<?> type, Type genericType, Annotation[] annotations) {
        return get(false).getAcceptableWriterMediaTypes(type, genericType, annotations);
    }

    @Override
    public List<ReaderInterceptor> getReaderInterceptors() {
        return get(false).getReaderInterceptors();
    }

    @Override
    public List<WriterInterceptor> getWriterInterceptors() {
        return get(false).getWriterInterceptors();
    }

    @Override
    public List<ClientRequestFilter> getClientRequestFilters() {
        return get(false).getClientRequestFilters();
    }

    @Override
    public List<ClientResponseFilter> getClientResponseFilters() {
        return get(false).getClientResponseFilters();
    }

    @Override
    public List<ContainerRequestFilter> getContainerRequestFilters(Annotation[] nameBindingAnnotations, boolean preMatching) {
        return get(false).getContainerRequestFilters(nameBindingAnnotations, preMatching);
    }

    @Override
    public List<ContainerResponseFilter> getContainerResponseFilters(Annotation[] nameBindingAnnotations) {
        return get(false).getContainerResponseFilters(nameBindingAnnotations);
    }

    @Override
    public List<DynamicFeature> getDynamicFeatures() {
        return get(false).getDynamicFeatures();
    }

    @Override
    public void clear() {
        get(true).clear();
    }

    @Override
    public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return get(false).getMessageBodyReader(type, genericType, annotations, mediaType);
    }

    @Override
    public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return get(false).getMessageBodyWriter(type, genericType, annotations, mediaType);
    }

    @Override
    public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
        return get(false).getExceptionMapper(type);
    }

    @Override
    public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
        return get(false).getContextResolver(contextType, mediaType);
    }

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        return get(false).getConverter(rawType, genericType, annotations);
    }

    private ProviderBinder get(boolean createCopy) {
        if (createCopy) {
            if (!copied) {
                synchronized (this) {
                    if (!copied) {
                        ProviderBinder copiedProviders = new DefaultProviderBinder(this.providers.getRuntimeType(), properties);
                        copiedProviders.copyComponentsFrom(this.providers);
                        this.providers = copiedProviders;
                        copied = true;
                    }
                }
            }
        }
        return this.providers;
    }
}
