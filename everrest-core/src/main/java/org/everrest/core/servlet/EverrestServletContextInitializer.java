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
package org.everrest.core.servlet;

import com.google.common.base.Throwables;
import org.everrest.core.DependencySupplier;
import org.everrest.core.ProviderBinder;
import org.everrest.core.RequestHandler;
import org.everrest.core.ResourceBinder;
import org.everrest.core.impl.DefaultProviderBinder;
import org.everrest.core.impl.EverrestApplication;
import org.everrest.core.impl.EverrestProcessor;
import org.everrest.core.impl.RequestDispatcher;
import org.everrest.core.impl.RequestHandlerImpl;
import org.everrest.core.impl.ResourceBinderImpl;
import org.everrest.core.impl.ServerConfigurationProperties;
import org.everrest.core.impl.async.AsynchronousJobService;
import org.everrest.core.impl.provider.ServerEmbeddedProvidersFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.RuntimeType.SERVER;

/** @author andrew00x */
public class EverrestServletContextInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(EverrestServletContextInitializer.class);

    public static final String EVERREST_SCAN_COMPONENTS    = "org.everrest.scan.components";
    public static final String EVERREST_SCAN_SKIP_PACKAGES = "org.everrest.scan.skip.packages";
    public static final String JAXRS_APPLICATION           = "javax.ws.rs.Application";

    protected final ServletContext servletContext;

    public EverrestServletContextInitializer(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * Try get application's FQN from context-param javax.ws.rs.Application and instantiate it. If such parameter is not
     * specified then scan web application's folders WEB-INF/classes and WEB-INF/lib for classes which contains JAX-RS
     * annotations. Interesting for three annotations {@link Path}, {@link Provider}.
     *
     * @return instance of javax.ws.rs.core.Application
     */
    public Application getApplication() {
        Application application = null;
        String applicationFQN = getParameter(JAXRS_APPLICATION);
        boolean scan = getBooleanParameter(EVERREST_SCAN_COMPONENTS, false);
        if (applicationFQN != null) {
            if (scan) {
                LOG.warn("Scan of JAX-RS components is disabled cause to specified 'javax.ws.rs.Application'.");
            }
            try {
                Class<?> cl = Thread.currentThread().getContextClassLoader().loadClass(applicationFQN);
                application = (Application)cl.newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                Throwables.propagate(e);
            }
        } else if (scan) {
            application = new Application() {
                @Override
                public Set<Class<?>> getClasses() {
                    return new LinkedHashSet<>(ComponentFinder.findComponents());
                }
            };
        }
        return application;
    }

    public synchronized ServerConfigurationProperties createConfiguration() {
        ServerConfigurationProperties configuration = (ServerConfigurationProperties) servletContext.getAttribute(ServerConfigurationProperties.class.getName());
        if (configuration == null) {
            configuration = new ServerConfigurationProperties();
            for (String parameterName : getParameterNames()) {
                configuration.setProperty(parameterName, getParameter(parameterName));
            }
            servletContext.setAttribute(ServerConfigurationProperties.class.getName(), configuration);
        }
        return configuration;
    }

    public synchronized RequestHandler createRequestHandler(RequestDispatcher dispatcher, ProviderBinder providers) {
        RequestHandler handler = (RequestHandler) servletContext.getAttribute(RequestHandler.class.getName());
        if (handler == null) {
            handler = new RequestHandlerImpl(dispatcher, providers);
            servletContext.setAttribute(RequestHandler.class.getName(), handler);
        }
        return handler;
    }

    public synchronized RequestDispatcher createRequestDispatcher(ResourceBinder resources) {
        RequestDispatcher dispatcher = (RequestDispatcher) servletContext.getAttribute(RequestDispatcher.class.getName());
        if (dispatcher == null) {
            dispatcher = new RequestDispatcher(resources);
            servletContext.setAttribute(RequestDispatcher.class.getName(), dispatcher);
        }
        return dispatcher;
    }

    public synchronized ResourceBinder createResourceBinder() {
        ResourceBinder resources = (ResourceBinder) servletContext.getAttribute(ResourceBinder.class.getName());
        if (resources == null) {
            resources = new ResourceBinderImpl();
            servletContext.setAttribute(ResourceBinder.class.getName(), resources);
        }
        return resources;
    }

    public synchronized DependencySupplier createDependencySupplier() {
        DependencySupplier dependencySupplier = (DependencySupplier)servletContext.getAttribute(DependencySupplier.class.getName());
        if (dependencySupplier == null) {
            dependencySupplier = new ServletContextDependencySupplier(servletContext);
            servletContext.setAttribute(DependencySupplier.class.getName(), dependencySupplier);
        }
        return dependencySupplier;
    }

    public synchronized ProviderBinder createProviderBinder(ServerConfigurationProperties configuration) {
        ProviderBinder providers = (ProviderBinder) servletContext.getAttribute(ProviderBinder.class.getName());
        if (providers == null) {
            providers = new DefaultProviderBinder(SERVER, configuration);
            providers.register(new ServerEmbeddedProvidersFeature());
            servletContext.setAttribute(ProviderBinder.class.getName(), providers);
        }
        return providers;
    }

    public synchronized EverrestApplication createEverrestApplication(ServerConfigurationProperties configuration) {
        EverrestApplication everrest = (EverrestApplication) servletContext.getAttribute(Application.class.getName());
        if (everrest == null) {
            everrest = new EverrestApplication();
            servletContext.setAttribute(Application.class.getName(), everrest);
            if (configuration.isAsynchronousEnabled()) {
                everrest.addResource(configuration.getAsynchronousServicePath(), AsynchronousJobService.class);
            }
            everrest.addApplication(getApplication());
        }
        return everrest;
    }

    public synchronized EverrestProcessor createEverrestProcessor() {
        EverrestProcessor processor = (EverrestProcessor) servletContext.getAttribute(EverrestProcessor.class.getName());
        if (processor == null) {
            ServerConfigurationProperties configuration = createConfiguration();
            ResourceBinder resources = createResourceBinder();
            ProviderBinder providers = createProviderBinder(configuration);
            RequestDispatcher dispatcher = createRequestDispatcher(resources);
            processor = new EverrestProcessor(configuration,
                                              createDependencySupplier(),
                                              createRequestHandler(dispatcher, providers),
                                              resources,
                                              providers,
                                              createEverrestApplication(configuration));
            processor.start();
            servletContext.setAttribute(EverrestProcessor.class.getName(), processor);
        }
        return processor;
    }

    public synchronized void destroyEverrestProcessor() {
        EverrestProcessor processor = (EverrestProcessor) servletContext.getAttribute(EverrestProcessor.class.getName());
        if (processor != null) {
            processor.stop();
            servletContext.removeAttribute(EverrestProcessor.class.getName());
        }
        servletContext.removeAttribute(ServerConfigurationProperties.class.getName());
        servletContext.removeAttribute(RequestHandler.class.getName());
        servletContext.removeAttribute(RequestDispatcher.class.getName());
        servletContext.removeAttribute(ResourceBinder.class.getName());
        servletContext.removeAttribute(DependencySupplier.class.getName());
        servletContext.removeAttribute(ProviderBinder.class.getName());
        servletContext.removeAttribute(Application.class.getName());
    }

    protected List<String> getParameterNames() {
        return Collections.list(servletContext.getInitParameterNames());
    }

    /**
     * Get parameter with specified name from servlet context initial parameters.
     *
     * @param name parameter name
     * @return value of parameter with specified name
     */
    protected String getParameter(String name) {
        String value = servletContext.getInitParameter(name);
        if (value != null) {
            return value.trim();
        }
        return null;
    }

    protected boolean getBooleanParameter(String name, boolean def) {
        String str = getParameter(name);
        if (str != null) {
            return "true".equalsIgnoreCase(str) || "yes".equalsIgnoreCase(str) || "on".equalsIgnoreCase(str) || "1".equals(str);
        }
        return def;
    }
}
