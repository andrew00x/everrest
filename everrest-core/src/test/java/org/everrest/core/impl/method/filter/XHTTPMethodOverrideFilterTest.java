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

import org.everrest.core.impl.ApplicationContext;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.container.ContainerRequestContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class XHTTPMethodOverrideFilterTest {
    private ContainerRequestContext request;
    private XHTTPMethodOverrideFilter filter;

    @Before
    public void setUp() throws Exception {
        request = mock(ContainerRequestContext.class);
        ApplicationContext context = mock(ApplicationContext.class);
        ApplicationContext.setCurrent(context);

        filter = new XHTTPMethodOverrideFilter();
    }

    @Test
    public void overridesHttpMethodByValueFromXHTTPMethodOverrideHeader() {
        when(request.getHeaderString("X-HTTP-Method-Override")).thenReturn("POST");
        filter.filter(request);
        verify(request).setMethod("POST");
    }
}