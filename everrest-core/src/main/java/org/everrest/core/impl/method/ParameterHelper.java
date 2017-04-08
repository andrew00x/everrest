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

import com.google.common.collect.ImmutableList;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.util.List;

public class ParameterHelper {
    /**
     * Collections of annotation that allowed to be used on fields on any type of Provider.
     *
     * @see javax.ws.rs.ext.Provider
     * @see javax.ws.rs.ext.Providers
     */
    public static final List<String> PROVIDER_FIELDS_ANNOTATIONS = ImmutableList.of(Context.class.getName());
    /**
     * Collections of annotation than allowed to be used on constructor's parameters of any type of Provider.
     *
     * @see javax.ws.rs.ext.Provider
     * @see javax.ws.rs.ext.Providers
     */
    public static final List<String> PROVIDER_CONSTRUCTOR_PARAMETER_ANNOTATIONS = ImmutableList.of(Context.class.getName());

    /**
     * Collections of annotation that allowed to be used on fields of resource class.
     */
    public static final List<String> RESOURCE_FIELDS_ANNOTATIONS = ImmutableList.of(CookieParam.class.getName(),
                                                                                    Context.class.getName(),
                                                                                    HeaderParam.class.getName(),
                                                                                    MatrixParam.class.getName(),
                                                                                    PathParam.class.getName(),
                                                                                    QueryParam.class.getName());

    /**
     * Collections of annotation than allowed to be used on constructor's parameters of resource class.
     */
    public static final List<String> RESOURCE_CONSTRUCTOR_PARAMETER_ANNOTATIONS = ImmutableList.of(CookieParam.class.getName(),
                                                                                                   Context.class.getName(),
                                                                                                   HeaderParam.class.getName(),
                                                                                                   MatrixParam.class.getName(),
                                                                                                   PathParam.class.getName(),
                                                                                                   QueryParam.class.getName());

    /**
     * Collections of annotation than allowed to be used on method's parameters of resource class.
     */
    public static final List<String> RESOURCE_METHOD_PARAMETER_ANNOTATIONS = ImmutableList.of(CookieParam.class.getName(),
                                                                                              Context.class.getName(),
                                                                                              HeaderParam.class.getName(),
                                                                                              MatrixParam.class.getName(),
                                                                                              PathParam.class.getName(),
                                                                                              QueryParam.class.getName(),
                                                                                              FormParam.class.getName());
}
