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

import org.everrest.core.ProviderBinder;
import org.everrest.core.ResourceBinder;

import javax.ws.rs.Path;
import java.util.Set;

import static javax.ws.rs.RuntimeType.SERVER;

/**
 * @author andrew00x
 */
public class RestComponentResolver {
    private static Set<Class<?>> serverSideProviders = DefaultProviderBinder.getSupportedContracts(SERVER);

    private ResourceBinder resources;
    private ProviderBinder providers;

    public RestComponentResolver(ResourceBinder resources, ProviderBinder providers) {
        this.resources = resources;
        this.providers = providers;
    }

    @SuppressWarnings({"unchecked"})
    public void addSingleton(Object instance) {
        Class aClass = instance.getClass();
        if (isRootResource(aClass)) {
            resources.addResource(instance, null);
        } else if (isServerSideProvider(aClass)) {
            providers.register(instance);
        }
    }

    @SuppressWarnings({"unchecked"})
    public void addPerRequest(Class<?> aClass) {
        if (isRootResource(aClass)) {
            resources.addResource(aClass, null);
        } else if (isServerSideProvider(aClass)) {
            providers.register(aClass);
        }
    }

    public static boolean isRootResourceOrProvider(Class<?> aClass) {
        return isRootResource(aClass) || isServerSideProvider(aClass);
    }

    public static boolean isRootResource(Class<?> aClass) {
        return aClass.isAnnotationPresent(Path.class);
    }

    private static boolean isServerSideProvider(Class<?> aClass) {
        return serverSideProviders.stream().anyMatch(providerContract -> providerContract.isAssignableFrom(aClass));
    }
}
