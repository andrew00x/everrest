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

import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import static java.util.Collections.emptyEnumeration;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EverrestInitializedListenerTest {
    private ServletContext servletContext;

    @Before
    public void setUp() throws Exception {
        servletContext = mock(ServletContext.class);
        when(servletContext.getInitParameterNames()).thenReturn(emptyEnumeration());
    }

    @Test
    public void setsEverrestServletContextInitializerAsServletContextAttributeWhenServletContextInitialized() {
        EverrestInitializedListener everrestInitializedListener = new EverrestInitializedListener();
        ServletContextEvent servletContextEvent = new ServletContextEvent(servletContext);
        everrestInitializedListener.contextInitialized(servletContextEvent);

        verify(servletContext).setAttribute(eq(EverrestServletContextInitializer.class.getName()), isA(EverrestServletContextInitializer.class));
    }

    @Test
    public void removesEverrestServletContextInitializerFromServletContextAttributeWhenServletContextDestroyed() {
        EverrestServletContextInitializer initializer = mock(EverrestServletContextInitializer.class);
        when(servletContext.getAttribute(EverrestServletContextInitializer.class.getName())).thenReturn(initializer);

        EverrestInitializedListener everrestInitializedListener = new EverrestInitializedListener();
        ServletContextEvent servletContextEvent = new ServletContextEvent(servletContext);
        everrestInitializedListener.contextDestroyed(servletContextEvent);

        verify(servletContext).removeAttribute(EverrestServletContextInitializer.class.getName());
    }

    @Test
    public void createsEverrestProcessorWhenServletContextInitialized() throws Exception {
        EverrestServletContextInitializer initializer = mock(EverrestServletContextInitializer.class);

        EverrestInitializedListener everrestInitializedListener = new EverrestInitializedListener();
        everrestInitializedListener.initializeEverrestComponents(initializer);

        verify(initializer).createEverrestProcessor();
    }

    @Test
    public void destroysEverrestProcessorWhenServletContextDestroyed() throws Exception {
        EverrestServletContextInitializer initializer = mock(EverrestServletContextInitializer.class);
        when(servletContext.getAttribute(EverrestServletContextInitializer.class.getName())).thenReturn(initializer);

        EverrestInitializedListener everrestInitializedListener = new EverrestInitializedListener();
        ServletContextEvent servletContextEvent = new ServletContextEvent(servletContext);
        everrestInitializedListener.contextDestroyed(servletContextEvent);

        verify(initializer).destroyEverrestProcessor();
    }
}