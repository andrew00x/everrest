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
import org.everrest.core.ProviderBinder;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class EverrestConfiguration implements Configuration {
    private final ConfigurationProperties properties;
    private final ProviderBinder providers;

    public EverrestConfiguration(ProviderBinder providers, ConfigurationProperties properties) {
        this.providers = providers;
        this.properties = properties;
    }

    @Override
    public RuntimeType getRuntimeType() {
        return providers.getRuntimeType();
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties.getProperties();
    }

    @Override
    public Object getProperty(String name) {
        return properties.getProperty(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return properties.getPropertyNames();
    }

    @Override
    public boolean isEnabled(Feature feature) {
        return providers.isEnabled(feature);
    }

    @Override
    public boolean isEnabled(Class<? extends Feature> featureClass) {
        return providers.isEnabled(featureClass);
    }

    @Override
    public boolean isRegistered(Object component) {
        return providers.isRegistered(component);
    }

    @Override
    public boolean isRegistered(Class<?> componentClass) {
        return providers.isRegistered(componentClass);
    }

    @Override
    public Map<Class<?>, Integer> getContracts(Class<?> componentClass) {
        return providers.getContracts(componentClass);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return providers.getClasses();
    }

    @Override
    public Set<Object> getInstances() {
        return providers.getInstances();
    }

    public void setProperty(String name, Object value) {
        properties.setProperty(name, value);
    }

    public void removeProperty(String name) {
        properties.removeProperty(name);
    }

    public ConfigurationProperties getConfigurationProperties() {
        return properties;
    }
}
