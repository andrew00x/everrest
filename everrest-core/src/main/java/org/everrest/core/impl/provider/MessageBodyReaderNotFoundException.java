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

public class MessageBodyReaderNotFoundException extends ProcessingException {
    /**
     * Creates new MessageBodyReaderNotFoundException with prepared error message.
     *
     * @param entityType Java type for which MessageBodyReader was not found
     * @param mediaType  media type for which MessageBodyReader was not found
     */
    public MessageBodyReaderNotFoundException(Class<?> entityType, MediaType mediaType) {
        this(String.format("Unsupported entity type %s. There is no any message body reader that can read this type from %s", entityType.getSimpleName(), mediaType));
    }

    public MessageBodyReaderNotFoundException(String message) {
        super(message);
    }
}
