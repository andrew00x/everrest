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

import org.everrest.core.impl.uri.UriComponent;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import java.io.IOException;

@PreMatching
public class UriNormalizeFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        request.setRequestUri(request.getUriInfo().getBaseUri(),
                UriComponent.normalize(request.getUriInfo().getRequestUri()));
    }
}
