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

import org.everrest.core.util.Tracer;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.everrest.core.ExtHttpHeaders.X_HTTP_METHOD_OVERRIDE;

@PreMatching
public class XHTTPMethodOverrideFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext request) {
        String method = request.getHeaderString(X_HTTP_METHOD_OVERRIDE);
        if (!isNullOrEmpty(method)) {
            if (Tracer.isTracingEnabled()) {
                Tracer.trace("Override HTTP method from \"X-HTTP-Method-Override\" header %s => %s", request.getMethod(), method);
            }
            request.setMethod(method);
        }
    }
}
