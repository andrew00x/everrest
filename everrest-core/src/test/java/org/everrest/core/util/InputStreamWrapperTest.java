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
package org.everrest.core.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStream;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InputStreamWrapperTest {
    @Mock private InputStream input;
    @Mock private Supplier<InputStream> supplier;
    @InjectMocks private InputStreamWrapper wrapper;

    @Before
    public void setUp() {
        when(supplier.get()).thenReturn(input);
    }

    @Test
    public void readsSingleByte() throws Exception {
        when(input.read()).thenReturn(8);
        assertEquals(8, wrapper.read());
    }

    @Test
    public void readsBytes() throws Exception {
        byte[] bytes = new byte[8];
        when(input.read(bytes)).thenReturn(8);
        assertEquals(8, wrapper.read(bytes));
    }

    @Test
    public void readsBytesWithOffset() throws Exception {
        byte[] bytes = new byte[8];
        when(input.read(bytes, 4, 2)).thenReturn(2);
        assertEquals(2, wrapper.read(bytes, 4, 2));
    }

    @Test
    public void skips() throws Exception {
        when(input.skip(8)).thenReturn(8L);
        assertEquals(8L, wrapper.skip(8));
    }

    @Test
    public void checksAvailable() throws Exception {
        when(input.available()).thenReturn(8);
        assertEquals(8, wrapper.available());
    }

    @Test
    public void closes() throws Exception {
        wrapper.close();
        verify(input).close();
    }

    @Test
    public void marks() throws Exception {
        wrapper.mark(8);
        verify(input).mark(8);
    }

    @Test
    public void resets() throws Exception {
        wrapper.reset();
        verify(input).reset();
    }

    @Test
    public void checksMarkSupported() throws Exception {
        when(input.markSupported()).thenReturn(true);
        assertTrue(wrapper.markSupported());
    }
}