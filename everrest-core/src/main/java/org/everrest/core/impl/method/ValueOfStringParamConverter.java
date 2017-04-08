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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Creates object from class which has method {@code valueOf} with single String argument.
 *
 * @author andrew00x
 */
public class ValueOfStringParamConverter implements ParamConverter<Object> {
    /** This method will be used for creation object. */
    private Method valueOfMethod;

    /**
     * @param valueOfMethod
     *         static method with single String parameter
     */
    public ValueOfStringParamConverter(Method valueOfMethod) {
        this.valueOfMethod = valueOfMethod;
    }

    @Override
    public Object fromString(String value) {
        checkArgument(value != null, "Null value is not supported");
        try {
            return valueOfMethod.invoke(null, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(String.format("Unable convert '%s' to %s", value, valueOfMethod.getDeclaringClass()), e);
        }
    }

    @Override
    public String toString(Object value) {
        checkArgument(value != null, "Null value is not supported");
        return String.valueOf(value);
    }
}
