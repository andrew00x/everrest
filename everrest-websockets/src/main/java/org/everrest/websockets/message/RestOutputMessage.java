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
package org.everrest.websockets.message;

import java.util.LinkedList;
import java.util.List;

/**
 * REST output messages.
 *
 * @author andrew00x
 */
public class RestOutputMessage extends OutputMessage {

    public static Builder anOutput() {
        return new Builder();
    }

    public static Builder anOutput(RestInputMessage input) {
        return anOutput()
                .uuid(input.getUuid())
                .method(input.getMethod())
                .path(input.getPath());
    }

    public static class Builder {
        private String method;
        private String path;
        private int responseCode;
        private String uuid;
        private String body;
        private List<Pair> headers = new LinkedList<>();

        public Builder responseCode(int code) {
            this.responseCode = code;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder addHeader(Pair header) {
            headers.add(header);
            return this;
        }

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public RestOutputMessage build() {
            return new RestOutputMessage(responseCode, method, path, uuid, headers.toArray(new Pair[headers.size()]), body);
        }
    }

    private String method;
    private String path;
    private Pair[] headers;

    public RestOutputMessage(int responseCode, String method, String path, String uuid, Pair[] headers, String body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        setResponseCode(responseCode);
        setUuid(uuid);
        setBody(body);
    }

    public RestOutputMessage() {
    }

    /**
     * Get name of HTTP method specified for resource method, e.g. GET, POST, PUT, etc.
     *
     * @return name of HTTP method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Set name of HTTP method specified for resource method, e.g. GET, POST, PUT, etc.
     *
     * @param method
     *         name of HTTP method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Get resource path.
     *
     * @return resource path
     */
    public String getPath() {
        return path;
    }

    /**
     * Set resource path.
     *
     * @param path
     *         resource path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Get HTTP headers.
     *
     * @return HTTP headers
     */
    public Pair[] getHeaders() {
        return headers;
    }

    /**
     * Set HTTP headers.
     *
     * @param headers
     *         HTTP headers
     */
    public void setHeaders(Pair[] headers) {
        this.headers = headers;
    }
}
