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

import org.everrest.core.method.TypeProducer;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ParamConverter;

/**
 * Creates object from {@code String}
 *
 * @param <T> type of produced object
 */
public class DefaultTypeProducer<T> implements TypeProducer<T> {
    private final ParamConverter<T> paramConverter;

    /**
     * @param paramConverter {@code ParamConverter} which able to convert given {@code String} to appropriate type
     */
    DefaultTypeProducer(ParamConverter<T> paramConverter) {
        this.paramConverter = paramConverter;
    }

    @Override
    public T createValue(String param, MultivaluedMap<String, String> values, String defaultValue) throws Exception {
        T result = null;
        String value = values.getFirst(param);
        if (value != null) {
            result = paramConverter.fromString(value);
        } else if (defaultValue != null) {
            result = paramConverter.fromString(defaultValue);
        }
        return result;
    }
}
