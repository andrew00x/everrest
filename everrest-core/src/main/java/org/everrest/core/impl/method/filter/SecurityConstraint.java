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

import javax.annotation.security.DenyAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.lang.annotation.Annotation;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

public class SecurityConstraint implements ContainerRequestFilter {
    private Annotation annotation;

    public SecurityConstraint(Annotation annotation) {
        this.annotation = annotation;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Class<?> annotationType = annotation.annotationType();
        if (annotationType == DenyAll.class) {
            throw new WebApplicationException(Response.status(FORBIDDEN)
                    .entity("User not authorized to call this method").type(TEXT_PLAIN)
                    .build());
        } else if (annotationType == RolesAllowed.class) {
            SecurityContext security = requestContext.getSecurityContext();
            for (String role : ((RolesAllowed)annotation).value()) {
                if (security.isUserInRole(role)) {
                    return;
                }
            }
            throw new WebApplicationException(Response.status(FORBIDDEN)
                    .entity("User not authorized to call this method").type(TEXT_PLAIN)
                    .build());
        }
    }
}
