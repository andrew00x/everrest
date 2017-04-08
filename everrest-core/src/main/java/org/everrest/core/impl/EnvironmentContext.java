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

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps objects from environment (e. g. servlet container) which can be passed
 * in resource. Parameter or field must be annotated by {@link javax.ws.rs.core.Context}.
 */
public class EnvironmentContext {
    private final Map<Class<?>, Object> store = new HashMap<>();

    public <T, I extends T> void put(Class<T> aClass, I instance) {
        store.put(aClass, instance);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> aClass) {
        return (T) store.get(aClass);
    }
}
