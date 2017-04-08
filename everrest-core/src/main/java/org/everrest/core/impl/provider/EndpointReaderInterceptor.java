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

import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.ApplicationContext;
import org.everrest.core.util.Tracer;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.IOException;
import java.io.InputStream;

public class EndpointReaderInterceptor implements ReaderInterceptor {
    private final ProviderBinder providers;

    public EndpointReaderInterceptor(ProviderBinder providers) {
        this.providers = providers;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        MessageBodyReader messageBodyReader = providers.getMessageBodyReader(context.getType(), context.getGenericType(), context.getAnnotations(), context.getMediaType());
        if (messageBodyReader == null) {
            throw new MessageBodyReaderNotFoundException(context.getType(), context.getMediaType());
        }
        if (ApplicationContext.getCurrent() != null && Tracer.isTracingEnabled()) {
            Tracer.trace(String.format("Matched MessageBodyReader for type %s, media type %s = (%s)", context.getType(), context.getMediaType(), messageBodyReader));
        }
        InputStream entityStream = context.getInputStream();
        Object entity = messageBodyReader.readFrom(context.getType(),
                context.getGenericType(),
                context.getAnnotations(),
                context.getMediaType(),
                context.getHeaders(),
                entityStream);
        if (entity != entityStream) {
            entityStream.close();
        }
        return entity;
    }
}
