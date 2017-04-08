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
package org.everrest.guice.servlet;

import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import org.everrest.core.DependencySupplier;
import org.everrest.core.ProviderBinder;
import org.everrest.core.ResourceBinder;
import org.everrest.core.impl.EverrestApplication;
import org.everrest.core.impl.EverrestProcessor;
import org.everrest.core.impl.FileCollectorDestroyer;
import org.everrest.core.impl.ServerConfigurationProperties;
import org.everrest.core.servlet.EverrestServletContextInitializer;
import org.everrest.guice.BindingPath;
import org.everrest.guice.EverrestConfigurationModule;
import org.everrest.guice.EverrestModule;
import org.everrest.guice.GuiceDependencySupplier;
import org.everrest.guice.GuiceObjectFactoryProducer;
import org.everrest.guice.GuiceRuntimeDelegateImpl;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.RuntimeDelegate;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.everrest.core.impl.RestComponentResolver.isRootResource;
import static org.everrest.core.impl.RestComponentResolver.isRootResourceOrProvider;

/**
 * @author andrew00x
 */
public abstract class EverrestGuiceContextListener extends GuiceServletContextListener {
    /**
     * Default EverrestGuiceContextListener implementation. It gets application's FQN from context-param
     * <i>javax.ws.rs.Application</i> and instantiate it. If such parameter is not specified then scan (if scanning is
     * enabled) web application's folders WEB-INF/classes and WEB-INF/lib for classes which contains JAX-RS annotations.
     * Interesting for three annotations {@link Path}, {@link Provider}. Scanning of JAX-RS
     * components is managed by context-param <i>org.everrest.scan.components</i>. This parameter must be <i>true</i> to enable
     * scanning.
     */
    public static class DefaultListener extends EverrestGuiceContextListener {
        @Override
        protected List<Module> getModules() {
            return Collections.emptyList();
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        super.contextInitialized(sce);
        ServletContext servletContext = sce.getServletContext();
        Injector injector = getInjector(servletContext);
        // use specific RuntimeDelegate instance which is able to work with guice rest service proxies.
        // (need for interceptors functionality)
        RuntimeDelegate.setInstance(new GuiceRuntimeDelegateImpl());
        DependencySupplier dependencySupplier = new GuiceDependencySupplier(injector);
        servletContext.setAttribute(DependencySupplier.class.getName(), dependencySupplier);

        ServerConfigurationProperties config = injector.getInstance(ServerConfigurationProperties.class);
        servletContext.setAttribute(ServerConfigurationProperties.class.getName(), config);

        EverrestServletContextInitializer initializer = new EverrestServletContextInitializer(servletContext);
        EverrestProcessor processor = initializer.createEverrestProcessor();
        ResourceBinder resources = initializer.createResourceBinder();
        ProviderBinder providers = initializer.createProviderBinder(config);
        GuiceObjectFactoryProducer objectFactoryProducer = new GuiceObjectFactoryProducer(injector);
        resources.setObjectFactoryProducer(objectFactoryProducer);
        providers.setObjectFactoryProducer(objectFactoryProducer);
        servletContext.setAttribute(EverrestServletContextInitializer.class.getName(), initializer);

        EverrestApplication guiceComponents = new EverrestApplication();
        processBindings(injector, guiceComponents);
        processor.addApplication(guiceComponents);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        makeFileCollectorDestroyer().stopFileCollector();
        ServletContext servletContext = sce.getServletContext();
        EverrestServletContextInitializer initializer = (EverrestServletContextInitializer) servletContext.getAttribute(EverrestServletContextInitializer.class.getName());
        if (initializer != null) {
            initializer.destroyEverrestProcessor();
            servletContext.removeAttribute(EverrestServletContextInitializer.class.getName());
        }
    }

    protected FileCollectorDestroyer makeFileCollectorDestroyer() {
        return new FileCollectorDestroyer();
    }

    @Override
    protected final Injector getInjector() {
        return Guice.createInjector(Stage.PRODUCTION, createModules());
    }

    private List<Module> createModules() {
        List<Module> all = new ArrayList<>();
        ServletModule servletModule = getServletModule();
        if (servletModule != null) {
            all.add(servletModule);
        }
        all.add(new EverrestModule());
        all.add(new EverrestConfigurationModule());
        List<Module> modules = getModules();
        if (modules != null && modules.size() > 0) {
            all.addAll(modules);
        }
        return all;
    }

    /**
     * Implementation can provide set of own {@link Module} for JAX-RS components.
     * <p/>
     * <pre>
     * protected List&lt;Module&gt; getModules()
     * {
     *    List&lt;Module&gt; modules = new ArrayList&lt;Module&gt;(1);
     *    modules.add(new Module()
     *    {
     *       public void configure(Binder binder)
     *       {
     *          binder.bind(MyResource.class);
     *          binder.bind(MyProvider.class);
     *       }
     *    });
     *    return modules;
     * }
     * </pre>
     *
     * @return JAX-RS modules
     */
    protected abstract List<Module> getModules();

    /**
     * Create servlet module. By default return module with one component GuiceEverrestServlet.
     *
     * @return ServletModule
     */
    protected ServletModule getServletModule() {
        return new ServletModule() {
            @Override
            protected void configureServlets() {
                serve("/*").with(GuiceEverrestServlet.class);
            }
        };
    }

    protected Injector getInjector(ServletContext servletContext) {
        return (Injector)servletContext.getAttribute(Injector.class.getName());
    }

    @SuppressWarnings({"unchecked"})
    protected void processBindings(Injector injector, EverrestApplication application) {
        for (Binding<?> binding : injector.getBindings().values()) {
            Key<?> bindingKey = binding.getKey();
            Type type = bindingKey.getTypeLiteral().getType();
            if (type instanceof Class) {
                Class aClass = (Class)type;
                if (isRootResourceOrProvider(aClass)) {
                    if (isRootResource(aClass)) {
                        Optional<String> resourcePath = getConfiguredResourcePath(bindingKey);
                        if (resourcePath.isPresent()) {
                            application.addResource(resourcePath.get(), aClass);
                        } else {
                            application.addClass(aClass);
                        }
                    } else {
                        application.addClass(aClass);
                    }
                }
            }
        }
    }

    private Optional<String> getConfiguredResourcePath(Key<?> bindingKey) {
        String path = null;
        if (bindingKey.getAnnotation() != null && bindingKey.getAnnotationType().isAssignableFrom(BindingPath.class)) {
            path = ((BindingPath) bindingKey.getAnnotation()).value();
        }
        return Optional.ofNullable(path);
    }
}
