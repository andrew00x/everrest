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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

public class SimpleConfigurationProperties extends ConfigurationProperties {
    private final Map<String, Object> properties = new HashMap<>();

    public SimpleConfigurationProperties() {
    }

    public SimpleConfigurationProperties(Map<String, Object> properties) {
        this.properties.putAll(properties);
    }

    public SimpleConfigurationProperties(ConfigurationProperties properties) {
        this(properties.getProperties());
    }

    @Override
    public Map<String, Object> getProperties() {
        return unmodifiableMap(properties);
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return unmodifiableCollection(properties.keySet());
    }

    @Override
    public void setProperty(String name, Object value) {
        requireNonNull(name);
        if (value == null) {
            removeProperty(name);
        } else {
            properties.put(name, value);
        }
    }

    @Override
    public void removeProperty(String name) {
        properties.remove(name);
    }
}
