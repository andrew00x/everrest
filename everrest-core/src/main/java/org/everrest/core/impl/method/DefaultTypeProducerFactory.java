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
package org.everrest.core.impl.method;

import org.everrest.core.impl.ApplicationContext;
import org.everrest.core.method.TypeProducer;
import org.everrest.core.method.TypeProducerFactory;

import javax.ws.rs.ext.ParamConverter;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

public class DefaultTypeProducerFactory implements TypeProducerFactory {
    @Override
    public <T> TypeProducer<T> createTypeProducer(Class<T> aClass, Type genericType, Annotation[] annotations) {
        ApplicationContext context = ApplicationContext.getCurrent();
        if (aClass == List.class || aClass == Set.class || aClass == SortedSet.class) {
            Class<?> actualTypeArgument = null;
            if (genericType != null) {
                actualTypeArgument = getActualTypeArgument(genericType);
            }
            if (actualTypeArgument == null) {
                actualTypeArgument = String.class;
            }
            ParamConverter<?> converter = context.getProviders().getConverter(actualTypeArgument, actualTypeArgument, annotations);
            if (converter != null) {
                Supplier<? extends Collection<?>> collectionFactory = null;
                if (aClass == List.class) {
                    collectionFactory = ArrayList::new;
                } else if (aClass == Set.class) {
                    collectionFactory = LinkedHashSet::new;
                } else if (aClass == SortedSet.class) {
                    collectionFactory = TreeSet::new;
                }
                return new CollectionTypeProducer(collectionFactory, converter);
            }
        } else {
            ParamConverter<T> converter = context.getProviders().getConverter(aClass, genericType, annotations);
            if (converter != null) {
                return new DefaultTypeProducer<>(converter);
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported type %s", aClass));
    }

    /**
     * Get actual type argument for supplied type.
     *
     * @param type generic {@code Type}
     * @return first actual type argument if type is {@link ParameterizedType} or {@code null} otherwise
     */
    private Class<?> getActualTypeArgument(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length == 1) {
                try {
                    return (Class<?>)actualTypeArguments[0];
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException(String.format("Unsupported type %s", actualTypeArguments[0]));
                }
            }
        }
        return null;
    }
}
