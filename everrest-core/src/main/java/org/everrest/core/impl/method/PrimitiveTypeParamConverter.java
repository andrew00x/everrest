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

import com.google.common.collect.ImmutableMap;

import javax.ws.rs.ext.ParamConverter;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static org.everrest.core.util.ReflectionUtils.getStringValueOfMethod;

/**
 * Converts a primitive type from/to String value.
 */
public class PrimitiveTypeParamConverter implements ParamConverter<Object> {
    private static final Map<String, Class<?>> PRIMITIVE_TYPES_WRAPPERS = ImmutableMap.<String, Class<?>>builder()
            .put("boolean", Boolean.class)
            .put("byte", Byte.class)
            .put("char", Character.class)
            .put("short", Short.class)
            .put("int", Integer.class)
            .put("long", Long.class)
            .put("float", Float.class)
            .put("double", Double.class)
            .build();

    /** Class of object which is supported by this converter. */
    private Class<?> clazz;

    public PrimitiveTypeParamConverter(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Object fromString(String value) {
        checkArgument(value != null, "Null value is not supported");
        Class<?> primitiveWrapper = PRIMITIVE_TYPES_WRAPPERS.get(clazz.getName());
        if (primitiveWrapper == Character.class) {
            if (value.length() > 1) {
                throw new IllegalArgumentException(String.format("Unable convert '%s' to single character", value));
            } else if (value.length() == 1) {
                return value.charAt(0);
            } else {
                return null;
            }
        }
        try {
            return getStringValueOfMethod(primitiveWrapper).invoke(null, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(String.format("Unable convert '%s' to %s", value, clazz.getName()), e);
        }
    }

    @Override
    public String toString(Object value) {
        checkArgument(value != null, "Null value is not supported");
        Class<?> primitiveWrapper = PRIMITIVE_TYPES_WRAPPERS.get(clazz.getName());
        try {
            return (String) primitiveWrapper.getDeclaredMethod("toString", clazz).invoke(null, value);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(String.format("Unable convert '%s' to String", value), e);
        }
    }
}
