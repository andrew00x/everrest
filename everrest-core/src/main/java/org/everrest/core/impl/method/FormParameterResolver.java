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
import org.everrest.core.impl.MultivaluedMapImpl;
import org.everrest.core.method.ParameterResolver;
import org.everrest.core.method.TypeProducer;
import org.everrest.core.method.TypeProducerFactory;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import java.lang.reflect.ParameterizedType;

import static org.everrest.core.util.ParameterizedTypeImpl.newParameterizedType;

/**
 * Creates object that might be injected to JAX-RS component through method (constructor) parameter or field annotated with
 * &#064;FormParam annotation.
 */
public class FormParameterResolver implements ParameterResolver<FormParam> {
    private final FormParam           formParam;
    private final TypeProducerFactory typeProducerFactory;

    FormParameterResolver(FormParam formParam, TypeProducerFactory typeProducerFactory) {
        this.formParam = formParam;
        this.typeProducerFactory = typeProducerFactory;
    }

    @Override
    public Object resolve(Parameter parameter, ApplicationContext context) throws Exception {
        String param = formParam.value();
        TypeProducer typeProducer = typeProducerFactory.createTypeProducer(parameter.getParameterClass(), parameter.getGenericType(), parameter.getAnnotations());

        MultivaluedMap<String, String> form = readForm(context, !parameter.isEncoded());
        return typeProducer.createValue(param, form, parameter.getDefaultValue());
    }

    @SuppressWarnings({"unchecked"})
    private MultivaluedMap<String, String> readForm(ApplicationContext context, boolean decode) throws java.io.IOException {
        MediaType contentType = context.getHttpHeaders().getMediaType();
        ParameterizedType multivaluedMapType = newParameterizedType(MultivaluedMap.class, String.class, String.class);
        MessageBodyReader reader = context.getProviders().getMessageBodyReader(MultivaluedMap.class, multivaluedMapType, null, contentType);
        reader.readFrom(MultivaluedMap.class,
                        multivaluedMapType,
                        null,
                        contentType,
                        context.getHttpHeaders().getRequestHeaders(),
                        context.getContainerRequest().getEntityStream());
        MultivaluedMap<String, String> form;
        if (decode) {
            form = (MultivaluedMap<String, String>)context.getAttributes().get("org.everrest.provider.entity.decoded.form");
        } else {
            form = (MultivaluedMap<String, String>)context.getAttributes().get("org.everrest.provider.entity.encoded.form");
        }
        if (form == null) {
            form = new MultivaluedMapImpl();
        }

        return form;
    }
}
