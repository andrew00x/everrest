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
package org.everrest.core.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

public class ReflectionUtils {

    /**
     * Get static {@link Method} with single string argument and name 'valueOf' for supplied class.
     *
     * @param clazz class for discovering to have public static method with name 'valueOf' and single string argument
     * @return valueOf method or {@code null} if class has not it
     */
    public static Method getStringValueOfMethod(Class<?> clazz) {
        try {
            Method method = clazz.getDeclaredMethod("valueOf", String.class);
            return Modifier.isStatic(method.getModifiers()) ? method : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get constructor with single string argument for supplied class.
     *
     * @param clazz class for discovering to have constructor with single string argument
     * @return constructor or {@code null} if class has not constructor with single string argument
     */
    public static Constructor<?> getStringConstructor(Class<?> clazz) {
        try {
            return clazz.getConstructor(String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get all interfaces implemented by specified {@code aClass} directly and non-directly.
     */
    public static Set<Class<?>> getImplementedInterfaces(Class<?> aClass) {
        Set<Class<?>> interfaces = new HashSet<>();
        addImplementedInterfaces(aClass, interfaces);
        return interfaces;
    }

    private static void addImplementedInterfaces(Class<?> aClass, Set<Class<?>> interfaces) {
        for (Class<?> anInterface : aClass.getInterfaces()) {
            interfaces.add(anInterface);
            addImplementedInterfaces(anInterface, interfaces);
        }
        if (aClass.getSuperclass() != null) {
            addImplementedInterfaces(aClass.getSuperclass(), interfaces);
        }
    }

    public static ParameterizedType getParameterizedType(Class<?> aClass, Class<?> rawType) {
        for (Type type : aClass.getGenericInterfaces()) {
            if (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() == rawType) {
                return (ParameterizedType) type;
            }
        }
        Type genericSuperclass = aClass.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType && ((ParameterizedType) genericSuperclass).getRawType() == rawType) {
            return (ParameterizedType) genericSuperclass;
        }
        if (aClass.getSuperclass() != null) {
            getParameterizedType(aClass.getSuperclass(), rawType);
        }
        return null;
    }

    public static <T extends Annotation> Annotation findFirstAnnotationAnnotatedWith(AnnotatedElement annotatedElement, Class<T> annotationClass) {
        for (Annotation annotation : annotatedElement.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(annotationClass)) {
                return annotation;
            }
        }
        return null;
    }

    public static <T extends Annotation> Annotation[] findAnnotationsAnnotatedWith(AnnotatedElement annotatedElement, Class<T> annotationClass) {
        return Arrays.stream(annotatedElement.getAnnotations())
                .filter(annotation -> annotation.annotationType().isAnnotationPresent(annotationClass))
                .toArray(Annotation[]::new);
    }

    public static <T extends Collection<?>> Class<T> findAcceptableCollectionImplementation(Class<T> collectionInterface) {
        Class impl = null;
        if (collectionInterface.isAssignableFrom(ArrayList.class)) {
            impl = ArrayList.class.asSubclass(collectionInterface);
        } else if (collectionInterface.isAssignableFrom(HashSet.class)) {
            impl = HashSet.class.asSubclass(collectionInterface);
        } else if (collectionInterface.isAssignableFrom(TreeSet.class)) {
            impl = TreeSet.class.asSubclass(collectionInterface);
        } else if (collectionInterface.isAssignableFrom(LinkedList.class)) {
            impl = LinkedList.class.asSubclass(collectionInterface);
        }
        if (impl == null) {
            throw new IllegalArgumentException(String.format("Can't find proper implementation for collection %s", collectionInterface));
        }
        return impl;
    }

    public static Annotation getFirstPresentAnnotation(AnnotatedElement annotated, Class<? extends Annotation>... possibleAnnotationClasses) {
        Annotation annotation = null;
        for (int i = 0; annotation == null && i < possibleAnnotationClasses.length; i++) {
            annotation = annotated.getAnnotation(possibleAnnotationClasses[i]);
        }
        return annotation;
    }

    private ReflectionUtils() {
    }
}
