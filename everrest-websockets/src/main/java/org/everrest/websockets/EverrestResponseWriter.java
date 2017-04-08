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
package org.everrest.websockets;

import org.everrest.core.ContainerResponseWriter;
import org.everrest.core.GenericContainerResponse;
import org.everrest.websockets.message.Pair;
import org.everrest.websockets.message.RestOutputMessage;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Fill RestOutputMessage by result of calling REST method.
 *
 * @author andrew00x
 */
class EverrestResponseWriter implements ContainerResponseWriter {
    private final RestOutputMessage output;
    private final OutputStream entityOutput;

    EverrestResponseWriter(RestOutputMessage output, OutputStream entityOutput) {
        this.output = output;
        this.entityOutput = entityOutput;
    }

    @Override
    public void writeHeaders(GenericContainerResponse response) throws IOException {
        output.setResponseCode(response.getStatus());
        output.setHeaders(Pair.fromMap(response.getHeaders()));
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return entityOutput;
    }
}
