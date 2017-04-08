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

import javax.ws.rs.ext.ParamConverter;

import static com.google.common.base.Preconditions.checkArgument;

public class StringParamConverter implements ParamConverter<String> {
    @Override
    public String fromString(String value) {
        checkArgument(value != null, "Null value is not supported");
        return value;
    }

    @Override
    public String toString(String value) {
        checkArgument(value != null, "Null value is not supported");
        return value;
    }
}
