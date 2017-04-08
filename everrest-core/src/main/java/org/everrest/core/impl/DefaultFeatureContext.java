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

import com.google.common.annotations.VisibleForTesting;
import org.everrest.core.ConfigurationProperties;
import org.everrest.core.ProviderBinder;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.FeatureContext;
import java.util.Map;

public class DefaultFeatureContext implements FeatureContext {
    private final ProviderBinder providers;
    private final ConfigurationProperties properties;

    public DefaultFeatureContext(ProviderBinder providers, ConfigurationProperties properties) {
        this.providers = providers;
        this.properties = properties;
    }

    @Override
    public Configuration getConfiguration() {
        return new EverrestConfiguration(providers, properties);
    }

    @Override
    public FeatureContext property(String name, Object value) {
        properties.setProperty(name, value);
        return this;
    }

    @Override
    public FeatureContext register(Class<?> componentClass) {
        providers.register(componentClass);
        return this;
    }

    @Override
    public FeatureContext register(Class<?> componentClass, int priority) {
        providers.register(componentClass, priority);
        return this;
    }

    @Override
    public FeatureContext register(Class<?> componentClass, Class<?>... contracts) {
        providers.register(componentClass, contracts);
        return this;
    }

    @Override
    public FeatureContext register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        providers.register(componentClass, contracts);
        return this;
    }

    @Override
    public FeatureContext register(Object component) {
        providers.register(component);
        return this;
    }

    @Override
    public FeatureContext register(Object component, int priority) {
        providers.register(component, priority);
        return null;
    }

    @Override
    public FeatureContext register(Object component, Class<?>... contracts) {
        providers.register(component, contracts);
        return this;
    }

    @Override
    public FeatureContext register(Object component, Map<Class<?>, Integer> contracts) {
        providers.register(component, contracts);
        return this;
    }

    @VisibleForTesting
    ConfigurationProperties getConfigurationProperties() {
        return properties;
    }

    @VisibleForTesting
    ProviderBinder getProviders() {
        return providers;
    }
}
