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
package org.everrest.core.impl.resource;

import org.everrest.core.resource.ResourceMethodDescriptor;

import javax.ws.rs.container.ResourceInfo;
import java.lang.reflect.Method;
import java.util.Objects;

public class DefaultResourceInfo implements ResourceInfo {
    static DefaultResourceInfo aResourceInfo(ResourceMethodDescriptor resourceMethodDescriptor) {
        return new DefaultResourceInfo(resourceMethodDescriptor.getParentResource().getObjectClass(), resourceMethodDescriptor.getMethod());
    }

    private final Class<?> resourceClass;
    private final Method resourceMethod;

    public DefaultResourceInfo(Class<?> resourceClass, Method resourceMethod) {
        this.resourceClass = resourceClass;
        this.resourceMethod = resourceMethod;
    }

    public DefaultResourceInfo(ResourceInfo other) {
        this.resourceClass = other.getResourceClass();
        this.resourceMethod = other.getResourceMethod();
    }

    @Override
    public Class<?> getResourceClass() {
        return resourceClass;
    }

    @Override
    public Method getResourceMethod() {
        return resourceMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultResourceInfo)) {
            return false;
        }
        DefaultResourceInfo other = (DefaultResourceInfo) o;
        return Objects.equals(resourceClass, other.resourceClass)
               && Objects.equals(resourceMethod, other.resourceMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceClass, resourceMethod);
    }
}
