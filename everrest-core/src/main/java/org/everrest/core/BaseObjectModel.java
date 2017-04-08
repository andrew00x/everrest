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

import org.everrest.core.impl.ConstructorDescriptorImpl;
import org.everrest.core.impl.FieldInjectorImpl;
import org.everrest.core.impl.MultivaluedMapImpl;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NameBinding;
import javax.ws.rs.core.MultivaluedMap;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.everrest.core.util.ReflectionUtils.findAnnotationsAnnotatedWith;

public class BaseObjectModel implements ObjectModel {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(BaseObjectModel.class);

    private static final Comparator<ConstructorDescriptor> CONSTRUCTOR_COMPARATOR_BY_NUMBER_OF_PARAMETERS =
            new ConstructorComparatorByNumberOfParameters();

    /** Compare two ConstructorDescriptor in number parameters order. */
    private static class ConstructorComparatorByNumberOfParameters implements Comparator<ConstructorDescriptor> {
        @Override
        public int compare(ConstructorDescriptor constructorDescriptorOne, ConstructorDescriptor constructorDescriptorTwo) {
            int result = constructorDescriptorTwo.getParameters().size() - constructorDescriptorOne.getParameters().size();
            if (result == 0) {
                LOG.warn("Two constructors with the same number of parameter found {} and {}", constructorDescriptorOne, constructorDescriptorTwo);
            }
            return result;
        }
    }

    protected final Class<?>                    clazz;
    protected final List<ConstructorDescriptor> constructors;
    protected final List<FieldInjector>         fieldInjectors;
    protected final Annotation[]                nameBindingAnnotations;

    /** Optional properties. */
    private MultivaluedMapImpl properties;

    public BaseObjectModel(Object instance) {
        this(instance, null);
    }

    public BaseObjectModel(Object instance, Annotation[] applicationNameBindingAnnotations) {
        this(instance, instance.getClass(), applicationNameBindingAnnotations);
    }

    public BaseObjectModel(Class<?> aClass) {
        this(aClass, null);
    }

    public BaseObjectModel(Class<?> aClass, Annotation[] applicationNameBindingAnnotations) {
        this(null, aClass, applicationNameBindingAnnotations);

        processConstructors();
        sortConstructorByNumberOfParameters();
        processFields();
    }

    private BaseObjectModel(@SuppressWarnings("unused") Object instance, Class<?> aClass, Annotation[] applicationNameBindingAnnotations) {
        this.clazz = aClass;
        this.constructors = new ArrayList<>();
        this.fieldInjectors = new ArrayList<>();
        Set<Annotation> mergedBindingAnnotations = new HashSet<>();
        Collections.addAll(mergedBindingAnnotations, findAnnotationsAnnotatedWith(aClass, NameBinding.class));
        if (applicationNameBindingAnnotations != null) {
            Collections.addAll(mergedBindingAnnotations, applicationNameBindingAnnotations);
        }
        this.nameBindingAnnotations = mergedBindingAnnotations.toArray(new Annotation[mergedBindingAnnotations.size()]);

    }

    protected void processConstructors() {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            constructors.add(new ConstructorDescriptorImpl(constructor));
        }
        if (constructors.size() == 0) {
            throw new RuntimeException(String.format("Not found accepted constructors for provider class %s", clazz.getName()));
        }
    }

    private void sortConstructorByNumberOfParameters() {
        if (constructors.size() > 1) {
            Collections.sort(constructors, CONSTRUCTOR_COMPARATOR_BY_NUMBER_OF_PARAMETERS);
        }
    }

    protected void processFields() {
        for (Field field : clazz.getDeclaredFields()) {
            fieldInjectors.add(new FieldInjectorImpl(field));
        }
        Class<?> superclass = clazz.getSuperclass();
        while (superclass != null && superclass != Object.class) {
            for (Field field : superclass.getDeclaredFields()) {
                FieldInjector fieldInjector = new FieldInjectorImpl(field);
                if (fieldInjector.getAnnotation() != null) {
                    fieldInjectors.add(fieldInjector);
                }
            }
            superclass = superclass.getSuperclass();
        }
    }

    @Override
    public Class<?> getObjectClass() {
        return clazz;
    }

    @Override
    public List<ConstructorDescriptor> getConstructorDescriptors() {
        return constructors;
    }

    @Override
    public List<FieldInjector> getFieldInjectors() {
        return fieldInjectors;
    }

    @Override
    public MultivaluedMap<String, String> getProperties() {
        if (properties == null) {
            properties = new MultivaluedMapImpl();
        }
        return properties;
    }

    @Override
    public List<String> getProperty(String key) {
        if (properties != null) {
            return properties.get(key);
        }
        return null;
    }

    @Override
    public Annotation[] getNameBindingAnnotations() {
        return nameBindingAnnotations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseObjectModel)) {
            return false;
        }
        BaseObjectModel other = (BaseObjectModel) o;
        return Objects.equals(clazz, other.getObjectClass());
    }

    @Override
    public int hashCode() {
        int hashcode = 8;
        hashcode = 31 * hashcode + Objects.hash(clazz);
        return hashcode;
    }
}
