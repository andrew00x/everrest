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
package org.everrest.core;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.Response;
import java.io.IOException;

public interface GenericContainerResponse extends ContainerResponseContext {

    /**
     * Set response. New response can override old one.
     *
     * @param response
     *         See {@link Response}
     */
    void setResponse(Response response);

    /**
     * Get preset {@link Response}.
     *
     * @return preset {@link Response}.
     */
    Response getResponse();

    /**
     * Write response to output stream.
     *
     * @throws IOException
     *         if any i/o errors occurs
     */
    void writeResponse() throws IOException;
}
