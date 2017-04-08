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
package org.everrest.assured;

import org.hamcrest.Matchers;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_NAME;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_PASSWORD;
import static org.everrest.assured.JettyHttpServer.SECURE_PATH;

@Listeners(value = {EverrestJetty.class})
public class TestSecureServices {
    @Path("/secure-test")
    public class SecureService {
        @GET
        @RolesAllowed("cloud-admin")
        @Path("/protected")
        public String getSecure() {
            return "protected";
        }

        @GET
        @Path("/unprotected")
        public String getUSecure() {
            return "unprotected";
        }
    }

    private final SecureService secureService = new SecureService();

    @Test
    public void allowsToCallUnprotectedMethodThroughUnprotectedContextWithoutAuthentication() {
        expect().body(Matchers.equalTo("unprotected"))
                .when().get("/secure-test/unprotected");
    }

    @Test
    public void doesNotAllowToCallUnprotectedMethodThroughProtectedContextWithoutAuthentication() {
        expect().statusCode(401)
                .when().get(SECURE_PATH + "/secure-test/unprotected");
    }

    @Test
    public void doesNotAllowToCallProtectedMethodThroughProtectedContextWithoutAuthentication() {
        expect().statusCode(401)
                .when().get(SECURE_PATH + "/secure-test/protected");
    }

    @Test
    public void doesNotAllowToCallProtectedMethodThroughUnprotectedContextWithoutAuthentication() {
        expect().statusCode(403)
                .when().get("/secure-test/protected");
    }

    @Test
    public void allowsToCallUnprotectedMethodThroughUnprotectedContextWithAuthentication() {
        given().auth().basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
               .expect()
               .body(Matchers.equalTo("unprotected"))
               .when().get("/secure-test/unprotected");
    }

    @Test
    public void allowsToCallUnprotectedMethodThroughProtectedContextWithAuthentication() {
        given().auth().basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
               .expect()
               .body(Matchers.equalTo("unprotected"))
               .when().get(SECURE_PATH + "/secure-test/unprotected");
    }

    @Test
    public void allowsToCallProtectedMethodThroughProtectedContextWithAuthentication() {
        given().auth().basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
               .expect()
               .body(Matchers.equalTo("protected"))
               .when().get(SECURE_PATH + "/secure-test/protected");
    }

    @Test
    public void doesNotAllowToCallProtectedMethodThroughUnprotectedContextWithAuthentication() {
        given().auth().basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
               .expect()
               .statusCode(403)
               .when().get("/secure-test/protected");
    }
}
