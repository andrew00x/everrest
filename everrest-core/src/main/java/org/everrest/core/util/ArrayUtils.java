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

import java.util.Objects;

public class ArrayUtils {
    /** Does the same as {@link java.util.Collections#disjoint(java.util.Collection, java.util.Collection)} but for arrays. */
    public static boolean disjoint(Object[] a1, Object[] a2) {
        if (a1.length == 0 && a2.length == 0) {
            return true;
        }
        for (Object o1 : a1) {
            for (Object o2 : a2) {
                if (Objects.equals(o1, o2)) {
                    return false;
                }
            }
        }

        return true;
    }
}
