/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors:
 * Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.everrest.core.impl.method;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.ext.ParamConverter;

import static org.junit.Assert.assertEquals;

@RunWith(DataProviderRunner.class)
public class PrimitiveTypeParamConverterTest {

    @DataProvider
    public static Object[][] testData() {
        return new Object[][]{
                {Boolean.TYPE, "true", true},
                {Boolean.TYPE, "false", false},
                {Byte.TYPE, "33", (byte) 33},
                {Character.TYPE, "A", 'A'},
                {Short.TYPE, "333", (short) 333},
                {Integer.TYPE, "33333", 33333},
                {Long.TYPE, "33333333333", 33333333333L},
                {Float.TYPE, "3.3", 3.3F},
                {Double.TYPE, "3.3", 3.3D}
        };
    }

    @UseDataProvider("testData")
    @Test
    public void convertsFromString(Class<?> primitiveTypeWrapper, String value, Object expected) throws Exception {
        ParamConverter converter = new PrimitiveTypeParamConverter(primitiveTypeWrapper);
        Object result = converter.fromString(value);
        assertEquals(expected, result);
    }

    @UseDataProvider("testData")
    @Test
    public void convertsToString(Class<?> primitiveTypeWrapper, String expected, Object value) throws Exception {
        ParamConverter converter = new PrimitiveTypeParamConverter(primitiveTypeWrapper);
        String result = converter.toString(value);
        assertEquals(expected, result);
    }
}
