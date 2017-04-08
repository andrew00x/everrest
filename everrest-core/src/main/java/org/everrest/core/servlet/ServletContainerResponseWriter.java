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
package org.everrest.core.servlet;

import org.everrest.core.ContainerResponseWriter;
import org.everrest.core.GenericContainerResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ServletContainerResponseWriter implements ContainerResponseWriter {
    private final HttpServletResponse servletResponse;

    public ServletContainerResponseWriter(HttpServletResponse response) {
        this.servletResponse = response;
    }

    @Override
    public void writeHeaders(GenericContainerResponse response) throws IOException {
        if (servletResponse.isCommitted()) {
            return;
        }
        servletResponse.setStatus(response.getStatus());
        for (Map.Entry<String, List<String>> entry : response.getStringHeaders().entrySet()) {
            String name = entry.getKey();
            entry.getValue().stream()
                            .filter(Objects::nonNull)
                            .forEach(value -> servletResponse.addHeader(name, value));
        }
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return servletResponse.getOutputStream();
    }
}