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
package org.everrest.core.impl.client;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintWriter;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.everrest.core.impl.header.MediaTypeHelper.withoutParameters;
import static org.junit.Assert.assertEquals;

public class ClientIntegrationTest {
    private Server server;

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void sendsGetRequest() throws Exception {
        startServer(new TestingHandler(200, "Hello world", "text/plain"));
        Response response = ClientBuilder.newClient().target("http://localhost:9111/").request().get();
        String entity = response.readEntity(String.class);
        assertEquals(200, response.getStatus());
        assertEquals("Hello world", entity);
        assertEquals(new MediaType("text", "plain"), withoutParameters(response.getMediaType()));
    }

    @Test
    public void sendsPostRequest() throws Exception {
        startServer(new TestingHandler(200, "to be or not to be", "text/plain") {
            @Override
            void validateRequest(HttpServletRequest request) {
                assertEquals("text/plain", request.getHeader(CONTENT_TYPE));
                assertEquals("18", request.getHeader(CONTENT_LENGTH));
            }
        });
        Entity requestEntity = Entity.text("to be or not to be");
        Response response = ClientBuilder.newClient().target("http://localhost:9111/").request().post(requestEntity);
        String entity = response.readEntity(String.class);
        assertEquals(200, response.getStatus());
        assertEquals("to be or not to be", entity);
        assertEquals(new MediaType("text", "plain"), withoutParameters(response.getMediaType()));
    }

    @Test
    public void sendsRequestAndGetNotSuccessResponse() throws Exception {
        startServer(new TestingHandler(400, "Some error", "text/plain"));
        Response response = ClientBuilder.newClient().target("http://localhost:9111/").request().get();
        String entity = response.readEntity(String.class);
        assertEquals(400, response.getStatus());
        assertEquals("Some error", entity);
        assertEquals(new MediaType("text", "plain"), withoutParameters(response.getMediaType()));
    }

    private void startServer(Handler handler) throws Exception {
        server = new Server(9111);
        server.setHandler(handler);
        server.start();
    }

    static class TestingHandler extends AbstractHandler {
        final int status;
        final String responseEntity;
        final String responseContentType;

        TestingHandler(int status, String responseEntity, String responseContentType) {
            this.status = status;
            this.responseEntity = responseEntity;
            this.responseContentType = responseContentType;
        }

        @Override
        public void handle(String s, Request internalRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            validateRequest(request);
            response.setContentType(responseContentType);
            response.setStatus(status);
            PrintWriter out = response.getWriter();
            out.print(responseEntity);
            internalRequest.setHandled(true);
        }

        void validateRequest(HttpServletRequest request){
        }
    }
}
