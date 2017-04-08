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
package org.everrest.core;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gives access to common predefined providers. Users of EverRest are not expected to use this class or any of its subclasses.
 */
public interface ProviderBinder extends Providers, ParamConverterProvider {
    RuntimeType getRuntimeType();

    void register(Class<?> componentClass);

    void register(Class<?> componentClass, int priority);

    void register(Class<?> componentClass, Class<?>... contracts);

    void register(Class<?> componentClass, Map<Class<?>, Integer> contracts);

    void register(Object component);

    void register(Object component, int priority);

    void register(Object component, Class<?>... contracts);

    void register(Object component, Map<Class<?>, Integer> contracts);

    boolean isRegistered(Class<?> componentClass);

    boolean isRegistered(Object component);

    Map<Class<?>, Integer> getContracts(Class<?> componentClass);

    Set<Class<?>> getClasses();

    Set<Object> getInstances();

    boolean isEnabled(Class<? extends Feature> featureClass);

    boolean isEnabled(Feature feature);

    void setObjectFactoryProducer(ObjectFactoryProducer objectFactoryProducer);

    List<MediaType> getAcceptableWriterMediaTypes(Class<?> type, Type genericType, Annotation[] annotations);

    List<ReaderInterceptor> getReaderInterceptors();

    List<WriterInterceptor> getWriterInterceptors();

    List<ClientRequestFilter> getClientRequestFilters();

    List<ClientResponseFilter> getClientResponseFilters();

    List<ContainerRequestFilter> getContainerRequestFilters(Annotation[] nameBindingAnnotations, boolean preMatching);

    List<ContainerResponseFilter> getContainerResponseFilters(Annotation[] nameBindingAnnotations);

    List<DynamicFeature> getDynamicFeatures();

    void clear();

    default void copyComponentsFrom(ProviderBinder other) {
        Set<Class<?>> registeredAsInstances = new HashSet<>();
        other.getInstances().stream()
                .filter(component -> !(component instanceof Feature)) // we copy all registered component, don't need features that registered them
                .forEach(component -> {
                    register(component, other.getContracts(component.getClass()));
                    registeredAsInstances.add(component.getClass());
                });
        other.getClasses().stream()
                .filter(componentClass -> !Feature.class.isAssignableFrom(componentClass)) // we copy all registered component, don't need features that registered them
                .filter(componentClass -> !registeredAsInstances.contains(componentClass))
                .forEach(componentClass -> register(componentClass, other.getContracts(componentClass)));
    }
}
