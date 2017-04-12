/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors:
 * Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.everrest.guice;

import com.google.inject.Module;
import org.everrest.core.impl.ContainerResponse;
import org.junit.Test;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ConstructorParametersInjectionTest extends BaseTest {
    @Override
    protected List<Module> getModules() {
        return newArrayList((Module) binder -> {
            binder.bind(RequestResource.class);
            binder.bind(UriInfoResource.class);
            binder.bind(HttpHeadersResource.class);
            binder.bind(SecurityContextResource.class);
            binder.bind(ProvidersResource.class);
            binder.bind(ApplicationResource.class);
        });
    }

    @Test
    public void injectsUriInfo() throws Exception {
        ContainerResponse response = launcher.service("POST", "/a/1", "", null, null, null);

        assertEquals(String.format("Expected %s injected", UriInfo.class), "UriInfo",  response.getEntity());
    }

    @Test
    public void injectsRequest() throws Exception {
        ContainerResponse response = launcher.service("POST", "/b/1", "", null, null, null);

        assertEquals(String.format("Expected %s injected", Request.class), "Request", response.getEntity());
    }

    @Test
    public void injectsHttpHeaders() throws Exception {
        ContainerResponse response = launcher.service("POST", "/c/1", "", null, null, null);

        assertEquals(String.format("Expected %s injected", HttpHeaders.class), "HttpHeaders", response.getEntity());
    }

    @Test
    public void injectsSecurityContext() throws Exception {
        ContainerResponse response = launcher.service("POST", "/d/1", "", null, null, null);

        assertEquals(String.format("Expected %s injected", SecurityContext.class), "SecurityContext", response.getEntity());
    }

    @Test
    public void injectsProviders() throws Exception {
        ContainerResponse response = launcher.service("POST", "/e/1", "", null, null, null);

        assertEquals(String.format("Expected %s injected", Providers.class), "Providers", response.getEntity());
    }

    @Test
    public void injectsApplication() throws Exception {
        ContainerResponse response = launcher.service("POST", "/f/1", "", null, null, null);

        assertEquals(String.format("Expected %s injected", Application.class), "Application", response.getEntity());
    }


    @Path("a")
    public static class UriInfoResource {
        private final UriInfo uriInfo;

        @Inject
        public UriInfoResource(@Context UriInfo uriInfo) {
            this.uriInfo = uriInfo;
        }

        @Path("1")
        @POST
        public String m1() {
            assertNotNull(uriInfo);
            return "UriInfo";
        }
    }

    @Path("b")
    public static class RequestResource {
        private final Request request;

        @Inject
        public RequestResource(@Context Request request) {
            this.request = request;
        }

        @Path("1")
        @POST
        public String m1() {
            assertNotNull(request);
            return "Request";
        }
    }

    @Path("c")
    public static class HttpHeadersResource {
        private final HttpHeaders httpHeaders;

        @Inject
        public HttpHeadersResource(@Context HttpHeaders httpHeaders) {
            this.httpHeaders = httpHeaders;
        }

        @Path("1")
        @POST
        public String m1() {
            assertNotNull(httpHeaders);
            return "HttpHeaders";
        }
    }

    @Path("d")
    public static class SecurityContextResource {
        private final SecurityContext securityContext;

        @Inject
        public SecurityContextResource(@Context SecurityContext securityContext) {
            this.securityContext = securityContext;
        }

        @Path("1")
        @POST
        public String m1() {
            assertNotNull(securityContext);
            return "SecurityContext";
        }
    }

    @Path("e")
    public static class ProvidersResource {
        private final Providers providers;

        @Inject
        public ProvidersResource(@Context Providers providers) {
            this.providers = providers;
        }

        @Path("1")
        @POST
        public String m1() {
            assertNotNull(providers);
            return "Providers";
        }
    }

    @Path("f")
    public static class ApplicationResource {
        private final Application application;

        @Inject
        public ApplicationResource(@Context Application application) {
            this.application = application;
        }

        @Path("1")
        @POST
        public String m1() {
            assertNotNull(application);
            return "Application";
        }
    }
}
