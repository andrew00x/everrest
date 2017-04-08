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

import org.everrest.core.provider.EntityProvider;

import javax.annotation.Priority;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;
import static org.everrest.core.provider.EntityProvider.EMBEDDED_ENTITY_PROVIDER_PRIORITY;

@Priority(EMBEDDED_ENTITY_PROVIDER_PRIORITY)
@Provider
@Consumes({MediaType.APPLICATION_XML, "application/*+xml", MediaType.TEXT_XML, "text/*+xml"})
@Produces({MediaType.APPLICATION_XML, "application/*+xml", MediaType.TEXT_XML, "text/*+xml"})
public class StreamSourceEntityProvider implements EntityProvider<StreamSource> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == StreamSource.class;
    }

    @Override
    public StreamSource readFrom(Class<StreamSource> type,
                                 Type genericType,
                                 Annotation[] annotations,
                                 MediaType mediaType,
                                 MultivaluedMap<String, String> httpHeaders,
                                 InputStream entityStream) throws IOException {
        return new StreamSource(entityStream);
    }

    @Override
    public long getSize(StreamSource streamSource, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return StreamSource.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(StreamSource streamSource,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException {
        StreamResult streamResult = new StreamResult(entityStream);
        try {
            TransformerFactory factory = createFeaturedTransformerFactory();
            factory.newTransformer().transform(streamSource, streamResult);
        } catch (TransformerException | TransformerFactoryConfigurationError e) {
            throw new IOException(String.format("Can't write to output stream, %s", e));
        }
    }

    private TransformerFactory createFeaturedTransformerFactory() throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(FEATURE_SECURE_PROCESSING, true);
        return factory;
    }
}
