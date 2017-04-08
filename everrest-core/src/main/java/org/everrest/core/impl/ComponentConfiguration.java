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

import java.util.Map;
import java.util.Objects;

import static java.util.Collections.unmodifiableMap;

public class ComponentConfiguration {
    private final Class<?> componentClass;
    private final Object component;
    private final Map<Class<?>, Integer> contracts;

    public ComponentConfiguration(Class<?> componentClass, Object component, Map<Class<?>, Integer> contracts) {
        this.componentClass = componentClass;
        this.component = component;
        this.contracts = unmodifiableMap(contracts);
    }

    public Class<?> getComponentClass() {
        return componentClass;
    }

    public Object getComponent() {
        return component;
    }

    public Map<Class<?>, Integer> getContracts() {
        return contracts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ComponentConfiguration)) {
            return false;
        }
        ComponentConfiguration other = (ComponentConfiguration) o;
        return Objects.equals(componentClass, other.componentClass)
                && Objects.equals(component, other.component)
                && Objects.equals(contracts, other.contracts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentClass, component, contracts);
    }
}
