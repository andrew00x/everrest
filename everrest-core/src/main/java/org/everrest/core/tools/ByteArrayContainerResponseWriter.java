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
package org.everrest.core.tools;

import org.everrest.core.ContainerResponseWriter;
import org.everrest.core.GenericContainerResponse;
import org.everrest.core.util.CaselessMultivaluedMap;

import javax.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Fake object that can be used for any tests.
 *
 * @author andrew00x
 */
public class ByteArrayContainerResponseWriter implements ContainerResponseWriter {
    private final ByteArrayOutputStream entityOutput;
    private final MultivaluedMap<String, Object> headers;

    public ByteArrayContainerResponseWriter() {
        headers = new CaselessMultivaluedMap<>();
        entityOutput = new ByteArrayOutputStream();
    }

    @Override
    public void writeHeaders(GenericContainerResponse response) throws IOException {
        headers.putAll(response.getHeaders());
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return entityOutput;
    }

    public byte[] getBody() {
        return entityOutput.toByteArray();
    }

    public String getBodyAsString() {
        return entityOutput.toString();
    }

    public MultivaluedMap<String, Object> getHeaders() {
        return headers;
    }

    /** Clears message body and HTTP headers map. */
    public void reset() {
        entityOutput.reset();
        headers.clear();
    }
}
