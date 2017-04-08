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

import javax.ws.rs.ext.ParamConverter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Creates object from class which has constructor with single String argument.
 *
 * @author andrew00x
 */
public class ConstructorStringParamConverter<T> implements ParamConverter<T> {
    /** This constructor will be used for creation object. */
    private Constructor<T> constructor;

    /**
     * @param constructor constructor with single String argument
     */
    public ConstructorStringParamConverter(Constructor<T> constructor) {
        this.constructor = constructor;
    }

    @Override
    public T fromString(String value) {
        checkArgument(value != null, "Null value is not supported");
        try {
            return constructor.newInstance(value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (InstantiationException | InvocationTargetException e) {
            throw new IllegalArgumentException(String.format("Unable convert '%s' to %s", value, constructor.getDeclaringClass()), e);
        }
    }

    @Override
    public String toString(T value) {
        checkArgument(value != null, "Null value is not supported");
        return String.valueOf(value);
    }
}
