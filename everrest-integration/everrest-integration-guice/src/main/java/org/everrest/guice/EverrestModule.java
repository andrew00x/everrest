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
package org.everrest.guice;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.AbstractModule;
import com.google.inject.MembersInjector;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.everrest.core.BaseObjectModel;
import org.everrest.core.FieldInjector;
import org.everrest.core.impl.ApplicationContext;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static org.everrest.core.impl.RestComponentResolver.isRootResourceOrProvider;

/**
 * @author andrew00x
 */
public class EverrestModule extends AbstractModule {
    @Provides
    public HttpHeaders getHttpHeaders() {
        return getApplicationContextOrFailWhenNotSet().getHttpHeaders();
    }

    @Provides
    public Providers getProviders() {
        return getApplicationContextOrFailWhenNotSet().getProviders();
    }

    @Provides
    public Request getRequest() {
        return getApplicationContextOrFailWhenNotSet().getRequest();
    }

    @Provides
    public SecurityContext getSecurityContext() {
        return getApplicationContextOrFailWhenNotSet().getSecurityContext();
    }

    @Provides
    public ServletConfig getServletConfig() {
        return getApplicationContextOrFailWhenNotSet().getEnvironmentContext().get(ServletConfig.class);
    }

    @Provides
    public UriInfo getUriInfo() {
        return getApplicationContextOrFailWhenNotSet().getUriInfo();
    }

    @Provides
    public Application getApplication() {
        return getApplicationContextOrFailWhenNotSet().getApplication();
    }

    private static ApplicationContext getApplicationContextOrFailWhenNotSet() {
        ApplicationContext context = ApplicationContext.getCurrent();
        checkState(context != null, "ApplicationContext is not initialized yet");
        return context;
    }

    private final LoadingCache<Class<?>, List<FieldInjector>> fieldsCache;

    public EverrestModule() {
        fieldsCache = CacheBuilder.newBuilder()
                .weakKeys()
                .build(new CacheLoader<Class<?>, List<FieldInjector>>() {
                    @Override
                    public List<FieldInjector> load(Class<?> aClass) {
                        return new BaseObjectModel(aClass).getFieldInjectors();
                    }
                });
    }

    /**
     * Add binding for HttpHeaders, InitialProperties, Providers, Request, SecurityContext, ServletConfig, UriInfo. All this types will be
     * supported for injection in constructor or fields of component of Guice container. Injections of other data from request,
     * e.g. &#064;PathParam, &#064;QueryParam, &#064;HeaderParam, etc is supported only for fields of pr-request resources and providers.
     *
     * @see javax.inject.Inject
     * @see com.google.inject.Inject
     */
    @Override
    protected void configure() {
        bindListener(REST_MATCHER, new TypeListener() {
            @Override
            public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
                encounter.register(new MembersInjector<I>() {
                    @Override
                    public void injectMembers(I instance) {
                        fieldsCache.getUnchecked(type.getRawType()).stream()
                                .filter(injector -> injector.getAnnotation() != null)
                                .forEach(injector -> injector.inject(instance, getApplicationContextOrFailWhenNotSet()));
                    }
                });
            }
        });
    }

    private static final Matcher<TypeLiteral<?>> REST_MATCHER = new RestComponentMatcher();

    private static class RestComponentMatcher extends AbstractMatcher<TypeLiteral<?>> {
        @Override
        public boolean matches(TypeLiteral<?> type) {
            return isRootResourceOrProvider(type.getRawType());
        }
    }
}
