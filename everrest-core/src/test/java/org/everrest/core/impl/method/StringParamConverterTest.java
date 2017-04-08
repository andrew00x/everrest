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
package org.everrest.core.impl.method;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringParamConverterTest {
    private StringParamConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = new StringParamConverter();
    }

    @Test
    public void convertsStringToObject() {
        assertEquals("hello", converter.fromString("hello"));
    }

    @Test
    public void convertsObjectToString() {
        assertEquals("hello", converter.toString("hello"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionWhenConvertedStringIsNull() {
        converter.fromString(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionWhenConvertedObjectIsNull() {
        converter.toString(null);
    }
}