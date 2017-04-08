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

import com.google.common.collect.ImmutableMap;
import org.everrest.core.ConfigurationProperties;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import java.io.InputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static com.google.common.collect.Sets.newHashSet;
import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LANGUAGE;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.DATE;
import static javax.ws.rs.core.HttpHeaders.ETAG;
import static javax.ws.rs.core.HttpHeaders.LAST_MODIFIED;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.HttpHeaders.SET_COOKIE;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientResponseTest {
    private final int status = 200;
    private MultivaluedMap<String, String> headers;

    private ClientResponse response;

    @Before
    public void setUp() {
        headers = new MultivaluedHashMap<>();
        ConfigurationProperties properties = mock(ConfigurationProperties.class);
        response = new ClientResponse(status, headers, null, properties);
    }

    @Test
    public void getsStatus() {
        assertEquals(status, response.getStatus());
    }

    @Test
    public void getsStatusInfo() {
        assertEquals(OK, response.getStatusInfo());
    }

    @Test
    public void setsStatus() {
        response.setStatus(403);
        assertEquals(403, response.getStatus());
        assertEquals(FORBIDDEN, response.getStatusInfo());
    }

    @Test
    public void setsStatusInfo() {
        response.setStatusInfo(FORBIDDEN);
        assertEquals(403, response.getStatus());
        assertEquals(FORBIDDEN, response.getStatusInfo());
    }

    @Test
    public void getsHeaders() {
        assertEquals(headers, response.getHeaders());
    }

    @Test
    public void getsHeaderString() {
        headers.putSingle(CONTENT_TYPE, "application/json");
        assertEquals("application/json", response.getHeaderString(CONTENT_TYPE));
    }

    @Test
    public void getsAllowedMethodsAsEmptySetWhenAllowHeaderIsNotSet() {
        assertTrue(response.getAllowedMethods().isEmpty());
    }

    @Test
    public void getsAllowedMethods() {
        headers.addAll(ALLOW, "Get", "put", "POST");
        assertEquals(newHashSet("GET", "PUT", "POST"), response.getAllowedMethods());
    }

    @Test
    public void getsDate() {
        headers.putSingle(DATE, "Fri, 08 Jan 2010 02:05:00 EET");
        assertTrue(Math.abs(date(2010, 1, 8, 2, 5, 0, "EET").getTime() - response.getDate().getTime()) < 1000);
    }

    @Test
    public void getsLanguage() {
        headers.putSingle(CONTENT_LANGUAGE, "ua-ua");
        assertEquals(new Locale("ua", "ua"), response.getLanguage());
    }

    @Test
    public void getsContentLength() {
        headers.putSingle(CONTENT_LENGTH, "99");
        assertEquals(99, response.getLength());
    }

    @Test
    public void getsContentLengthAsMinusOneWhenContentLengthHeaderIsNotSet() {
        assertEquals(-1, response.getLength());
    }

    @Test
    public void getsContentLengthAsMinusOneWhenContentLengthHeaderIsInvalid() {
        headers.putSingle(CONTENT_LENGTH, "wrong");
        assertEquals(-1, response.getLength());
    }

    @Test
    public void getsContentType() {
        headers.putSingle(CONTENT_TYPE, "application/json");
        assertEquals(new MediaType("application", "json"), response.getMediaType());
    }

    @Test
    public void getsCookies() {
        headers.addAll(SET_COOKIE, "name1=value1", "name2=value2");
        assertEquals(ImmutableMap.of("name1", new NewCookie("name1", "value1"), "name2", new NewCookie("name2", "value2")),
                response.getCookies());
    }

    @Test
    public void getsCookiesAsEmptyMapWhenSetCookieHeaderIsNotSet() {
        assertTrue(response.getCookies().isEmpty());
    }

    @Test
    public void getsEntityTag() {
        headers.putSingle(ETAG, "W/\"test\"");
        assertEquals(new EntityTag("test", true), response.getEntityTag());
    }

    @Test
    public void getsLastModified() {
        headers.putSingle(LAST_MODIFIED, "Fri, 08 Jan 2010 02:05:00 EET");
        assertTrue(Math.abs(date(2010, 1, 8, 2, 5, 0, "EET").getTime() - response.getLastModified().getTime()) < 1000);
    }

    @Test
    public void getsLocation() {
        headers.putSingle(LOCATION, "http://test.com/foo");
        assertEquals(URI.create("http://test.com/foo"), response.getLocation());
    }

    @Test
    public void getsLinks() {
        String link = "< http://localhost:8080/x/y/z >; rel=\"xxx\"; title=\"yyy\"";
        headers.putSingle(LINK, link);
        assertEquals(newHashSet(Link.valueOf(link)), response.getLinks());
    }

    @Test
    public void getsLinksAsEmptySetWhenLinkHeaderIsNotSet() {
        assertTrue(response.getLinks().isEmpty());
    }

    @Test
    public void testsThatHasLink() {
        String link = "< http://localhost:8080/x/y/z >; rel=\"xxx\"";
        headers.putSingle(LINK, link);
        assertTrue(response.hasLink("xxx"));
        assertFalse(response.hasLink("yyy"));
    }

    @Test
    public void getsLinkByRelation() {
        String link = "< http://localhost:8080/x/y/z >; rel=\"xxx\"";
        headers.putSingle(LINK, link);
        assertEquals(Link.valueOf(link), response.getLink("xxx"));
    }

    @Test
    public void getsLinkBuilderByRelation() {
        String link = "< http://localhost:8080/x/y/z >; rel=\"xxx\"";
        headers.putSingle(LINK, link);
        assertEquals(Link.valueOf(link), response.getLinkBuilder("xxx").build());
    }

    @Test
    public void testsThatHasEntityWhenEntityStreamSupportsMarkReset() throws Exception {
        InputStream entity = mock(InputStream.class);
        when(entity.markSupported()).thenReturn(true);
        when(entity.read()).thenReturn(1);
        response.setEntityStream(entity);
        assertTrue(response.hasEntity());
        verify(entity).mark(1);
    }

    @Test
    public void testsThatHasEntityWhenEntityStreamDoesNotSupportMarkReset() throws Exception {
        InputStream entity = mock(InputStream.class);
        when(entity.available()).thenReturn(1);
        response.setEntityStream(entity);
        assertTrue(response.hasEntity());
    }

    @Test
    public void testsThatHasEntityReturnsFalseWhenEntityStreamIsNotSet() {
        assertFalse(response.hasEntity());
    }

    @Test
    public void getsEntityStream() {
        InputStream entityStream = mock(InputStream.class);
        response.setEntityStream(entityStream);
        assertSame(entityStream, response.getEntityStream());
    }

    @Test
    public void closesPreviewsEntityStreamWhenSetsNew() throws Exception {
        InputStream entityStream = mock(InputStream.class);
        response.setEntityStream(entityStream);
        response.setEntityStream(mock(InputStream.class));

        verify(entityStream).close();
    }

    private Date date(int year, int month, int day, int hours, int minutes, int seconds, String timeZone) {
        Calendar calendar = Calendar.getInstance();

        calendar.setTimeZone(TimeZone.getTimeZone(timeZone));

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);

        return calendar.getTime();
    }
}