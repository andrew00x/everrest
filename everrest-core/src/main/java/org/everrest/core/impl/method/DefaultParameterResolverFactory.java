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

import org.everrest.core.method.ParameterResolver;
import org.everrest.core.method.ParameterResolverFactory;
import org.everrest.core.method.TypeProducerFactory;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.lang.annotation.Annotation;

/**
 * @author andrew00x
 */
public class DefaultParameterResolverFactory implements ParameterResolverFactory {
    private final TypeProducerFactory typeProducerFactory;

    public DefaultParameterResolverFactory(TypeProducerFactory typeProducerFactory) {
        this.typeProducerFactory = typeProducerFactory;
    }

    /**
     * Create parameter resolver for supplied annotation.
     *
     * @param annotation
     *         JAX-RS annotation
     * @return ParameterResolver
     */
    public ParameterResolver createParameterResolver(Annotation annotation) {
        final Class annotationType = annotation.annotationType();
        if (annotationType == CookieParam.class) {
            return new CookieParameterResolver((CookieParam) annotation, typeProducerFactory);
        } else if (annotationType == Context.class) {
            return new ContextParameterResolver();
        } else if (annotationType == FormParam.class) {
            return new FormParameterResolver((FormParam) annotation, typeProducerFactory);
        } else if (annotationType == HeaderParam.class) {
            return new HeaderParameterResolver((HeaderParam) annotation, typeProducerFactory);
        } else if (annotationType == MatrixParam.class) {
            return new MatrixParameterResolver((MatrixParam) annotation, typeProducerFactory);
        } else if (annotationType == PathParam.class) {
            return new PathParameterResolver((PathParam) annotation, typeProducerFactory);
        } else if (annotationType == QueryParam.class) {
            return new QueryParameterResolver((QueryParam) annotation, typeProducerFactory);
        } else {
            throw new IllegalArgumentException(String.format("Unsupported annotation %s", annotationType));
        }
    }
}
