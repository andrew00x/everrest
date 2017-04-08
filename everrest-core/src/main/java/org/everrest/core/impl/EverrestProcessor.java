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
import org.everrest.core.DependencySupplier;
import org.everrest.core.GenericContainerRequest;
import org.everrest.core.GenericContainerResponse;
import org.everrest.core.Lifecycle;
import org.everrest.core.ProviderBinder;
import org.everrest.core.RequestHandler;
import org.everrest.core.ResourceBinder;
import org.everrest.core.impl.method.MethodInvokerDecoratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Application;
import java.io.IOException;

import static java.util.Objects.requireNonNull;
import static org.everrest.core.impl.ApplicationContext.anApplicationContext;
import static org.everrest.core.impl.ProcessingPhase.ENDED;
import static org.everrest.core.impl.ProcessingPhase.PRE_MATCHED;
import static org.everrest.core.impl.ServerConfigurationProperties.METHOD_INVOKER_DECORATOR_FACTORY;

/**
 * @author andrew00x
 */
public class EverrestProcessor implements Lifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(EverrestProcessor.class);

    private final DependencySupplier dependencySupplier;
    private final RequestHandler requestHandler;
    private final EverrestApplication everrestApplication;
    private final ServerConfigurationProperties configuration;
    private final ResourceBinder resources;
    private final ProviderBinder providers;

    private final MethodInvokerDecoratorFactory methodInvokerDecoratorFactory;

    public EverrestProcessor(DependencySupplier dependencySupplier,
                             RequestHandler requestHandler,
                             ResourceBinder resources,
                             ProviderBinder providers) {
        this(null, dependencySupplier, requestHandler, resources, providers, null);
    }

    public EverrestProcessor(ServerConfigurationProperties configuration,
                             DependencySupplier dependencySupplier,
                             RequestHandler requestHandler,
                             ResourceBinder resources,
                             ProviderBinder providers,
                             Application application) {
        this.configuration = configuration == null ? new ServerConfigurationProperties() : configuration;
        this.dependencySupplier = dependencySupplier;
        this.requestHandler = requestHandler;
        this.resources = resources;
        this.providers = providers;
        everrestApplication = new EverrestApplication();
        if (application != null) {
            addApplication(application);
        }
        methodInvokerDecoratorFactory = createMethodInvokerDecoratorFactory(this.configuration);
    }

    private MethodInvokerDecoratorFactory createMethodInvokerDecoratorFactory(ServerConfigurationProperties configuration) {
        String decoratorFactoryClassName = configuration.getStringProperty(METHOD_INVOKER_DECORATOR_FACTORY , null);
        if (decoratorFactoryClassName != null) {
            try {
                Class<?> decoratorFactoryClass = Thread.currentThread().getContextClassLoader().loadClass(decoratorFactoryClassName);
                return MethodInvokerDecoratorFactory.class.cast(decoratorFactoryClass.newInstance());
            } catch (Exception e) {
                throw new IllegalStateException(String.format("Cannot instantiate '%s', : %s", decoratorFactoryClassName, e), e);
            }
        }
        return null;
    }

    public void process(GenericContainerRequest request, GenericContainerResponse response, EnvironmentContext environmentContext) throws IOException {
        RuntimeConfigurationProperties runtimeProperties = new RuntimeConfigurationProperties(configuration);
        RuntimeProviderBinder runtimeProviders = new RuntimeProviderBinder(providers, runtimeProperties);
        ApplicationContext context = anApplicationContext()
                .withRequest(request)
                .withResponse(response)
                .withEnvironmentContext(environmentContext)
                .withProviders(runtimeProviders)
                .withApplication(everrestApplication)
                .withConfiguration(runtimeProperties)
                .withDependencySupplier(dependencySupplier)
                .withMethodInvokerDecoratorFactory(methodInvokerDecoratorFactory)
                .build();
        try {
            context.start();
            context.setProcessingPhase(PRE_MATCHED);
            ApplicationContext.setCurrent(context);
            requestHandler.handleRequest(request, response);
        } finally {
            try {
                context.stop();
                context.setProcessingPhase(ENDED);
            } finally {
                ApplicationContext.setCurrent(null);
            }
        }
    }

    public void addApplication(Application application) {
        requireNonNull(application);
        everrestApplication.addApplication(application);
        ApplicationPublisher applicationPublisher = new ApplicationPublisher(resources, providers);
        applicationPublisher.publish(application);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        for (Object singleton : everrestApplication.getSingletons()) {
            try {
                new LifecycleComponent(singleton).destroy();
            } catch (InternalException e) {
                LOG.error("Unable to destroy component", e);
            }
        }
    }

    public EverrestApplication getApplication() {
        return everrestApplication;
    }

    @VisibleForTesting
    MethodInvokerDecoratorFactory getMethodInvokerDecoratorFactory() {
        return methodInvokerDecoratorFactory;
    }
}
