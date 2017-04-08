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

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import org.everrest.core.impl.ApplicationContext;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author andrew00x
 */
public class EverrestModule extends AbstractModule {
    public static class HttpHeadersProvider implements Provider<HttpHeaders> {
        @Override
        public HttpHeaders get() {
            ApplicationContext context = ApplicationContext.getCurrent();
            checkState(context != null, "ApplicationContext is not initialized yet");
            return context.getHttpHeaders();
        }
    }

    public static class ProvidersProvider implements Provider<Providers> {
        @Override
        public Providers get() {
            ApplicationContext context = ApplicationContext.getCurrent();
            checkState(context != null, "ApplicationContext is not initialized yet");
            return context.getProviders();
        }
    }

    public static class RequestProvider implements Provider<Request> {
        @Override
        public Request get() {
            ApplicationContext context = ApplicationContext.getCurrent();
            checkState(context != null, "ApplicationContext is not initialized yet");
            return context.getRequest();
        }
    }

    public static class SecurityContextProvider implements Provider<SecurityContext> {
        @Override
        public SecurityContext get() {
            ApplicationContext context = ApplicationContext.getCurrent();
            checkState(context != null, "ApplicationContext is not initialized yet");
            return context.getSecurityContext();
        }
    }

    public static class ServletConfigProvider implements Provider<ServletConfig> {
        @Override
        public ServletConfig get() {
            ApplicationContext context = ApplicationContext.getCurrent();
            checkState(context != null, "ApplicationContext is not initialized yet");
            return context.getEnvironmentContext().get(ServletConfig.class);
        }
    }

    public static class UriInfoProvider implements Provider<UriInfo> {
        @Override
        public UriInfo get() {
            ApplicationContext context = ApplicationContext.getCurrent();
            checkState(context != null, "ApplicationContext is not initialized yet");
            return context.getUriInfo();
        }
    }

    public static class ApplicationProvider implements Provider<Application> {
        @Override
        public Application get() {
            ApplicationContext context = ApplicationContext.getCurrent();
            checkState(context != null, "ApplicationContext is not initialized yet");
            return context.getApplication();
        }
    }

    /**
     * Add binding for HttpHeaders, InitialProperties, Providers, Request, SecurityContext, ServletConfig, UriInfo. All this types will be
     * supported for injection in constructor or fields of component of Guice container.
     *
     * @see javax.inject.Inject
     * @see com.google.inject.Inject
     */
    @Override
    protected void configure() {
        bind(HttpHeaders.class).toProvider(new HttpHeadersProvider());
        bind(Providers.class).toProvider(new ProvidersProvider());
        bind(Request.class).toProvider(new RequestProvider());
        bind(SecurityContext.class).toProvider(new SecurityContextProvider());
        bind(ServletConfig.class).toProvider(new ServletConfigProvider());
        bind(UriInfo.class).toProvider(new UriInfoProvider());
        bind(Application.class).toProvider(new ApplicationProvider());
    }
}
