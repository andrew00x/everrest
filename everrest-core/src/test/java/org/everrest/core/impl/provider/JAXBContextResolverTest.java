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
package org.everrest.core.impl.provider;

import org.everrest.core.generated.Book;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class JAXBContextResolverTest {

    private JAXBContextResolver jaxbContextResolver;

    @Before
    public void setUp() throws Exception {
        jaxbContextResolver = new JAXBContextResolver();
    }

    @Test
    public void getsJAXBContextResolverAsContextResolver() throws Exception {
        JAXBContextResolver contextResolver = jaxbContextResolver.getContext(null);
        assertSame(jaxbContextResolver, contextResolver);
    }

    @Test
    public void returnsJAXBContextForClass() throws Exception {
        assertNotNull(jaxbContextResolver.getJAXBContext(Book.class));
    }
}