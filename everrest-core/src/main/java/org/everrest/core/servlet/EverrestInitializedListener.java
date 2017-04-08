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

import com.google.common.annotations.VisibleForTesting;
import org.everrest.core.impl.FileCollectorDestroyer;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Initialize required components of JAX-RS framework and deploy single JAX-RS application.
 *
 * @author andrew00x
 */
public class EverrestInitializedListener implements ServletContextListener {

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

    private FileCollectorDestroyer makeFileCollectorDestroyer() {
        return new FileCollectorDestroyer();
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        EverrestServletContextInitializer initializer = new EverrestServletContextInitializer(servletContext);
        initializeEverrestComponents(initializer);
        servletContext.setAttribute(EverrestServletContextInitializer.class.getName(), initializer);
    }

    @VisibleForTesting
    void initializeEverrestComponents(EverrestServletContextInitializer initializer) {
        initializer.createEverrestProcessor();
    }
}
