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

import org.everrest.core.ConfigurationProperties;
import org.everrest.core.SimpleConfigurationProperties;

import java.util.Collection;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class RuntimeConfigurationProperties extends ConfigurationProperties {
    private volatile boolean copied;
    private ConfigurationProperties properties;

    public RuntimeConfigurationProperties(ConfigurationProperties properties) {
        this.properties = requireNonNull(properties);
    }

    @Override
    public Map<String, Object> getProperties() {
        return get(false).getProperties();
    }

    @Override
    public Object getProperty(String name) {
        return get(false).getProperty(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return get(false).getPropertyNames();
    }

    @Override
    public void setProperty(String name, Object value) {
        get(true).setProperty(name, value);
    }

    @Override
    public void removeProperty(String name) {
        get(true).removeProperty(name);
    }

    private ConfigurationProperties get(boolean createCopy) {
        if (createCopy) {
            if (!copied) {
                synchronized (this) {
                    if (!copied) {
                        ConfigurationProperties copiedProviders = new SimpleConfigurationProperties(this.properties);
                        this.properties = copiedProviders;
                        copied = true;
                    }
                }
            }
        }
        return this.properties;
    }
}
