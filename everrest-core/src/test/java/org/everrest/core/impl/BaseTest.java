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
import org.everrest.core.impl.async.AsynchronousJobService;
import org.everrest.core.impl.provider.ServerEmbeddedProvidersFeature;
import org.everrest.core.tools.DependencySupplierImpl;
import org.everrest.core.tools.ResourceLauncher;
import org.junit.After;
import org.junit.Before;

import static javax.ws.rs.RuntimeType.SERVER;

/**
 * @author andrew00x
 */
public abstract class BaseTest {
    protected DependencySupplierImpl dependencySupplier;
    protected EverrestProcessor      processor;
    protected ProviderBinder providers;
    protected ResourceLauncher       launcher;

    @Before
    public void setUp() throws Exception {
        ResourceBinderImpl resources = new ResourceBinderImpl();
        providers = new DefaultProviderBinder(SERVER, new ServerConfigurationProperties());
        providers.register(new ServerEmbeddedProvidersFeature());
        resources.addResource("/async", AsynchronousJobService.class, null);
        dependencySupplier = new DependencySupplierImpl();
        processor = new EverrestProcessor(new ServerConfigurationProperties(), dependencySupplier, new RequestHandlerImpl(new RequestDispatcher(resources), providers), resources, providers, null);
        launcher = new ResourceLauncher(processor);
    }

    @After
    public void tearDown() throws Exception {
        processor.stop();
    }
}
