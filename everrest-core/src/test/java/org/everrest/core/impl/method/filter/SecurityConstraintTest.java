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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import static org.everrest.core.$matchers.ExceptionMatchers.webApplicationExceptionWithStatus;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SecurityConstraintTest {
    @Rule public ExpectedException thrown = ExpectedException.none();

    private PermitAll             permitAll;
    private DenyAll               denyAll;
    private RolesAllowed          rolesAllowed;
    private SecurityContext       securityContext;
    private ContainerRequestContext request;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        permitAll = mock(PermitAll.class);
        when(permitAll.annotationType()).thenReturn((Class)PermitAll.class);

        denyAll = mock(DenyAll.class);
        when(denyAll.annotationType()).thenReturn((Class)DenyAll.class);

        rolesAllowed = mock(RolesAllowed.class);
        when(rolesAllowed.annotationType()).thenReturn((Class)RolesAllowed.class);
        when(rolesAllowed.value()).thenReturn(new String[]{"user"});

        securityContext = mock(SecurityContext.class);
        request = mock(ContainerRequestContext.class);
        when(request.getSecurityContext()).thenReturn(securityContext);
    }

    @Test
    public void allowsAccessWhenPermitAllAnnotationPresents() throws Exception {
        SecurityConstraint securityConstraint = new SecurityConstraint(permitAll);
        securityConstraint.filter(request);
        verify(request, never()).getSecurityContext();
    }

    @Test
    public void denysAccessWhenDenyAllAnnotationPresents() throws Exception {
        SecurityConstraint securityConstraint = new SecurityConstraint(denyAll);
        thrown.expect(webApplicationExceptionWithStatus(Status.FORBIDDEN));

        securityConstraint.filter(request);
        verify(request, never()).getSecurityContext();
    }

    @Test
    public void allowsAccessWhenUserHasAcceptableRole() throws Exception {
        SecurityConstraint securityConstraint = new SecurityConstraint(rolesAllowed);
        when(securityContext.isUserInRole("user")).thenReturn(true);

        securityConstraint.filter(request);
        verify(request).getSecurityContext();
        verify(securityContext).isUserInRole("user");
    }

    @Test
    public void denysAccessWhenUserDoesNotHaveAcceptableRole() throws Exception {
        SecurityConstraint securityConstraint = new SecurityConstraint(rolesAllowed);
        when(securityContext.isUserInRole("user")).thenReturn(false);

        thrown.expect(webApplicationExceptionWithStatus(Status.FORBIDDEN));
        securityConstraint.filter(request);
        verify(request).getSecurityContext();
        verify(securityContext).isUserInRole("user");
    }
}