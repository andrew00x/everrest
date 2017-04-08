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
package org.everrest.core.$matchers;

import org.everrest.core.UnhandledException;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class ExceptionMatchers {
    public static BaseMatcher<Throwable> webApplicationExceptionWithStatus(Response.Status status) {
        return new BaseMatcher<Throwable>() {
            @Override
            public boolean matches(Object item) {
                return item instanceof WebApplicationException
                       && status.equals(((WebApplicationException)item).getResponse().getStatusInfo());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("WebApplicationException with status %d \"%s\"", status.getStatusCode(), status.getReasonPhrase()));
            }
        };
    }

    public static BaseMatcher<Throwable> webApplicationExceptionWithStatus(int status) {
        return new BaseMatcher<Throwable>() {
            @Override
            public boolean matches(Object item) {
                return item instanceof WebApplicationException
                        && status == ((WebApplicationException)item).getResponse().getStatus();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("WebApplicationException with status %d", status));
            }
        };
    }

    public static BaseMatcher<Throwable> exceptionSameInstance(Exception expectedException) {
        return new BaseMatcher<Throwable>() {
            @Override
            public boolean matches(Object item) {
                return item == expectedException;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("Expected exception: %s", expectedException));
            }
        };
    }

    public static BaseMatcher<Throwable> wrappedExceptionWithMessage(Class<? extends Throwable> expectedType, Class<? extends Throwable> wrappedBy, String expectedMessage) {
        return new BaseMatcher<Throwable>() {
            @Override
            public boolean matches(Object item) {
                return item != null
                        && wrappedBy.isInstance(item)
                        && expectedType.isInstance(wrappedBy.cast(item).getCause())
                        && expectedMessage.equals(wrappedBy.cast(item).getCause().getMessage());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("Expected exception of type: %s, wrapped by: %s, with message: %s", expectedType.getSimpleName(), wrappedBy.getSimpleName(), expectedMessage));
            }
        };
    }

    public static BaseMatcher<Throwable> unhandledExceptionWithStatus(int status) {
        return new BaseMatcher<Throwable>() {
            @Override
            public boolean matches(Object item) {
                return ((UnhandledException)item).getResponseStatus() == status;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("Expected UnhandledException with status: %s", status));
            }
        };
    }
}
