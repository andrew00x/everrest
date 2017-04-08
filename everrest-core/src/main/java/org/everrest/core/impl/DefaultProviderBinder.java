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

import com.google.common.collect.ImmutableMap;
import org.everrest.core.ConfigurationProperties;
import org.everrest.core.ObjectFactory;
import org.everrest.core.ObjectFactoryProducer;
import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.method.ConstructorStringParamConverter;
import org.everrest.core.impl.method.PrimitiveTypeParamConverter;
import org.everrest.core.impl.method.StringParamConverter;
import org.everrest.core.impl.method.ValueOfStringParamConverter;
import org.everrest.core.impl.provider.ProviderDescriptorImpl;
import org.everrest.core.provider.ProviderDescriptor;
import org.everrest.core.util.MediaTypeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.Priorities.USER;
import static javax.ws.rs.RuntimeType.CLIENT;
import static javax.ws.rs.RuntimeType.SERVER;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static org.everrest.core.impl.header.MediaTypeHelper.createDescendingMediaTypeIterator;
import static org.everrest.core.util.ArrayUtils.disjoint;
import static org.everrest.core.util.ReflectionUtils.getImplementedInterfaces;
import static org.everrest.core.util.ReflectionUtils.getStringConstructor;
import static org.everrest.core.util.ReflectionUtils.getStringValueOfMethod;

/**
 * Gives access to common predefined providers. Users of EverRest are not expected to use this class or any of its subclasses.
 */
public class DefaultProviderBinder implements ProviderBinder {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultProviderBinder.class);

    private static final int NO_PRIORITY = -1;

    private static Map<Class<?>, Set<RuntimeType>> supportedContracts = ImmutableMap.<Class<?>, Set<RuntimeType>>builder()
            .put(ExceptionMapper.class, EnumSet.of(CLIENT, SERVER))
            .put(MessageBodyReader.class, EnumSet.of(CLIENT, SERVER))
            .put(MessageBodyWriter.class, EnumSet.of(CLIENT, SERVER))
            .put(ContextResolver.class, EnumSet.of(CLIENT, SERVER))
            .put(ReaderInterceptor.class, EnumSet.of(CLIENT, SERVER))
            .put(WriterInterceptor.class, EnumSet.of(CLIENT, SERVER))
            .put(ParamConverterProvider.class, EnumSet.of(CLIENT, SERVER))
            .put(Feature.class, EnumSet.of(CLIENT, SERVER))
            .put(ContainerRequestFilter.class, EnumSet.of(SERVER))
            .put(ContainerResponseFilter.class, EnumSet.of(SERVER))
            .put(DynamicFeature.class, EnumSet.of(SERVER))
            .put(ClientRequestFilter.class, EnumSet.of(CLIENT))
            .put(ClientResponseFilter.class, EnumSet.of(CLIENT))
            .build();

    public static Set<Class<?>> getSupportedContracts(RuntimeType runtimeType) {
        return supportedContracts.entrySet().stream()
                .filter(e -> e.getValue().contains(runtimeType))
                .map(Map.Entry::getKey)
                .collect(toSet());
    }

    private static MediaTypeComparator mediaTypeComparator = new MediaTypeComparator();

    private static Comparator<ObjectFactory<ProviderDescriptor>> byPriorityComparator = (objectFactoryOne, objectFactoryTwo) -> {
        int result = objectFactoryOne.getObjectModel().getPriority().get()
                .compareTo(objectFactoryTwo.getObjectModel().getPriority().get());
        if (result == 0) {
            if (objectFactoryOne.getObjectModel().getObjectClass() != objectFactoryTwo.getObjectModel().getObjectClass()) {
                return 1;
            }
        }
        return result;
    };

    private Predicate<Class<?>> contractAssignableFromPredicate(Class<?> componentClass) {
        return contract -> {
            if (contract.isAssignableFrom(componentClass)) {
                return true;
            }
            LOG.warn("Ignore contract {} that is not assignable to component {}", contract, componentClass);
            return false;
        };
    }

    private Predicate<Class<?>> contractSupportedPredicate() {
        return contract -> supportedContracts.entrySet().stream()
                .filter(e -> e.getValue().contains(this.runtimeType))
                .map(Map.Entry::getKey)
                .anyMatch(contract::isAssignableFrom);
    }

    private Predicate<Class<?>> contractSupportedLoggingPredicate() {
        Predicate<Class<?>> predicate = contractSupportedPredicate();
        return contract -> {
            boolean result = predicate.test(contract);
            if (!result) {
                LOG.warn("Ignore unsupported contract {}", contract);
            }
            return result;
        };
    }

    private static class ContextResolverKey {
        static ContextResolverKey of(Class<?> contextType, MediaType mediaType) {
            return new ContextResolverKey(contextType, mediaType);
        }

        final Class<?> contextType;
        final MediaType mediaType;

        ContextResolverKey(Class<?> contextType, MediaType mediaType) {
            this.contextType = contextType;
            this.mediaType = mediaType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ContextResolverKey)) {
                return false;
            }
            ContextResolverKey other = (ContextResolverKey) o;
            return Objects.equals(contextType, other.contextType)
                   && Objects.equals(mediaType, other.mediaType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contextType, mediaType);
        }
    }

    private final RuntimeType runtimeType;
    private final ConfigurationProperties configuration;
    protected ObjectFactoryProducer objectFactoryProducer = new DefaultObjectFactoryProducer();

    protected final ConcurrentNavigableMap<MediaType, Collection<ObjectFactory<ProviderDescriptor>>> writeProviders = new ConcurrentSkipListMap<>(mediaTypeComparator);
    protected final ConcurrentNavigableMap<MediaType, Collection<ObjectFactory<ProviderDescriptor>>> readProviders = new ConcurrentSkipListMap<>(mediaTypeComparator);
    protected final ConcurrentMap<Class<? extends Throwable>, ObjectFactory<ProviderDescriptor>> exceptionMappers = new ConcurrentHashMap<>();
    protected final ConcurrentMap<ContextResolverKey, ObjectFactory<ProviderDescriptor>> contextResolvers = new ConcurrentHashMap<>();
    protected final Set<ObjectFactory<ProviderDescriptor>> readerInterceptors = new ConcurrentSkipListSet<>(byPriorityComparator);
    protected final Set<ObjectFactory<ProviderDescriptor>> writerInterceptors = new ConcurrentSkipListSet<>(byPriorityComparator);
    protected final Set<ObjectFactory<ProviderDescriptor>> clientRequestFilters = new ConcurrentSkipListSet<>(byPriorityComparator);
    protected final Set<ObjectFactory<ProviderDescriptor>> clientResponseFilters = new ConcurrentSkipListSet<>(byPriorityComparator.reversed());
    protected final Set<ObjectFactory<ProviderDescriptor>> containerRequestFilters = new ConcurrentSkipListSet<>(byPriorityComparator);
    protected final Set<ObjectFactory<ProviderDescriptor>> containerResponseFilters = new ConcurrentSkipListSet<>(byPriorityComparator.reversed());
    protected final Set<ObjectFactory<ProviderDescriptor>> paramConverterProviders = new CopyOnWriteArraySet<>();
    protected final Set<ObjectFactory<ProviderDescriptor>> features = new CopyOnWriteArraySet<>();
    protected final Set<ObjectFactory<ProviderDescriptor>> dynamicFeatures = new CopyOnWriteArraySet<>();
    protected final Set<Class<?>> enabledFeatureClasses = new CopyOnWriteArraySet<>();
    protected final Set<Feature> enabledFeatures = new CopyOnWriteArraySet<>();
    protected final Map<Class<?>, ComponentConfiguration> allRegistrations = new ConcurrentHashMap<>();

    public DefaultProviderBinder(RuntimeType runtimeType, ConfigurationProperties configuration) {
        this.runtimeType = runtimeType;
        this.configuration = configuration;
    }

    @Override
    public RuntimeType getRuntimeType() {
        return runtimeType;
    }

    @Override
    public void register(Class<?> componentClass) {
        requireNonNull(componentClass);
        register(new ComponentConfiguration(componentClass, null, findContracts(componentClass)));
    }

    @Override
    public void register(Class<?> componentClass, int priority) {
        requireNonNull(componentClass);
        register(new ComponentConfiguration(componentClass, null, findContracts(componentClass, priority)));
    }

    @Override
    public void register(Class<?> componentClass, Class<?>... contracts) {
        requireNonNull(componentClass);
        if (contracts == null || contracts.length == 0) {
            LOG.warn("Ignore component {} with empty or null contracts", componentClass);
        } else {
            register(new ComponentConfiguration(componentClass, null, asMapOfContacts(componentClass, contracts)));
        }
    }

    @Override
    public void register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        requireNonNull(componentClass);
        if (contracts == null || contracts.isEmpty()) {
            LOG.warn("Ignore component {} with empty or null contracts", componentClass);
        } else {
            Predicate<Class<?>> contractSupportedPredicate = contractSupportedLoggingPredicate();
            Predicate<Class<?>> contractAssignableFromPredicate = contractAssignableFromPredicate(componentClass);
            Map<Class<?>, Integer> filteredContacts = contracts.entrySet().stream()
                    .filter(e -> contractAssignableFromPredicate.test(e.getKey()))
                    .filter(e -> contractSupportedPredicate.test(e.getKey()))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
            register(new ComponentConfiguration(componentClass, null, filteredContacts));
        }
    }

    @Override
    public void register(Object component) {
        requireNonNull(component);
        register(new ComponentConfiguration(component.getClass(), component, findContracts(component.getClass())));
    }

    @Override
    public void register(Object component, int priority) {
        requireNonNull(component);
        register(new ComponentConfiguration(component.getClass(), component, findContracts(component.getClass(), priority)));
    }

    @Override
    public void register(Object component, Class<?>... contracts) {
        requireNonNull(component);
        if (contracts == null || contracts.length == 0) {
            LOG.warn("Ignore component {} with empty or null contracts", component);
        } else {
            register(new ComponentConfiguration(component.getClass(), component, asMapOfContacts(component.getClass(), contracts)));
        }
    }

    @Override
    public void register(Object component, Map<Class<?>, Integer> contracts) {
        requireNonNull(component);
        if (contracts == null || contracts.isEmpty()) {
            LOG.warn("Ignore component {} with empty or null contracts", component);
        } else {
            Predicate<Class<?>> contractSupportedPredicate = contractSupportedLoggingPredicate();
            Predicate<Class<?>> contractAssignableFromPredicate = contractAssignableFromPredicate(component.getClass());
            Map<Class<?>, Integer> filteredContacts = contracts.entrySet().stream()
                    .filter(e -> contractAssignableFromPredicate.test(e.getKey()))
                    .filter(e -> contractSupportedPredicate.test(e.getKey()))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
            register(new ComponentConfiguration(component.getClass(), component, filteredContacts));
        }
    }

    private Map<Class<?>, Integer> findContracts(Class<?> componentClass) {
        return findContracts(componentClass, getPriorityAnnotationValue(componentClass).orElse(NO_PRIORITY));
    }

    private Map<Class<?>, Integer> findContracts(Class<?> componentClass, Integer priority) {
        return getImplementedInterfaces(componentClass).stream()
                .filter(contractSupportedPredicate())
                .collect(toMap(identity(), e -> priority));
    }

    private Map<Class<?>, Integer> asMapOfContacts(Class<?> componentClass, Class<?>... contracts) {
        Integer priority = getPriorityAnnotationValue(componentClass).orElse(NO_PRIORITY);
        return Arrays.stream(contracts)
                .filter(contractAssignableFromPredicate(componentClass))
                .filter(contractSupportedLoggingPredicate())
                .collect(toMap(identity(), e -> priority));
    }

    private Optional<Integer> getPriorityAnnotationValue(Class<?> aClass) {
        Priority priority = aClass.getAnnotation(Priority.class);
        return priority == null ? Optional.empty() : Optional.of(priority.value());
    }

    @SuppressWarnings("unchecked")
    private void register(ComponentConfiguration configuration) {
        Class<?> componentClass = configuration.getComponentClass();
        Object component = configuration.getComponent();
        final boolean singleton = component != null;
        boolean registered = false;
        for (Map.Entry<Class<?>, Integer> entry : configuration.getContracts().entrySet()) {
            Class<?> contract = entry.getKey();
            Integer priority = entry.getValue();
            if (ExceptionMapper.class.isAssignableFrom(contract)) {
                if (singleton) {
                    registered |= addExceptionMapper(objectFactoryProducer.create(new ProviderDescriptorImpl(component, contract), component));
                } else {
                    registered |= addExceptionMapper(objectFactoryProducer.create(new ProviderDescriptorImpl(componentClass, contract)));
                }
            } else if (MessageBodyReader.class.isAssignableFrom(contract)) {
                if (singleton) {
                    registered |= addMessageBodyReader(objectFactoryProducer.create(new ProviderDescriptorImpl(component, contract, priority), component));
                } else {
                    registered |= addMessageBodyReader(objectFactoryProducer.create(new ProviderDescriptorImpl(componentClass, contract, priority)));
                }
            } else if (MessageBodyWriter.class.isAssignableFrom(contract)) {
                if (singleton) {
                    registered |= addMessageBodyWriter(objectFactoryProducer.create(new ProviderDescriptorImpl(component, contract, priority), component));
                } else {
                    registered |= addMessageBodyWriter(objectFactoryProducer.create(new ProviderDescriptorImpl(componentClass, contract, priority)));
                }
            } else if (ContextResolver.class.isAssignableFrom(contract)) {
                if (singleton) {
                    registered |= addContextResolver(objectFactoryProducer.create(new ProviderDescriptorImpl(component, contract), component));
                } else {
                    registered |= addContextResolver(objectFactoryProducer.create(new ProviderDescriptorImpl(componentClass, contract)));
                }
            } else if (ReaderInterceptor.class.isAssignableFrom(contract)) {
                if (singleton) {
                    registered |= addReaderInterceptor(objectFactoryProducer.create(new ProviderDescriptorImpl(component, contract, priority == NO_PRIORITY ? USER : priority), component));
                } else {
                    registered |= addReaderInterceptor(objectFactoryProducer.create(new ProviderDescriptorImpl(componentClass, contract, priority == NO_PRIORITY ? USER : priority)));
                }
            } else if (WriterInterceptor.class.isAssignableFrom(contract)) {
                if (singleton) {
                    registered |= addWriterInterceptor(objectFactoryProducer.create(new ProviderDescriptorImpl(component, contract, priority == NO_PRIORITY ? USER : priority), component));
                } else {
                    registered |= addWriterInterceptor(objectFactoryProducer.create(new ProviderDescriptorImpl(componentClass, contract, priority == NO_PRIORITY ? USER : priority)));
                }
            } else if (ClientRequestFilter.class.isAssignableFrom(contract)) {
                if (singleton) {
                    registered |= addClientRequestFilter(objectFactoryProducer.create(new ProviderDescriptorImpl(component, contract, priority == NO_PRIORITY ? USER : priority), component));
                } else {
                    registered |= addClientRequestFilter(objectFactoryProducer.create(new ProviderDescriptorImpl(componentClass, contract, priority == NO_PRIORITY ? USER : priority)));
                }
            } else if (ClientResponseFilter.class.isAssignableFrom(contract)) {
                if (singleton) {
                    registered |= addClientResponseFilter(objectFactoryProducer.create(new ProviderDescriptorImpl(component, contract, priority == NO_PRIORITY ? USER : priority), component));
                } else {
                    registered |= addClientResponseFilter(objectFactoryProducer.create(new ProviderDescriptorImpl(componentClass, contract, priority == NO_PRIORITY ? USER : priority)));
                }
            } else if (ContainerRequestFilter.class.isAssignableFrom(contract)) {
                if (singleton) {
                    registered |= addContainerRequestFilter(objectFactoryProducer.create(new ProviderDescriptorImpl(component, contract, priority == NO_PRIORITY ? USER : priority), component));
                } else {
                    registered |= addContainerRequestFilter(objectFactoryProducer.create(new ProviderDescriptorImpl(componentClass, contract, priority == NO_PRIORITY ? USER : priority)));
                }
            } else if (ContainerResponseFilter.class.isAssignableFrom(contract)) {
                if (singleton) {
                    registered |= addContainerResponseFilter(objectFactoryProducer.create(new ProviderDescriptorImpl(component, contract, priority == NO_PRIORITY ? USER : priority), component));
                } else {
                    registered |= addContainerResponseFilter(objectFactoryProducer.create(new ProviderDescriptorImpl(componentClass, contract, priority == NO_PRIORITY ? USER : priority)));
                }
            } else if (ParamConverterProvider.class.isAssignableFrom(contract)) {
                if (singleton) {
                    registered |= addParamConverterProvider(objectFactoryProducer.create(new ProviderDescriptorImpl(component, contract), component));
                } else {
                    registered |= addParamConverterProvider(objectFactoryProducer.create(new ProviderDescriptorImpl(componentClass, contract)));
                }
            } else if (Feature.class.isAssignableFrom(contract)) {
                if (singleton) {
                    registered |= addFeature(objectFactoryProducer.create(new ProviderDescriptorImpl(component, contract), component));
                } else {
                    registered |= addFeature(objectFactoryProducer.create(new ProviderDescriptorImpl(componentClass, contract)));
                }
            } else if (DynamicFeature.class.isAssignableFrom(contract)) {
                if (singleton) {
                    registered |= addDynamicFeature(objectFactoryProducer.create(new ProviderDescriptorImpl(component, contract), component));
                } else {
                    registered |= addDynamicFeature(objectFactoryProducer.create(new ProviderDescriptorImpl(componentClass, contract)));
                }
            }
        }
        if (registered) {
            LOG.debug("Add provider: {}", singleton ? component : componentClass);
            allRegistrations.put(componentClass, configuration);
        }
    }

    @Override
    public boolean isRegistered(Class<?> componentClass) {
        return allRegistrations.containsKey(componentClass);
    }

    @Override
    public boolean isRegistered(Object component) {
        ComponentConfiguration configuration = allRegistrations.get(component.getClass());
        return configuration.getComponent() == component;
    }

    @Override
    public Map<Class<?>, Integer> getContracts(Class<?> componentClass) {
        ComponentConfiguration configuration = allRegistrations.get(componentClass);
        if (configuration == null) {
            return emptyMap();
        }
        return configuration.getContracts();
    }

    @Override
    public Set<Class<?>> getClasses() {
        return unmodifiableSet(allRegistrations.keySet());
    }

    @Override
    public Set<Object> getInstances() {
        return allRegistrations.values().stream()
                .filter(configuration -> configuration.getComponent() != null)
                .map(ComponentConfiguration::getComponent)
                .collect(toSet());
    }

    @Override
    public boolean isEnabled(Class<? extends Feature> featureClass) {
        return enabledFeatureClasses.contains(featureClass);
    }

    @Override
    public boolean isEnabled(Feature feature) {
        return enabledFeatures.contains(feature);
    }

    @Override
    public void setObjectFactoryProducer(ObjectFactoryProducer objectFactoryProducer) {
        this.objectFactoryProducer = objectFactoryProducer;
    }

    protected boolean addReaderInterceptor(ObjectFactory<ProviderDescriptor> interceptorFactory) {
        if (readerInterceptors.add(interceptorFactory)) {
            return true;
        }
        LOG.warn("Ignore ReaderInterceptor {}. The same component class already registered",
                interceptorFactory instanceof SingletonObjectFactory ? ((SingletonObjectFactory) interceptorFactory).getInstance() : interceptorFactory.getObjectModel().getObjectClass());
        return false;
    }

    protected boolean addWriterInterceptor(ObjectFactory<ProviderDescriptor> interceptorFactory) {
        if (writerInterceptors.add(interceptorFactory)) {
            return true;
        }
        LOG.warn("Ignore WriterInterceptor {}. The same component class already registered",
                interceptorFactory instanceof SingletonObjectFactory ? ((SingletonObjectFactory) interceptorFactory).getInstance() : interceptorFactory.getObjectModel().getObjectClass());
        return false;
    }

    protected boolean addClientRequestFilter(ObjectFactory<ProviderDescriptor> filterFactory) {
        if (clientRequestFilters.add(filterFactory)) {
            return true;
        }
        LOG.warn("Ignore ClientRequestFilter {}. The same component class already registered",
                filterFactory instanceof SingletonObjectFactory ? ((SingletonObjectFactory) filterFactory).getInstance() : filterFactory.getObjectModel().getObjectClass());
        return false;
    }

    protected boolean addClientResponseFilter(ObjectFactory<ProviderDescriptor> filterFactory) {
        if (clientResponseFilters.add(filterFactory)) {
            return true;
        }
        LOG.warn("Ignore ClientResponseFilter {}. The same component class already registered",
                filterFactory instanceof SingletonObjectFactory ? ((SingletonObjectFactory) filterFactory).getInstance() : filterFactory.getObjectModel().getObjectClass());
        return false;
    }

    protected boolean addContainerRequestFilter(ObjectFactory<ProviderDescriptor> filterFactory) {
        if (containerRequestFilters.add(filterFactory)) {
            return true;
        }
        LOG.warn("Ignore ContainerRequestFilter {}. The same component class already registered",
                filterFactory instanceof SingletonObjectFactory ? ((SingletonObjectFactory) filterFactory).getInstance() : filterFactory.getObjectModel().getObjectClass());
        return false;
    }

    protected boolean addContainerResponseFilter(ObjectFactory<ProviderDescriptor> filterFactory) {
        if (containerResponseFilters.add(filterFactory)) {
            return true;
        }
        LOG.warn("Ignore ContainerResponseFilter {}. The same component class already registered",
                filterFactory instanceof SingletonObjectFactory ? ((SingletonObjectFactory) filterFactory).getInstance() : filterFactory.getObjectModel().getObjectClass());
        return false;
    }

    protected boolean addParamConverterProvider(ObjectFactory<ProviderDescriptor> paramConverterProviderFactory) {
        if (paramConverterProviders.add(paramConverterProviderFactory)) {
            return true;
        }
        LOG.warn("Ignore ParamConverterProvider {}. The same component class already registered",
                paramConverterProviderFactory instanceof SingletonObjectFactory ? ((SingletonObjectFactory) paramConverterProviderFactory).getInstance() : paramConverterProviderFactory.getObjectModel().getObjectClass());
        return false;
    }

    protected boolean addFeature(ObjectFactory<ProviderDescriptor> featureFactory) {
        if (features.add(featureFactory)) {
            Feature feature = (Feature) featureFactory.getInstance(null);
            boolean enabled = feature.configure(new DefaultFeatureContext(this, configuration));
            if (enabled) {
                if (featureFactory instanceof SingletonObjectFactory) {
                    enabledFeatures.add(feature);
                    enabledFeatureClasses.add(feature.getClass());
                } else {
                    enabledFeatureClasses.add(featureFactory.getObjectModel().getObjectClass());
                }
            }
            return true;
        }
        LOG.warn("Ignore Feature {}. The same component class already registered",
                featureFactory instanceof SingletonObjectFactory ? ((SingletonObjectFactory) featureFactory).getInstance() : featureFactory.getObjectModel().getObjectClass());
        return false;
    }

    protected boolean addDynamicFeature(ObjectFactory<ProviderDescriptor> dynamicFeatureFactory) {
        if (dynamicFeatures.add(dynamicFeatureFactory)) {
            return true;
        }
        LOG.warn("Ignore DynamicFeature {}. The same component class already registered",
                dynamicFeatureFactory instanceof SingletonObjectFactory ? ((SingletonObjectFactory) dynamicFeatureFactory).getInstance() : dynamicFeatureFactory.getObjectModel().getObjectClass());
        return false;
    }


    /**
     * Get list of most acceptable writer's media type for specified type.
     *
     * @param type        type
     * @param genericType generic type
     * @param annotations annotations
     * @return sorted acceptable media type collection
     */
    @Override
    public List<MediaType> getAcceptableWriterMediaTypes(Class<?> type, Type genericType, Annotation[] annotations) {
        return doGetAcceptableWriterMediaTypes(type, genericType, annotations);
    }

    @Override
    public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
        return doGetContextResolver(contextType, mediaType);
    }

    @Override
    public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> errorType) {
        return doGetExceptionMapper(errorType);
    }

    @Override
    public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return doGetMessageBodyReader(type, genericType, annotations, mediaType);
    }

    @Override
    public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return doGetMessageBodyWriter(type, genericType, annotations, mediaType);
    }

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        ParamConverter<T> converter = doGetConverter(rawType, genericType, annotations);
        if (converter == null) {
            converter = getEmbeddedConverter(rawType);
        }
        return converter;
    }

    @SuppressWarnings("unchecked")
    private <T> ParamConverter<T> getEmbeddedConverter(Class<T> aClass) {
        Method methodValueOf;
        Constructor<?> constructor;
        if (aClass.isPrimitive()) {
            return (ParamConverter<T>) new PrimitiveTypeParamConverter(aClass);
        } else if (aClass == String.class) {
            return (ParamConverter<T>) new StringParamConverter();
        } else if ((methodValueOf = getStringValueOfMethod(aClass)) != null) {
            return (ParamConverter<T>) new ValueOfStringParamConverter(methodValueOf);
        } else if ((constructor = getStringConstructor(aClass)) != null) {
            return (ParamConverter<T>) new ConstructorStringParamConverter(constructor);
        } else {
            return null;
        }
    }

    @Override
    public List<ReaderInterceptor> getReaderInterceptors() {
        return doGetReaderInterceptors();
    }

    @Override
    public List<WriterInterceptor> getWriterInterceptors() {
        return doGetWriterInterceptors();
    }

    @Override
    public List<ClientRequestFilter> getClientRequestFilters() {
        return doGetClientRequestFilters();
    }

    @Override
    public List<ClientResponseFilter> getClientResponseFilters() {
        return doGetClientResponseFilters();
    }

    @Override
    public List<ContainerRequestFilter> getContainerRequestFilters(Annotation[] nameBindingAnnotations, boolean preMatching) {
        return doGetContainerRequestFilters(nameBindingAnnotations, preMatching);
    }

    @Override
    public List<ContainerResponseFilter> getContainerResponseFilters(Annotation[] nameBindingAnnotations) {
        return doGetContainerResponseFilters(nameBindingAnnotations);
    }

    @Override
    public List<DynamicFeature> getDynamicFeatures() {
        return doGetDynamicFeatures();
    }

    protected boolean addContextResolver(ObjectFactory<ProviderDescriptor> contextResolverFactory) {
        boolean added = false;
        for (Type type : contextResolverFactory.getObjectModel().getObjectClass().getGenericInterfaces()) {
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType)type;
                if (ContextResolver.class == parameterizedType.getRawType()) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length != 1) {
                        throw new IllegalArgumentException("Unable strongly determine actual type argument");
                    }
                    Class<?> contextType = (Class<?>)typeArguments[0];
                    for (MediaType mediaType : contextResolverFactory.getObjectModel().produces()) {
                        if (contextResolvers.putIfAbsent(ContextResolverKey.of(contextType, mediaType), contextResolverFactory) == null) {
                            added = true;
                        } else {
                            LOG.warn("Ignore ContextResolver {}. ContextResolver for {} and media type {} already registered",
                                    contextResolverFactory instanceof SingletonObjectFactory ? ((SingletonObjectFactory) contextResolverFactory).getInstance() : contextResolverFactory.getObjectModel().getObjectClass(),
                                    contextType,
                                    mediaType);
                        }
                    }
                }
            }
        }
        return added;
    }

    @SuppressWarnings("unchecked")
    protected boolean addExceptionMapper(ObjectFactory<ProviderDescriptor> exceptionMapperFactory) {
        for (Type type : exceptionMapperFactory.getObjectModel().getObjectClass().getGenericInterfaces()) {
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType)type;
                if (ExceptionMapper.class == parameterizedType.getRawType()) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length != 1) {
                        throw new IllegalArgumentException("Unable strong determine actual type argument");
                    }
                    Class<? extends Throwable> errorType = (Class<? extends Throwable>)typeArguments[0];
                    if (exceptionMappers.putIfAbsent(errorType, exceptionMapperFactory) == null) {
                        return true;
                    } else {
                        LOG.warn("Ignore ExceptionMapper {}. ExceptionMapper for exception {} already registered",
                                exceptionMapperFactory instanceof SingletonObjectFactory ? ((SingletonObjectFactory) exceptionMapperFactory).getInstance() : exceptionMapperFactory.getObjectModel().getObjectClass(),
                                errorType);
                    }
                }
            }
        }
        return false;
    }

    protected boolean addMessageBodyReader(ObjectFactory<ProviderDescriptor> readerFactory) {
        boolean added = false;
        for (MediaType mediaType : readerFactory.getObjectModel().consumes()) {
            if (addProviderFactory(readProviders, mediaType, readerFactory)) {
                added = true;
            } else {
                LOG.warn("Ignore MessageBodyReader {} for media type {}. The same component class already registered",
                        readerFactory instanceof SingletonObjectFactory ? ((SingletonObjectFactory) readerFactory).getInstance() : readerFactory.getObjectModel().getObjectClass(),
                        mediaType);
            }
        }
        return added;
    }

    protected boolean addMessageBodyWriter(ObjectFactory<ProviderDescriptor> writerFactory) {
        boolean added = false;
        for (MediaType mediaType : writerFactory.getObjectModel().produces()) {
            if (addProviderFactory(writeProviders, mediaType, writerFactory)) {
                added = true;
            } else {
                LOG.warn("Ignore MessageBodyWriter {} for media type {}. The same component class already registered",
                        writerFactory instanceof SingletonObjectFactory ? ((SingletonObjectFactory) writerFactory).getInstance() : writerFactory.getObjectModel().getObjectClass(),
                        mediaType);
            }
        }
        return added;
    }

    private <K> boolean addProviderFactory(ConcurrentMap<K, Collection<ObjectFactory<ProviderDescriptor>>> providersFactoryMap,
                                           K key,
                                           ObjectFactory<ProviderDescriptor> providerFactory) {
        ConcurrentSkipListSet<ObjectFactory<ProviderDescriptor>> providersFactoryList = (ConcurrentSkipListSet<ObjectFactory<ProviderDescriptor>>) providersFactoryMap.get(key);
        if (providersFactoryList == null) {
            ConcurrentSkipListSet<ObjectFactory<ProviderDescriptor>> newList = new ConcurrentSkipListSet<>(byPriorityComparator);
            providersFactoryList = (ConcurrentSkipListSet<ObjectFactory<ProviderDescriptor>>) providersFactoryMap.putIfAbsent(key, newList);
            if (providersFactoryList == null) {
                providersFactoryList = newList;
            }
        }
        return providersFactoryList.add(providerFactory);
    }

    @SuppressWarnings({"unchecked"})
    protected List<MediaType> doGetAcceptableWriterMediaTypes(Class<?> type, Type genericType, Annotation[] annotations) {
        List<MediaType> result = new ArrayList<>();
        Map<Class, MessageBodyWriter> instanceCache = new HashMap<>();
        for (Map.Entry<MediaType, Collection<ObjectFactory<ProviderDescriptor>>> e : writeProviders.entrySet()) {
            MediaType mediaType = e.getKey();
            for (ObjectFactory messageBodyWriterFactory : e.getValue()) {
                Class messageBodyWriterClass = messageBodyWriterFactory.getObjectModel().getObjectClass();
                MessageBodyWriter messageBodyWriter = instanceCache.get(messageBodyWriterClass);
                if (messageBodyWriter == null) {
                    messageBodyWriter = (MessageBodyWriter)messageBodyWriterFactory.getInstance(ApplicationContext.getCurrent());
                    instanceCache.put(messageBodyWriterClass, messageBodyWriter);
                }
                if (messageBodyWriter.isWriteable(type, genericType, annotations, WILDCARD_TYPE)) {
                    result.add(mediaType);
                }
            }
        }
        if (result.size() > 1) {
            Collections.sort(result, mediaTypeComparator);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    protected <T> ContextResolver<T> doGetContextResolver(Class<T> contextType, MediaType mediaType) {
        ObjectFactory<ProviderDescriptor> contextResolverFactory = null;
        Iterator<MediaType> mediaTypeRange = createDescendingMediaTypeIterator(mediaType);
        while (mediaTypeRange.hasNext() && contextResolverFactory == null) {
            contextResolverFactory = contextResolvers.get(ContextResolverKey.of(contextType, mediaTypeRange.next()));
        }
        if (contextResolverFactory == null) {
            return null;
        }
        return (ContextResolver<T>) contextResolverFactory.getInstance(ApplicationContext.getCurrent());
    }

    @SuppressWarnings("unchecked")
    protected <T extends Throwable> ExceptionMapper<T> doGetExceptionMapper(Class<T> errorType) {
        ObjectFactory objectFactory = exceptionMappers.get(errorType);
        if (objectFactory == null) {
            Class superclassOfErrorType = errorType.getSuperclass();
            while (objectFactory == null && superclassOfErrorType != Object.class) {
                objectFactory = exceptionMappers.get(superclassOfErrorType);
                superclassOfErrorType = superclassOfErrorType.getSuperclass();
            }
        }
        if (objectFactory == null) {
            return null;
        }
        return (ExceptionMapper<T>)objectFactory.getInstance(ApplicationContext.getCurrent());
    }

    /**
     * Looking for message body reader according to supplied entity class, entity generic type, annotations and content type.
     *
     * @param <T>         message body reader actual type argument
     * @param type        entity type
     * @param genericType entity generic type
     * @param annotations annotations
     * @param mediaType   entity content type
     * @return message body reader or null if no one was found.
     */
    @SuppressWarnings({"unchecked"})
    protected <T> MessageBodyReader<T> doGetMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        Iterator<MediaType> mediaTypeRange = createDescendingMediaTypeIterator(mediaType);
        Map<Class, MessageBodyReader> instanceCache = new HashMap<>();
        List<MessageBodyReader> matchedReaders = new ArrayList<>();
        while (mediaTypeRange.hasNext()) {
            MediaType actual = mediaTypeRange.next();
            Collection<ObjectFactory<ProviderDescriptor>> messageBodyReaderFactories = readProviders.get(actual);
            if (messageBodyReaderFactories != null) {
                for (ObjectFactory messageBodyReaderFactory : messageBodyReaderFactories) {
                    Class<?> messageBodyReaderClass = messageBodyReaderFactory.getObjectModel().getObjectClass();
                    MessageBodyReader messageBodyReader = instanceCache.get(messageBodyReaderClass);
                    if (messageBodyReader == null) {
                        messageBodyReader = (MessageBodyReader) messageBodyReaderFactory.getInstance(ApplicationContext.getCurrent());
                        instanceCache.put(messageBodyReaderClass, messageBodyReader);
                    }
                    if (messageBodyReader.isReadable(type, genericType, annotations, actual)) {
                        matchedReaders.add(messageBodyReader);
                    }
                }
            }
        }
        if (matchedReaders.isEmpty()) {
            return null;
        }
        if (matchedReaders.size() > 1) {
            Collections.sort(matchedReaders, comparing(this::getTypeSupportedByReader, new TypeProximityComparator(type)));
        }
        return matchedReaders.get(0);
    }

    private Type getTypeSupportedByReader(MessageBodyReader<?> reader) {
        Class readerSuperClass = reader.getClass();
        while (readerSuperClass != null) {
            for (Type anInterface : readerSuperClass.getGenericInterfaces()) {
                if (anInterface instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) anInterface;
                    if (parameterizedType.getRawType() == MessageBodyReader.class) {
                        return parameterizedType.getActualTypeArguments()[0];
                    }
                }
            }
            readerSuperClass = readerSuperClass.getSuperclass();
        }
        return null;
    }

    /**
     * Looking for message body writer according to supplied entity class, entity generic type, annotations and content type.
     *
     * @param <T>         message body writer actual type argument
     * @param type        entity type
     * @param genericType entity generic type
     * @param annotations annotations
     * @param mediaType   content type in which entity should be represented
     * @return message body writer or null if no one was found.
     */
    @SuppressWarnings({"unchecked"})
    protected <T> MessageBodyWriter<T> doGetMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        Iterator<MediaType> mediaTypeRange = createDescendingMediaTypeIterator(mediaType);
        Map<Class, MessageBodyWriter> instanceCache = new HashMap<>();
        List<MessageBodyWriter> matchedWriters = new ArrayList<>();
        while (mediaTypeRange.hasNext()) {
            MediaType actual = mediaTypeRange.next();
            Collection<ObjectFactory<ProviderDescriptor>> messageBodyWriterFactories = writeProviders.get(actual);
            if (messageBodyWriterFactories != null) {
                for (ObjectFactory messageBodyWriterFactory : messageBodyWriterFactories) {
                    Class<?> messageBodyWriterClass = messageBodyWriterFactory.getObjectModel().getObjectClass();
                    MessageBodyWriter messageBodyWriter = instanceCache.get(messageBodyWriterClass);
                    if (messageBodyWriter == null) {
                        messageBodyWriter = (MessageBodyWriter) messageBodyWriterFactory.getInstance(ApplicationContext.getCurrent());
                        instanceCache.put(messageBodyWriterClass, messageBodyWriter);
                    }
                    if (messageBodyWriter.isWriteable(type, genericType, annotations, actual)) {
                        matchedWriters.add(messageBodyWriter);
                    }
                }
            }
        }
        if (matchedWriters.isEmpty()) {
            return null;
        }
        if (matchedWriters.size() > 1) {
            Collections.sort(matchedWriters, comparing(this::getTypeSupportedByWriter, new TypeProximityComparator(type)));
        }
        return matchedWriters.get(0);
    }

    private Type getTypeSupportedByWriter(MessageBodyWriter writer) {
        Class writerSuperClass = writer.getClass();
        while (writerSuperClass != null) {
            for (Type anInterface : writerSuperClass.getGenericInterfaces()) {
                if (anInterface instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) anInterface;
                    if (parameterizedType.getRawType() == MessageBodyWriter.class) {
                        return parameterizedType.getActualTypeArguments()[0];
                    }
                }
            }
            writerSuperClass = writerSuperClass.getSuperclass();
        }
        return null;
    }

    /** Compares two types by their proximity in hierarchy to some type specified in constructor. */
    private static class TypeProximityComparator implements Comparator<Type> {
        private final Class<?> targetType;

        TypeProximityComparator(Class<?> targetType) {
            this.targetType = targetType;
        }

        @Override
        public int compare(Type typeOne, Type typeTwo) {
            if (!(typeOne instanceof Class) || !(typeTwo instanceof Class)) {
                return 0;
            }
            int inheritanceDepthOne = calculateInheritanceDepth((Class<?>) typeOne, targetType);
            int inheritanceDepthTwo = calculateInheritanceDepth((Class<?>) typeTwo, targetType);
            if (inheritanceDepthOne < 0 && inheritanceDepthTwo >= 0) {
                return 1;
            } else if (inheritanceDepthOne >= 0 && inheritanceDepthTwo < 0) {
                return -1;
            } else if (inheritanceDepthOne > inheritanceDepthTwo) {
                return 1;
            } else if (inheritanceDepthOne < inheritanceDepthTwo) {
                return -1;
            }
            return 0;
        }

        private int calculateInheritanceDepth(Class<?> inherited, Class<?> inheritor) {
            if (!inherited.isAssignableFrom(inheritor)) {
                return -1;
            }
            Class<?> superClass = inheritor;
            int depth = 0;
            while (superClass != null && superClass != inherited) {
                superClass = superClass.getSuperclass();
                depth++;
            }
            return depth;
        }
    }

    protected List<ReaderInterceptor> doGetReaderInterceptors() {
        ApplicationContext context = ApplicationContext.getCurrent();
        return readerInterceptors.stream()
                .map(factory -> (ReaderInterceptor) factory.getInstance(context))
                .collect(toList());
    }

    protected List<WriterInterceptor> doGetWriterInterceptors() {
        ApplicationContext context = ApplicationContext.getCurrent();
        return writerInterceptors.stream()
                .map(factory -> (WriterInterceptor) factory.getInstance(context))
                .collect(toList());
    }

    protected List<ClientRequestFilter> doGetClientRequestFilters() {
        ApplicationContext context = ApplicationContext.getCurrent();
        return clientRequestFilters.stream()
                .map(factory -> (ClientRequestFilter) factory.getInstance(context))
                .collect(toList());
    }

    protected List<ClientResponseFilter> doGetClientResponseFilters() {
        ApplicationContext context = ApplicationContext.getCurrent();
        return clientResponseFilters.stream()
                .map(factory -> (ClientResponseFilter) factory.getInstance(context))
                .collect(toList());
    }

    protected List<ContainerRequestFilter> doGetContainerRequestFilters(Annotation[] nameBindingAnnotations, boolean preMatching) {
        ApplicationContext context = ApplicationContext.getCurrent();
        return containerRequestFilters.stream()
                .filter(containerRequestFilterPredicate(nameBindingAnnotations, preMatching))
                .map(factory -> (ContainerRequestFilter) factory.getInstance(context))
                .collect(toList());
    }

    private Predicate<ObjectFactory<ProviderDescriptor>> containerRequestFilterPredicate(Annotation[] nameBindingAnnotations, boolean preMatching) {
        return factory -> {
            ProviderDescriptor objectModel = factory.getObjectModel();
            if (preMatching && objectModel.getObjectClass().isAnnotationPresent(PreMatching.class)) {
                return true;
            } else if (!preMatching) {
                if (nameBindingAnnotations.length == 0 && objectModel.getNameBindingAnnotations().length == 0) {
                    return true;
                } else if (!disjoint(nameBindingAnnotations, objectModel.getNameBindingAnnotations())) {
                    return true;
                }
            }
            return false;
        };
    }

    protected List<ContainerResponseFilter> doGetContainerResponseFilters(Annotation[] nameBindingAnnotations) {
        ApplicationContext context = ApplicationContext.getCurrent();
        return containerResponseFilters.stream()
                .filter(containerResponseFilterPredicate(nameBindingAnnotations))
                .map(factory -> (ContainerResponseFilter) factory.getInstance(context))
                .collect(toList());
    }

    private Predicate<ObjectFactory<ProviderDescriptor>> containerResponseFilterPredicate(Annotation[] nameBindingAnnotations) {
        return factory -> {
            ProviderDescriptor objectModel = factory.getObjectModel();
            if (nameBindingAnnotations.length == 0 && objectModel.getNameBindingAnnotations().length == 0) {
                return true;
            } else if (!disjoint(nameBindingAnnotations, objectModel.getNameBindingAnnotations())) {
                return true;
            }
            return false;
        };
    }

    protected List<DynamicFeature> doGetDynamicFeatures() {
        ApplicationContext context = ApplicationContext.getCurrent();
        return dynamicFeatures.stream()
                .map(factory -> (DynamicFeature) factory.getInstance(context))
                .collect(toList());
    }

    protected <T> ParamConverter<T> doGetConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        ParamConverter<T> converter = null;
        ApplicationContext context = ApplicationContext.getCurrent();
        for (Iterator<ObjectFactory<ProviderDescriptor>> iterator = paramConverterProviders.iterator(); iterator.hasNext() && converter == null; ) {
            converter = ((ParamConverterProvider)iterator.next().getInstance(context))
                    .getConverter(rawType, genericType, annotations);
        }
        return converter;
    }

    /** Clear all registered providers including embedded. */
    @Override
    public void clear() {
        exceptionMappers.clear();
        readProviders.clear();
        clientRequestFilters.clear();
        clientResponseFilters.clear();
        containerRequestFilters.clear();
        containerResponseFilters.clear();
        readerInterceptors.clear();
        writerInterceptors.clear();
        writeProviders.clear();
        contextResolvers.clear();
        paramConverterProviders.clear();
        features.clear();
        dynamicFeatures.clear();
        enabledFeatureClasses.clear();
        enabledFeatures.clear();
        allRegistrations.clear();
    }
}
