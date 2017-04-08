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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArrayUtilsTest {
    @Test
    public void testsDisjointReturnsTrueWhenTwoArraysHaveNoCommonElements() {
        Object[] array1 = new Object[]{"a", "b"};
        Object[] array2 = new Object[]{"x", "y"};
        assertTrue(ArrayUtils.disjoint(array1, array2));
    }

    @Test
    public void testsDisjointReturnsFalseWhenTwoArraysHaveAtLeastOneCommonElement() {
        Object[] array1 = new Object[]{"a", "b"};
        Object[] array2 = new Object[]{"x", "a", "z"};
        assertFalse(ArrayUtils.disjoint(array1, array2));
    }
}