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
package org.everrest.core.impl.provider.multipart;

import org.apache.commons.fileupload.FileItem;

import javax.annotation.Priority;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.everrest.core.provider.EntityProvider.EMBEDDED_ENTITY_PROVIDER_PRIORITY;
import static org.everrest.core.util.ParameterizedTypeImpl.newParameterizedType;

/**
 * @author andrew00x
 */
@Priority(EMBEDDED_ENTITY_PROVIDER_PRIORITY)
@Provider
@Consumes({"multipart/*"})
public class MapMultipartFormDataMessageBodyReader implements MessageBodyReader<Map<String, InputItem>> {

    private final Providers providers;

    public MapMultipartFormDataMessageBodyReader(@Context Providers providers) {
        this.providers = providers;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (type == Map.class && genericType instanceof ParameterizedType) {
            ParameterizedType t = (ParameterizedType)genericType;
            Type[] ta = t.getActualTypeArguments();
            return ta.length == 2 && ta[0] == String.class && ta[1] == InputItem.class;
        }
        return false;
    }

    @Override
    public Map<String, InputItem> readFrom(Class<Map<String, InputItem>> type, Type genericType, Annotation[] annotations,
                                           MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        final Type genericSuperclass = newParameterizedType(Iterator.class, FileItem.class);
        final MessageBodyReader<Iterator> multipartReader =
                providers.getMessageBodyReader(Iterator.class, genericSuperclass, annotations, mediaType);
        final Iterator iterator =
                multipartReader.readFrom(Iterator.class, genericSuperclass, annotations, mediaType, httpHeaders, entityStream);
        final Map<String, InputItem> result = new LinkedHashMap<>();
        while (iterator.hasNext()) {
            final DefaultInputItem item = new DefaultInputItem((FileItem)iterator.next(), providers);
            result.put(item.getName(), item);
        }
        return result;
    }
}
