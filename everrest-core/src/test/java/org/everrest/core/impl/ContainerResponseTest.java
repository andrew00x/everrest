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
package org.everrest.core.impl;

import com.google.common.collect.ImmutableMap;
import org.everrest.core.ConfigurationProperties;
import org.everrest.core.ContainerResponseWriter;
import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.provider.StringEntityProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
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
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static org.everrest.core.util.ParameterizedTypeImpl.newParameterizedType;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContainerResponseTest {
    private ProviderBinder providers;
    private ContainerRequest containerRequest;
    private ConfigurationProperties containerRequestProperties;
    private ContainerResponseWriter containerResponseWriter;
    private ByteArrayOutputStream entityStream;

    private ContainerResponse containerResponse;

    @Before
    public void setUp() throws Exception {
        providers = mock(DefaultProviderBinder.class);
        containerRequest = mock(ContainerRequest.class);
        containerRequestProperties = mock(ConfigurationProperties.class);
        when(containerRequest.getProperties()).thenReturn(containerRequestProperties);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getProviders()).thenReturn(providers);
        when(applicationContext.getContainerRequest()).thenReturn(containerRequest);
        when(applicationContext.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        ApplicationContext.setCurrent(applicationContext);

        entityStream = new ByteArrayOutputStream();
        containerResponseWriter = mock(ContainerResponseWriter.class);
        containerResponse = new ContainerResponse(containerResponseWriter);
        doAnswer(writeEntity()).when(containerResponseWriter).writeBody(same(containerResponse), any(MessageBodyWriter.class));
        when(containerResponseWriter.getOutputStream()).thenReturn(entityStream);
    }

    @After
    public void tearDown() throws Exception {
        ApplicationContext.setCurrent(null);
    }

    @SuppressWarnings("unchecked")
    private Answer<Void> writeEntity() {
        return invocation -> {
            MessageBodyWriter messageBodyWriter = (MessageBodyWriter) invocation.getArguments()[1];
            Object entity = containerResponse.getEntity();
            if (entity != null) {
                messageBodyWriter.writeTo(entity,
                                          entity.getClass(),
                                          containerResponse.getEntityType(),
                                          null,
                                          containerResponse.getMediaType(),
                                          containerResponse.getHeaders(),
                                          entityStream);
            }
            return null;
        };
    }

    @Test
    public void setsNullResponse() throws Exception {
        containerResponse.setResponse(null);

        assertEquals(0, containerResponse.getStatus());
        assertNull(containerResponse.getEntity());
        assertNull(containerResponse.getEntityType());
        assertTrue(containerResponse.getHeaders().isEmpty());
        assertNull(containerResponse.getMediaType());
    }

    @Test
    public void setsResponse() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(CONTENT_TYPE, TEXT_PLAIN_TYPE);
        Response response = mockResponse(200, headers, "foo");

        containerResponse.setResponse(response);

        assertEquals(200, containerResponse.getStatus());
        assertEquals("foo", containerResponse.getEntity());
        assertEquals(String.class, containerResponse.getEntityType());
        assertEquals(headers, containerResponse.getHeaders());
        assertEquals(TEXT_PLAIN_TYPE, containerResponse.getMediaType());
    }

    @Test
    public void setsResponseWithGenericEntity() throws Exception {
        GenericEntity<List<String>> genericEntity = new GenericEntity<List<String>>(newArrayList("foo")) {
        };
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(CONTENT_TYPE, TEXT_PLAIN_TYPE);
        Response response = mockResponse(200, headers, genericEntity);

        containerResponse.setResponse(response);

        assertEquals(200, containerResponse.getStatus());
        assertEquals(newArrayList("foo"), containerResponse.getEntity());
        assertEquals(newParameterizedType(List.class, String.class), containerResponse.getEntityType());
        assertEquals(headers, containerResponse.getHeaders());
        assertEquals(TEXT_PLAIN_TYPE, containerResponse.getMediaType());
    }

    @Test
    public void setsStatusByCode() {
        containerResponse.setStatus(204);
        assertEquals(204, containerResponse.getStatus());
        assertEquals(Status.fromStatusCode(204), containerResponse.getStatusInfo());
    }

    @Test
    public void setsStatusByStatusInfo() {
        containerResponse.setStatusInfo(Status.fromStatusCode(404));
        assertEquals(404, containerResponse.getStatus());
        assertEquals(Status.fromStatusCode(404), containerResponse.getStatusInfo());
    }

    @Test
    public void getsStringHeaders() {
        containerResponse.getHeaders().putSingle(CONTENT_TYPE, new MediaType("application", "json"));
        containerResponse.getHeaders().putSingle(ETAG, new EntityTag("e_tag", true));

        MultivaluedMap<String, String> stringHeaders = containerResponse.getStringHeaders();
        assertEquals(2, stringHeaders.size());
        assertEquals(newArrayList("W/\"e_tag\""), stringHeaders.get(ETAG));
        assertEquals(newArrayList("application/json"), stringHeaders.get(CONTENT_TYPE));

        containerResponse.getHeaders().remove(ETAG);
        containerResponse.getHeaders().putSingle(LOCATION, URI.create("http://localhost:8080/a/b"));

        assertEquals(2, stringHeaders.size());
        assertNull(stringHeaders.get(ETAG));
        assertEquals(newArrayList("http://localhost:8080/a/b"), stringHeaders.get(LOCATION));
    }

    @Test
    public void getsHeaderAsString() {
        containerResponse.getHeaders().put("X-Header1", newArrayList("hello", "world"));

        assertEquals("hello,world", containerResponse.getHeaderString("X-Header1"));
    }

    @Test
    public void getsAllowedMethods() {
        containerResponse.getHeaders().put(ALLOW, newArrayList(GET, "POst", "put", GET));
        assertEquals(newHashSet(GET, "POST", "PUT"), containerResponse.getAllowedMethods());
    }

    @Test
    public void returnsAllowedMethodsAsEmptySetIfAllowHeaderIsNotSet() {
        assertTrue(containerResponse.getAllowedMethods().isEmpty());
    }

    @Test
    public void getsDateHeader() {
        containerResponse.getHeaders().putSingle(DATE, "Fri, 27 Nov 1981 13:35:00 EET");
        Date date = containerResponse.getDate();
        assertTrue(Math.abs(date(1981, 11, 27, 13, 35, 0, "EET").getTime() - date.getTime()) < 1000);
    }

    @Test
    public void getsLanguageHeader() {
        containerResponse.getHeaders().putSingle(CONTENT_LANGUAGE, "en-us");
        assertEquals(new Locale("en", "us"), containerResponse.getLanguage());
    }

    @Test
    public void getsContentTypeHeader() {
        containerResponse.getHeaders().putSingle(CONTENT_TYPE, "text/plain");
        assertEquals(new MediaType("text", "plain"), containerResponse.getMediaType());
    }

    @Test
    public void getsContentLength() {
        containerResponse.getHeaders().putSingle(CONTENT_LENGTH, "99");
        assertEquals(99, containerResponse.getLength());
    }

    @Test
    public void getsContentLengthAsMinusOneWhenContentLengthHeaderIsNotSet() {
        assertEquals(-1, containerResponse.getLength());
    }

    @Test
    public void getsContentLengthAsMinusOneWhenContentLengthHeaderIsNotValid() {
        containerResponse.getHeaders().putSingle(CONTENT_LENGTH, "wrong");
        assertEquals(-1, containerResponse.getLength());
    }

    @Test
    public void getsEntityTag() {
        containerResponse.getHeaders().putSingle(ETAG, "W/\"test\"");
        assertEquals(new EntityTag("test", true), containerResponse.getEntityTag());
    }

    @Test
    public void getsLastModified() {
        containerResponse.getHeaders().putSingle(LAST_MODIFIED, "Fri, 27 Nov 1981 13:35:00 EET");
        Date lastModified = containerResponse.getLastModified();
        assertTrue(Math.abs(date(1981, 11, 27, 13, 35, 0, "EET").getTime() - lastModified.getTime()) < 1000);
    }

    @Test
    public void getsLocation() {
        containerResponse.getHeaders().putSingle(LOCATION, "http://test.com/foo");
        assertEquals(URI.create("http://test.com/foo"), containerResponse.getLocation());
    }

    @Test
    public void getsLinks() {
        String link = "< http://localhost:8080/x/y/z >; rel=\"xxx\"; title=\"yyy\"";
        containerResponse.getHeaders().putSingle(LINK, link);
        assertEquals(newHashSet(Link.valueOf(link)), containerResponse.getLinks());
    }

    @Test
    public void getsLinksAsEmptySetWhenLinkHeaderIsNotSet() {
        assertTrue(containerResponse.getLinks().isEmpty());
    }

    @Test
    public void testThatHasLink() {
        String link = "< http://localhost:8080/x/y/z >; rel=\"xxx\"";
        containerResponse.getHeaders().putSingle(LINK, link);
        assertTrue(containerResponse.hasLink("xxx"));
        assertFalse(containerResponse.hasLink("yyy"));
    }

    @Test
    public void getsLinkByRelation() {
        String link = "< http://localhost:8080/x/y/z >; rel=\"xxx\"";
        containerResponse.getHeaders().putSingle(LINK, link);
        assertEquals(Link.valueOf(link), containerResponse.getLink("xxx"));
    }

    @Test
    public void getsLinkBuilderByRelation() {
        String link = "< http://localhost:8080/x/y/z >; rel=\"xxx\"";
        containerResponse.getHeaders().putSingle(LINK, link);
        assertEquals(Link.valueOf(link), containerResponse.getLinkBuilder("xxx").build());
    }

    @Test
    public void getsCookies() {
        containerResponse.getHeaders().addAll(SET_COOKIE, "name1=value1", "name2=value2");
        assertEquals(ImmutableMap.of("name1", new NewCookie("name1", "value1"), "name2", new NewCookie("name2", "value2")),
                containerResponse.getCookies());
    }

    @Test
    public void getsCookiesAsEmptyMapWhenSetCookieHeaderIsNotSet() {
        assertTrue(containerResponse.getCookies().isEmpty());
    }

    @Test
    public void checksThatHasEntity() {
        assertFalse(containerResponse.hasEntity());
        containerResponse.setEntity("entity");
        assertTrue(containerResponse.hasEntity());
    }

    @Test
    public void getsEntity() {
        containerResponse.setEntity("entity");
        assertEquals("entity", containerResponse.getEntity());
    }

    @Test
    public void getsEntityClass() {
        containerResponse.setEntity("entity");
        assertEquals(String.class, containerResponse.getEntityClass());
    }

    @Test
    public void getsEntityType() {
        containerResponse.setEntity("entity");
        assertEquals(String.class, containerResponse.getEntityType());
    }

    @Test
    public void setsEntity() {
        containerResponse.setEntity("entity");
        assertEquals("entity", containerResponse.getEntity());
        assertEquals(String.class, containerResponse.getEntityClass());
        assertEquals(String.class, containerResponse.getEntityType());
    }

    @Test
    public void setsGenericEntity() {
        List<String> entity = newArrayList("entity");
        containerResponse.setEntity(new GenericEntity<List<String>>(entity) {
        });
        assertEquals(entity, containerResponse.getEntity());
        assertEquals(ArrayList.class, containerResponse.getEntityClass());
        assertEquals(newParameterizedType(List.class, String.class), containerResponse.getEntityType());
    }

    @Test
    public void nullEntityResetsEntityType() {
        containerResponse.setEntity("entity");
        containerResponse.setEntity(null);
        assertNull(containerResponse.getEntity());
        assertNull(containerResponse.getEntityClass());
        assertNull(containerResponse.getEntityType());
    }

    @Test
    public void setsEntityItsAnnotationsAndContentType() {
        containerResponse.setEntity("entity", new Annotation[]{new A3Impl()}, new MediaType("text", "plain"));
        assertEquals("entity", containerResponse.getEntity());
        assertEquals(String.class, containerResponse.getEntityClass());
        assertEquals(String.class, containerResponse.getEntityType());
        assertArrayEquals(new Annotation[]{new A3Impl()}, containerResponse.getEntityAnnotations());
        assertEquals(new MediaType("text", "plain"), containerResponse.getMediaType());
    }

    @Test
    public void preservesMediaTypeAndAnnotationsWhenSetsEntity() {
        containerResponse.setEntity("entity", new Annotation[]{new A3Impl()}, new MediaType("text", "plain"));
        containerResponse.setEntity("updated entity");
        assertEquals("updated entity", containerResponse.getEntity());
        assertArrayEquals(new Annotation[]{new A3Impl()}, containerResponse.getEntityAnnotations());
        assertEquals(new MediaType("text", "plain"), containerResponse.getMediaType());
    }

    @Test
    public void setsEntityOutputStream() throws Exception {
        OutputStream oldStream = mock(OutputStream.class);
        containerResponse.setEntityStream(oldStream);

        OutputStream newStream = mock(OutputStream.class);
        containerResponse.setEntityStream(newStream);
        verify(oldStream).close();
    }

    @Test
    public void getsResponse() {
        containerResponse.setStatus(200);
        containerResponse.setEntity("foo", new Annotation[0], TEXT_PLAIN_TYPE);
        containerResponse.getHeaders().putSingle(CONTENT_LENGTH, 3);

        Response response = containerResponse.getResponse();
        assertEquals(200, response.getStatus());
        assertEquals(3, response.getLength());
        assertEquals(TEXT_PLAIN_TYPE, response.getMediaType());
        assertEquals("foo", response.getEntity());
    }

    @Test
    public void returnsNullResponseIfStatusIsNotSet() {
        containerResponse.setEntity("foo");
        assertNull(containerResponse.getResponse());
    }

    @Test
    public void writesResponseWhenResponseEntityIsNull() throws Exception {
        when(containerRequest.getMethod()).thenReturn(GET);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        Response response = mockResponse(200, headers, null);
        containerResponse.setResponse(response);

        containerResponse.writeResponse();

        assertEquals(200, containerResponse.getStatus());
        verify(containerResponseWriter).writeHeaders(containerResponse);
        assertEquals(0, entityStream.size());
    }

    @Test
    public void writesResponseWhenResponseEntityIsNotNullRequestMethodIsHeadAndContentTypeIsSetExplicitly() throws Exception {
        when(containerRequest.getMethod()).thenReturn(HEAD);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(CONTENT_TYPE, TEXT_PLAIN_TYPE);
        Response response = mockResponse(200, headers, "foo");
        containerResponse.setResponse(response);

        containerResponse.writeResponse();

        assertEquals(200, containerResponse.getStatus());
        assertEquals(TEXT_PLAIN_TYPE, containerResponse.getMediaType());
        verify(containerResponseWriter).writeHeaders(containerResponse);
        assertEquals(0, entityStream.size());
    }

    @Test
    public void writesResponseWhenResponseEntityIsNotNullAndContentTypeIsSetExplicitly() throws Exception {
        when(providers.getMessageBodyWriter(String.class, String.class, new Annotation[0], TEXT_PLAIN_TYPE)).thenReturn(new StringEntityProvider());
        when(containerRequest.getMethod()).thenReturn(GET);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(CONTENT_TYPE, TEXT_PLAIN_TYPE);
        Response response = mockResponse(200, headers, "foo");
        containerResponse.setResponse(response);

        containerResponse.writeResponse();

        assertEquals(200, containerResponse.getStatus());
        assertEquals(TEXT_PLAIN_TYPE, containerResponse.getMediaType());
        verify(containerResponseWriter).writeHeaders(containerResponse);
        assertEquals("foo", entityStream.toString());
    }

    @Test
    public void writesResponseWhenResponseEntityIsNotNullAndAcceptHeaderIsSetAndContentTypeIsNotSet() throws Exception {
        when(providers.getMessageBodyWriter(String.class, String.class, new Annotation[0], TEXT_PLAIN_TYPE)).thenReturn(new StringEntityProvider());
        when(containerRequest.getMethod()).thenReturn(GET);
        when(containerRequest.getAcceptableMediaTypes()).thenReturn(newArrayList(TEXT_PLAIN_TYPE));

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        Response response = mockResponse(200, headers, "foo");
        containerResponse.setResponse(response);

        containerResponse.writeResponse();

        assertEquals(200, containerResponse.getStatus());
        assertEquals(TEXT_PLAIN_TYPE, containerResponse.getMediaType());
        verify(containerResponseWriter).writeHeaders(containerResponse);
        assertEquals("foo", entityStream.toString());
    }

    @Test
    public void writesResponseWhenResponseEntityIsNotNullAndAcceptHeaderIsNotSetAndContentTypeIsNotSet() throws Exception {
        when(providers.getMessageBodyWriter(String.class, String.class, new Annotation[0], TEXT_PLAIN_TYPE)).thenReturn(new StringEntityProvider());
        when(providers.getAcceptableWriterMediaTypes(String.class, String.class, new Annotation[0])).thenReturn(newArrayList(TEXT_PLAIN_TYPE));
        when(containerRequest.getMethod()).thenReturn(GET);
        when(containerRequest.getAcceptableMediaTypes()).thenReturn(newArrayList(WILDCARD_TYPE));
        when(containerRequest.getAcceptableMediaType(newArrayList(TEXT_PLAIN_TYPE))).thenReturn(TEXT_PLAIN_TYPE);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        Response response = mockResponse(200, headers, "foo");
        containerResponse.setResponse(response);

        containerResponse.writeResponse();

        assertEquals(200, containerResponse.getStatus());
        assertEquals(TEXT_PLAIN_TYPE, containerResponse.getMediaType());
        verify(containerResponseWriter).writeHeaders(containerResponse);
        assertEquals("foo", entityStream.toString());
    }

    @Test
    public void createsNotAcceptableResponseWhenNotFoundAnyWriterForEntity() throws Exception {
        when(containerRequest.getMethod()).thenReturn(GET);
        when(containerRequest.getAcceptableMediaTypes()).thenReturn(newArrayList(WILDCARD_TYPE));

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        Response response = mockResponse(200, headers, "foo");
        containerResponse.setResponse(response);

        containerResponse.writeResponse();

        assertEquals(406, containerResponse.getStatus());
        assertEquals("Unsupported entity type String. There is no any MessageBodyWriter that can serialize this type to application/octet-stream", containerResponse.getEntity());
        verify(containerResponseWriter).writeHeaders(containerResponse);
        verify(containerResponseWriter).writeBody(same(containerResponse), isA(StringEntityProvider.class));
    }

    @Test
    public void neverCreatesNotAcceptableResponseForHeadRequestEvenWhenNotFoundAnyWriterForEntity() throws Exception {
        when(containerRequest.getMethod()).thenReturn(HEAD);
        when(containerRequest.getAcceptableMediaTypes()).thenReturn(newArrayList(WILDCARD_TYPE));

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        Response response = mockResponse(200, headers, "foo");
        containerResponse.setResponse(response);

        containerResponse.writeResponse();

        assertEquals(200, containerResponse.getStatus());
        verify(containerResponseWriter).writeHeaders(containerResponse);
        assertEquals(0, entityStream.size());
    }

    private Response mockResponse(int status, MultivaluedMap<String, Object> headers, Object entity) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        when(response.getStatusInfo()).thenReturn(Status.fromStatusCode(status));
        when(response.getHeaders()).thenReturn(headers);
        when(response.getEntity()).thenReturn(entity);
        return response;
    }

    private static Date date(int year, int month, int day, int hours, int minutes, int seconds, String timeZone) {
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

    @interface A3 {
    }

    static class A3Impl implements A3 {
        @Override
        public Class<? extends Annotation> annotationType() {
            return A3.class;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof A3;
        }
    }
}