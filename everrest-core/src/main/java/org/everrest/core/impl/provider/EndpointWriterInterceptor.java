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
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;

public class EndpointWriterInterceptor implements WriterInterceptor {
    private final ProviderBinder providers;

    public EndpointWriterInterceptor(ProviderBinder providers) {
        this.providers = providers;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        MessageBodyWriter entityWriter = providers.getMessageBodyWriter(context.getType(), context.getGenericType(), context.getAnnotations(), context.getMediaType());
        if (entityWriter == null) {
            throw new MessageBodyWriterNotFoundException(context.getType(), context.getMediaType());
        }
        if (ApplicationContext.getCurrent() != null && Tracer.isTracingEnabled()) {
            Tracer.trace("Matched MessageBodyWriter for type %s, media type %s = (%s)", context.getType(), context.getMediaType(), entityWriter);
        }

        if (context.getHeaders().getFirst(CONTENT_LENGTH) == null) {
            long contentLength = entityWriter.getSize(context.getEntity(), context.getType(), context.getGenericType(), context.getAnnotations(), context.getMediaType());
            if (contentLength >= 0) {
                context.getHeaders().putSingle(CONTENT_LENGTH, contentLength);
            }
        }

        writeEntity(context, entityWriter);
    }

    @SuppressWarnings("unchecked")
    protected void writeEntity(WriterInterceptorContext context, MessageBodyWriter entityWriter) throws IOException {
        entityWriter.writeTo(context.getEntity(),
                             context.getType(),
                             context.getGenericType(),
                             context.getAnnotations(),
                             context.getMediaType(),
                             context.getHeaders(),
                             context.getOutputStream());
    }
}
