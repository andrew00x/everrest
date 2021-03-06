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

import com.google.inject.Module;
import com.google.inject.servlet.ServletModule;
import org.everrest.core.impl.EverrestProcessor;
import org.everrest.core.tools.ResourceLauncher;
import org.everrest.guice.servlet.EverrestGuiceContextListener;
import org.junit.After;
import org.junit.Before;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyEnumeration;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author andrew00x
 */
public abstract class BaseTest {
    private class Listener extends EverrestGuiceContextListener {
        protected ServletModule getServletModule() {
            return new ServletModule();
        }

        protected List<Module> getModules() {
            return BaseTest.this.getModules();
        }
    }

    protected EverrestProcessor processor;
    protected ResourceLauncher  launcher;

    private ServletContext servletContext;
    private Listener listener;
    private Map<String, Object> servletContextAttributes;

    @Before
    public void setUp() throws Exception {
        servletContextAttributes = new HashMap<>();
        mockServletContext();

        listener = new Listener();
        listener.contextInitialized(new ServletContextEvent(servletContext));

        processor = (EverrestProcessor)servletContext.getAttribute(EverrestProcessor.class.getName());
        launcher = new ResourceLauncher(processor);
    }

    private void mockServletContext() {
        servletContext = mock(ServletContext.class);
        when(servletContext.getInitParameterNames()).thenReturn(emptyEnumeration());
        doAnswer(invocation -> servletContextAttributes.put((String)invocation.getArguments()[0], invocation.getArguments()[1]))
                .when(servletContext).setAttribute(anyString(), any());
        when(servletContext.getAttribute(anyString()))
                .then(invocation -> servletContextAttributes.get((String) invocation.getArguments()[0]));
    }

    @After
    public void tearDown() throws Exception {
        listener.contextDestroyed(new ServletContextEvent(servletContext));
    }

    protected abstract List<Module> getModules();
}
