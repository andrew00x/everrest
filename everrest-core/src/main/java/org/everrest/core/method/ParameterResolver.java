/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors:
 * Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.everrest.core.method;

import org.everrest.core.Parameter;
import org.everrest.core.impl.ApplicationContext;

import java.lang.annotation.Annotation;

/**
 * Creates object that might be injected in JAX-RS component.
 *
 * @param <T> JAX-RS annotation
 * @author andrew00x
 */
public interface ParameterResolver<T extends Annotation> {
    /**
     * Creates object which will be passed in JAX-RS component.
     *
     * @param parameter See {@link Parameter}
     * @param context   See {@link ApplicationContext}
     * @return newly created instance of class {@link Parameter#getParameterClass()}
     * @throws Exception if any errors occurs
     */
    Object resolve(Parameter parameter, ApplicationContext context) throws Exception;
}
