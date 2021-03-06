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

import org.everrest.core.Parameter;
import org.everrest.core.impl.ApplicationContext;
import org.everrest.core.method.ParameterResolver;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

/**
 * @author andrew00x
 */
public class ContextParameterResolver implements ParameterResolver<Context> {

    @Override
    public Object resolve(Parameter parameter, ApplicationContext context) throws Exception {
        Class<?> parameterClass = parameter.getParameterClass();
        if (parameterClass == HttpHeaders.class) {
            return context.getHttpHeaders();
        } else if (parameterClass == SecurityContext.class) {
            return context.getSecurityContext();
        } else if (parameterClass == Request.class) {
            return context.getRequest();
        } else if (parameterClass == UriInfo.class) {
            return context.getUriInfo();
        } else if (parameterClass == Providers.class) {
            return context.getProviders();
        } else if (parameterClass == Application.class) {
            return context.getApplication();
        }
        return context.getEnvironmentContext().get(parameter.getParameterClass());
    }
}
