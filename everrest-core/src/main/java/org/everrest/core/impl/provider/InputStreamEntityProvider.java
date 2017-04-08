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
package org.everrest.core.impl.provider;

import com.google.common.io.ByteStreams;
import org.everrest.core.impl.ApplicationContext;
import org.everrest.core.provider.EntityProvider;

import javax.annotation.Priority;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static org.everrest.core.impl.ServerConfigurationProperties.DEFAULT_MAX_BUFFER_SIZE;
import static org.everrest.core.impl.ServerConfigurationProperties.EVERREST_MAX_BUFFER_SIZE;
import static org.everrest.core.provider.EntityProvider.EMBEDDED_ENTITY_PROVIDER_PRIORITY;

/**
 * @author andrew00x
 */
@Priority(EMBEDDED_ENTITY_PROVIDER_PRIORITY)
@Provider
public class InputStreamEntityProvider implements EntityProvider<InputStream> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == InputStream.class;
    }

    @Override
    public InputStream readFrom(Class<InputStream> type,
                                Type genericType,
                                Annotation[] annotations,
                                MediaType mediaType,
                                MultivaluedMap<String, String> httpHeaders,
                                InputStream entityStream) throws IOException {
        ApplicationContext context = ApplicationContext.getCurrent();
        if (context.isAsynchronous()) {
            int bufferSize = context.getConfigurationProperties().getIntegerProperty(EVERREST_MAX_BUFFER_SIZE, DEFAULT_MAX_BUFFER_SIZE);
            return IOHelper.bufferStream(entityStream, bufferSize);
        }
        return entityStream;
    }

    @Override
    public long getSize(InputStream t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return InputStream.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(InputStream in,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException {
        try {
            ByteStreams.copy(in, entityStream);
        } finally {
            in.close();
        }
    }
}
