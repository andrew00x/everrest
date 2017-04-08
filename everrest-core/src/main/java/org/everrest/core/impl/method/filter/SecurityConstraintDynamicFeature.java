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
package org.everrest.core.impl.method.filter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.everrest.core.impl.resource.DefaultResourceInfo;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.everrest.core.util.ReflectionUtils.getFirstPresentAnnotation;

/**
 * {@code DynamicFeature} that adds support for JSR-250 security annotations {@code PermitAll}, {@code DenyAll},
 * {@code RolesAllowed} on resource or sub-resource methods.
 *
 * @author andrew00x
 * @see PermitAll
 * @see DenyAll
 * @see RolesAllowed
 */
@Provider
public class SecurityConstraintDynamicFeature implements DynamicFeature {
        private final LoadingCache<ResourceInfo, Optional<Annotation>> securityAnnotationCache;

    public SecurityConstraintDynamicFeature() {
        securityAnnotationCache = CacheBuilder.newBuilder()
                .concurrencyLevel(16)
                .maximumSize(256)
                .expireAfterAccess(60, MINUTES)
                .build(new CacheLoader<ResourceInfo, Optional<Annotation>>() {
                    @Override
                    public Optional<Annotation> load(ResourceInfo resourceInfo) {
                        return Optional.ofNullable(getSecurityAnnotation(resourceInfo));
                    }
                });
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        ResourceInfo key = new DefaultResourceInfo(resourceInfo);
        Optional<Annotation> securityAnnotation = securityAnnotationCache.getUnchecked(key);
        if (securityAnnotation.isPresent()) {
            context.register(new SecurityConstraint(securityAnnotation.get()));
        }
    }

    private Annotation getSecurityAnnotation(ResourceInfo resourceInfo) {
        if (resourceInfo.getResourceMethod() == null) {
            return null; // Fake OPTIONS method. If resource does not have OPTIONS method we provide 'fake' method that generates WADL description of resource.
        }
        return getSecurityAnnotation(resourceInfo.getResourceClass(), resourceInfo.getResourceMethod());
    }

    /**
     * Get security annotation (DenyAll, RolesAllowed, PermitAll) from {@code method} or class which contains {@code method}.
     * Supper class or implemented interfaces will be also checked. Annotation on method has the advantage on
     * annotation on class or interface.
     *
     * @param method method to be checked for security annotation
     * @return one of security annotation or {@code null} is no such annotation found
     */
    @SuppressWarnings("unchecked")
    @VisibleForTesting
    Annotation getSecurityAnnotation(final Class<?> resourceClass, final Method method) {
        Class<? extends Annotation>[] securityAnnotationClasses = new Class[]{DenyAll.class, RolesAllowed.class, PermitAll.class};
        Annotation annotation = getFirstPresentAnnotation(method, securityAnnotationClasses);
        if (annotation == null) {
            annotation = getFirstPresentAnnotation(resourceClass, securityAnnotationClasses);
            if (annotation == null) {
                Method aMethod;
                Class<?> aClass = resourceClass;
                while (annotation == null && aClass != Object.class) {
                    Class<?>[] interfaces = aClass.getInterfaces();
                    for (int i = 0; annotation == null && i < interfaces.length; i++) {
                        try {
                            aMethod = interfaces[i].getDeclaredMethod(method.getName(), method.getParameterTypes());
                            annotation = getFirstPresentAnnotation(aMethod, securityAnnotationClasses);
                        } catch (NoSuchMethodException ignored) {
                        }
                        if (annotation == null) {
                            annotation = getFirstPresentAnnotation(interfaces[i], securityAnnotationClasses);
                        }
                    }
                    if (annotation == null) {
                        aClass = aClass.getSuperclass();
                        if (aClass != Object.class) {
                            try {
                                aMethod = aClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
                                annotation = getFirstPresentAnnotation(aMethod, securityAnnotationClasses);
                            } catch (NoSuchMethodException ignored) {
                            }
                            if (annotation == null) {
                                annotation = getFirstPresentAnnotation(aClass, securityAnnotationClasses);
                            }
                        }
                    }
                }
            }
        }
        return annotation;
    }
}
