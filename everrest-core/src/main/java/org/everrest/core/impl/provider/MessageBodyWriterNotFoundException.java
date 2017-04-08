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

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MediaType;

public class MessageBodyWriterNotFoundException extends ProcessingException {
    /**
     * Creates new MessageBodyWriterNotFoundException with prepared error message.
     *
     * @param entityType Java type for which MessageBodyWriter was not found
     * @param mediaType  media type for which MessageBodyWriter was not found
     */
    public MessageBodyWriterNotFoundException(Class<?> entityType, MediaType mediaType) {
        this(String.format("Unsupported entity type %s. There is no any MessageBodyWriter that can serialize this type to %s", entityType.getSimpleName(), mediaType));
    }

    public MessageBodyWriterNotFoundException(String message) {
        super(message);
    }
}
