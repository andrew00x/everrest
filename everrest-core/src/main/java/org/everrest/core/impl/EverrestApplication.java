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

import javax.ws.rs.core.Application;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Defines the JAX-RS components, it is uses as 'wrapper' for custom instance of Application.
 * <p/>
 * Usage:
 * <p/>
 * <pre>
 * EverrestProcessor processor = ...
 * Application app = ...
 * EverrestApplication everrest = new EverrestApplication();
 * ServerConfigurationProperties config = ...
 * ...
 * everrest.addApplication(app);
 * processor.addApplication(everrest);
 * </pre>
 *
 * @author andrew00x
 */
public class EverrestApplication extends Application {
    private final Set<Class<?>>         classes;
    private final Set<Object>           singletons;
    private final Map<String, Class<?>> resourceClasses;
    private final Map<String, Object>   resourceSingletons;

    public EverrestApplication() {
        classes = new LinkedHashSet<>();
        singletons = new LinkedHashSet<>();
        resourceClasses = new LinkedHashMap<>();
        resourceSingletons = new LinkedHashMap<>();
    }

    public void addClass(Class<?> clazz) {
        classes.add(clazz);
    }

    public void addSingleton(Object singleton) {
        singletons.add(singleton);
    }

    public void addResource(String uriPattern, Class<?> resourceClass) {
        resourceClasses.put(uriPattern, resourceClass);
    }

    public void addResource(String uriPattern, Object resource) {
        resourceSingletons.put(uriPattern, resource);
    }

    public Map<String, Class<?>> getResourceClasses() {
        return resourceClasses;
    }

    public Map<String, Object> getResourceSingletons() {
        return resourceSingletons;
    }

    /** @see javax.ws.rs.core.Application#getClasses() */
    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    /** @see javax.ws.rs.core.Application#getSingletons() */
    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

    /**
     * Add components defined by {@code application} to this instance.
     *
     * @param application
     *         application
     * @see javax.ws.rs.core.Application
     */
    public void addApplication(Application application) {
        if (application != null) {
            Set<Object> appSingletons = application.getSingletons();
            if (appSingletons != null && !appSingletons.isEmpty()) {
                Set<Object> allSingletons = new LinkedHashSet<>(this.singletons.size() + appSingletons.size());
                allSingletons.addAll(appSingletons);
                allSingletons.addAll(this.singletons);
                this.singletons.clear();
                this.singletons.addAll(allSingletons);
            }
            Set<Class<?>> appClasses = application.getClasses();
            if (appClasses != null && !appClasses.isEmpty()) {
                Set<Class<?>> allClasses = new LinkedHashSet<>(this.classes.size() + appClasses.size());
                allClasses.addAll(appClasses);
                allClasses.addAll(this.classes);
                this.classes.clear();
                this.classes.addAll(allClasses);
            }
            if (application instanceof EverrestApplication) {
                EverrestApplication everrest = (EverrestApplication)application;

                Map<String, Class<?>> appResourceClasses = everrest.getResourceClasses();
                if (!appResourceClasses.isEmpty()) {
                    Map<String, Class<?>> allResourceClasses = new LinkedHashMap<>(this.resourceClasses.size() + appResourceClasses.size());
                    allResourceClasses.putAll(appResourceClasses);
                    allResourceClasses.putAll(this.resourceClasses);
                    this.resourceClasses.clear();
                    this.resourceClasses.putAll(allResourceClasses);
                }

                Map<String, Object> appResourceSingletons = everrest.getResourceSingletons();
                if (!appResourceSingletons.isEmpty()) {
                    Map<String, Object> allResourceSingletons = new LinkedHashMap<>(this.resourceSingletons.size() + appResourceSingletons.size());
                    allResourceSingletons.putAll(appResourceSingletons);
                    allResourceSingletons.putAll(this.resourceSingletons);
                    this.resourceSingletons.clear();
                    this.resourceSingletons.putAll(allResourceSingletons);
                }
            }
        }
    }
}

