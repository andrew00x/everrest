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
package org.everrest.core.method;

import java.lang.annotation.Annotation;

public interface ParameterResolverFactory {
    /**
     * Creates parameter resolver for supplied annotation.
     *
     * @param annotation JAX-RS annotation
     * @return ParameterResolver
     */
    ParameterResolver createParameterResolver(Annotation annotation);
}
