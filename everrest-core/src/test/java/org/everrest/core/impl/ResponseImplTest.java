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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import org.everrest.core.ConfigurationProperties;
import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.uri.LinkBuilderImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.stubbing.Answer;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
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
import static javax.ws.rs.core.Response.Status.Family.OTHER;
import static javax.ws.rs.core.Response.Status.OK;
import static org.everrest.core.util.ParameterizedTypeImpl.newParameterizedType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author andrew00x
 */
public class ResponseImplTest {
    @Rule public ExpectedException thrown = ExpectedException.none();

    private ProviderBinder providers;
    private ConfigurationProperties properties;

    @Before
    public void setUp() throws Exception {
        providers = mock(DefaultProviderBinder.class);
        properties = mock(ConfigurationProperties.class);
    }

    @After
    public void tearDown() throws Exception {
        RuntimeDelegate.setInstance(null);
    }

    @Test
    public void getsContentType() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        MediaType mediaType = new MediaType("text", "plain", ImmutableMap.of("charset", "utf-8"));
        headers.putSingle(CONTENT_TYPE, mediaType);
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertSame(mediaType, response.getMediaType());
    }

    @Test
    public void parsesContentTypeHeader() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(CONTENT_TYPE, "text/plain;charset=utf-8");
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(new MediaType("text", "plain", ImmutableMap.of("charset", "utf-8")), response.getMediaType());
    }

    @Test
    public void getsContentLanguage() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        Locale locale = new Locale("en", "GB");
        headers.putSingle(CONTENT_LANGUAGE, locale);
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertSame(locale, response.getLanguage());
    }

    @Test
    public void parsesContentLanguageHeader() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(CONTENT_LANGUAGE, "en-GB");
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(new Locale("en", "GB"), response.getLanguage());
    }

    @Test
    public void getsContentLengthMinusOneIfHeaderIsNotSet() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(-1, response.getLength());
    }

    @Test
    public void getsContentLength() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(CONTENT_LENGTH, 3);
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(3, response.getLength());
    }

    @Test
    public void parsesContentLengthHeader() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(CONTENT_LENGTH, "3");
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(3, response.getLength());
    }

    @Test
    public void getsContentLengthMinusOneIfParsingOfHeaderFails() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(CONTENT_LENGTH, "one");
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(-1, response.getLength());
    }

    @Test
    public void getsAllowedMethods() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.put(ALLOW, newArrayList("get", "Put", "POST"));
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(newHashSet("GET", "PUT", "POST"), response.getAllowedMethods());
    }

    @Test
    public void getsEmptySetIfAllowHeaderIsNotSet() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(newHashSet(), response.getAllowedMethods());
    }

    @Test
    public void getsSetCookieHeader() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

        List<Object> cookiesList = newArrayList(new NewCookie("name", "andrew"), new NewCookie("company", "codenvy", "/path", "codenvy.com", 1, "comment", 300, null, true, true));
        headers.put(SET_COOKIE, cookiesList);

        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);
        Map<String, NewCookie> expectedCookies = ImmutableMap.of("name", new NewCookie("name", "andrew"),
                                                                 "company", new NewCookie("company", "codenvy", "/path", "codenvy.com", 1, "comment", 300, null, true, true));

        assertEquals(expectedCookies, response.getCookies());
    }

    @Test
    public void parsesSetCookieHeader() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

        headers.put(SET_COOKIE, newArrayList("name=andrew",
                                             "company=codenvy;version=1;paTh=/path;Domain=codenvy.com;comment=\"comment\";max-age=300;HttpOnly;secure"));
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);
        Map<String, NewCookie> expectedCookies = ImmutableMap.of("name", new NewCookie("name", "andrew"),
                                                                 "company", new NewCookie("company", "codenvy", "/path", "codenvy.com", 1, "comment", 300, null, true, true));

        assertEquals(expectedCookies, response.getCookies());
    }

    @Test
    public void getsEmptyMapIfSetCookieHeaderIsNotSet() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(newHashMap(), response.getCookies());
    }

    @Test
    public void getsEntityTag() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        EntityTag entityTag = new EntityTag("bar");
        headers.putSingle(ETAG, entityTag);
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertSame(entityTag, response.getEntityTag());
    }

    @Test
    public void parsesEntityTagHeader() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(ETAG, "\"bar\"");
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(new EntityTag("bar"), response.getEntityTag());
    }

    @Test
    public void getsDate() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        Date entityTag = new Date();
        headers.putSingle(DATE, entityTag);
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertSame(entityTag, response.getDate());
    }

    @Test
    public void parsesDateHeader() throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        Date date = new Date();
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(DATE, dateFormat.format(date));
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(dateFormat.format(date), dateFormat.format(response.getDate()));
    }

    @Test
    public void getsLastModified() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        Date entityTag = new Date();
        headers.putSingle(LAST_MODIFIED, entityTag);
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertSame(entityTag, response.getLastModified());
    }

    @Test
    public void parsesLastModifiedHeader() throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        Date date = new Date();
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(LAST_MODIFIED, dateFormat.format(date));
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(dateFormat.format(date), dateFormat.format(response.getLastModified()));
    }

    @Test
    public void getsLocation() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        URI location = new URI("http://localhost:8080/bar");
        headers.putSingle(LOCATION, location);
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertSame(location, response.getLocation());
    }

    @Test
    public void parsesLocationHeader() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(LOCATION, "http://localhost:8080/bar");
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(new URI("http://localhost:8080/bar"), response.getLocation());
    }

    @Test
    public void getsLinks() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        Link link = new LinkBuilderImpl().uri("http://localhost:8080/x/y/z").rel("xxx").build();
        headers.put(LINK, newArrayList(link));
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(newHashSet(link), response.getLinks());
    }

    @Test
    public void parsesLinkHeader() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.put(LINK, newArrayList("< http://localhost:8080/x/y/z  >; rel=xxx"));
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(newHashSet(new LinkBuilderImpl().uri("http://localhost:8080/x/y/z").rel("xxx").build()), response.getLinks());
    }

    @Test
    public void getsEmptySetIfLinkHeaderIsNotSet() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(newHashSet(), response.getLinks());
    }

    @Test
    public void checksLinkPresence() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.put(LINK, newArrayList("< http://localhost:8080/x/y/z  >; rel=xxx"));
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertTrue(response.hasLink("xxx"));
        assertFalse(response.hasLink("yyy"));
    }

    @Test
    public void getsLinkByRelation() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.put(LINK, newArrayList("< http://localhost:8080/x/y/z  >; rel=xxx"));
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(new LinkBuilderImpl().uri("http://localhost:8080/x/y/z").rel("xxx").build(), response.getLink("xxx"));
        assertNull(response.getLink("yyy"));
    }

    @Test
    public void getsLinkBuilderByRelation() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.put(LINK, newArrayList("< http://localhost:8080/x/y/z  >; rel=xxx"));
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(new LinkBuilderImpl().uri("http://localhost:8080/x/y/z").rel("xxx").build(), response.getLinkBuilder("xxx").build());
        assertNull(response.getLinkBuilder("yyy"));
    }

    @Test
    public void getsHeadersAsStringToStringMapAndUsesRuntimeDelegateForConvertValuesToString() throws Exception {
        HeaderDelegate<HeaderValue> headerDelegate = mock(HeaderDelegate.class);
        when(headerDelegate.toString(isA(HeaderValue.class))).thenReturn("bar");
        RuntimeDelegate runtimeDelegate = mock(RuntimeDelegate.class);
        when(runtimeDelegate.createHeaderDelegate(HeaderValue.class)).thenReturn(headerDelegate);
        RuntimeDelegate.setInstance(runtimeDelegate);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.put("foo", newArrayList(new HeaderValue()));
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(ImmutableMap.of("foo", newArrayList("bar")), response.getStringHeaders());
    }

    @Test
    public void getsHeadersAsStringToStringMapAndUsesToStringMethodOfValueToConvertIt() throws Exception {
        RuntimeDelegate runtimeDelegate = mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(runtimeDelegate);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        HeaderValue headerValue = mock(HeaderValue.class);
        when(headerValue.toString()).thenReturn("bar");
        headers.put("foo", newArrayList(headerValue));
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals(ImmutableMap.of("foo", newArrayList("bar")), response.getStringHeaders());
    }

    @Test
    public void getSingleHeaderAsStringAndUsesRuntimeDelegateForConvertValueToString() throws Exception {
        HeaderDelegate<HeaderValue> headerDelegate = mock(HeaderDelegate.class);
        when(headerDelegate.toString(isA(HeaderValue.class))).thenReturn("bar");
        RuntimeDelegate runtimeDelegate = mock(RuntimeDelegate.class);
        when(runtimeDelegate.createHeaderDelegate(HeaderValue.class)).thenReturn(headerDelegate);
        RuntimeDelegate.setInstance(runtimeDelegate);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.put("foo", newArrayList(new HeaderValue()));
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals("bar", response.getHeaderString("foo"));
    }

    @Test
    public void getMultipleHeaderAsStringAndUsesRuntimeDelegateForConvertValuesToString() throws Exception {
        HeaderDelegate<HeaderValue> headerDelegate = mock(HeaderDelegate.class);
        when(headerDelegate.toString(isA(HeaderValue.class))).thenReturn("bar1", "bar2");
        RuntimeDelegate runtimeDelegate = mock(RuntimeDelegate.class);
        when(runtimeDelegate.createHeaderDelegate(HeaderValue.class)).thenReturn(headerDelegate);
        RuntimeDelegate.setInstance(runtimeDelegate);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.put("foo", newArrayList(new HeaderValue(), new HeaderValue()));
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertEquals("bar1,bar2", response.getHeaderString("foo"));
    }

    @Test
    public void getsNullIfHeaderNotExist() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ResponseImpl response = new ResponseImpl(200, "foo", null, headers);

        assertNull(response.getHeaderString("foo"));
    }

    public static class HeaderValue {
    }

    @Test
    public void getsStatusInfoForKnownStatus() throws Exception {
        ResponseImpl response = new ResponseImpl(200, "foo", null, null);

        assertEquals(OK, response.getStatusInfo());
    }

    @Test
    public void getsUnknownStatusInfoForUnknownStatus() throws Exception {
        ResponseImpl response = new ResponseImpl(0, "foo", null, null);

        Response.StatusType statusInfo = response.getStatusInfo();
        assertEquals(0, statusInfo.getStatusCode());
        assertEquals(OTHER, statusInfo.getFamily());
        assertEquals("Unknown", statusInfo.getReasonPhrase());
    }

    @Test
    public void readsEntityOfType() throws Exception {
        String entity = "to be or not to be";
        InputStream in = new ByteArrayInputStream(entity.getBytes());
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(CONTENT_TYPE, "text/plain");
        MessageBodyReader<String> reader = mockMessageBodyReaderFor(String.class, String.class, null, new MediaType("text", "plain"));
        when(reader.readFrom(eq(String.class), eq(String.class), isNull(Annotation[].class), eq(new MediaType("text", "plain")), isA(MultivaluedMap.class), isA(InputStream.class)))
                .thenAnswer(readEntityAsString());
        ResponseImpl response = new ResponseImpl(200, in, null, headers, providers, properties);

        String readEntity = response.readEntity(String.class);

        assertEquals(entity, readEntity);
        assertEquals(entity, response.getEntity());
    }

    @Test
    public void returnsInputStreamWhenResponseEntityRepresentedByInputStream() throws Exception {
        InputStream entity = new ByteArrayInputStream("to be or not to be".getBytes());
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        MessageBodyReader<InputStream> reader = mockMessageBodyReaderFor(InputStream.class, InputStream.class, null, null);
        when(reader.readFrom(eq(InputStream.class), eq(InputStream.class), isNull(Annotation[].class), isNull(MediaType.class), isA(MultivaluedMap.class), isA(InputStream.class)))
                .thenReturn(entity);
        ResponseImpl response = new ResponseImpl(200, entity, null, headers, providers, properties);

        InputStream readEntity = response.readEntity(InputStream.class);

        assertEquals(entity, readEntity);
    }

    @Test
    public void readsEntityOfTypeWhenStreamWasBuffered() throws Exception {
        String entity = "to be or not to be";
        InputStream in = new ByteArrayInputStream(entity.getBytes());
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(CONTENT_TYPE, "text/plain");
        MessageBodyReader<String> reader = mockMessageBodyReaderFor(String.class, String.class, null, new MediaType("text", "plain"));

        when(reader.readFrom(eq(String.class), eq(String.class), isNull(Annotation[].class), eq(new MediaType("text", "plain")), isA(MultivaluedMap.class), isA(InputStream.class)))
                .thenAnswer(readEntityAsString());

        ResponseImpl response = new ResponseImpl(200, in, null, headers, providers, properties);
        assertTrue(response.bufferEntity());

        String readEntity = response.readEntity(String.class);
        String nextReadEntity = response.readEntity(String.class);
        String nextNextReadEntity = response.readEntity(String.class);

        assertEquals(entity, readEntity);
        assertEquals(entity, nextReadEntity);
        assertEquals(entity, nextNextReadEntity);
        assertEquals(entity, response.getEntity());
    }

    @Test
    public void throwsExceptionWhenReadEntityFromEmptyStream() throws Exception {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ResponseImpl response = new ResponseImpl(200, in, null, headers, providers, properties);

        thrown.expect(IllegalStateException.class);
        response.readEntity(String.class);
    }

    @Test
    public void throwsExceptionWhenMessageBodyReaderIsNotAvailable() throws Exception {
        InputStream in = new ByteArrayInputStream(new byte[1]);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ResponseImpl response = new ResponseImpl(200, in, null, headers, providers, properties);

        thrown.expect(ProcessingException.class);
        thrown.expectMessage("Unsupported entity type String");
        response.readEntity(String.class);
    }

    @Test
    public void readsEntityOfGenericType() throws Exception {
        InputStream in = new ByteArrayInputStream("to be or not to be".getBytes());
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(CONTENT_TYPE, "text/plain");
        MessageBodyReader<List> reader = mockMessageBodyReaderFor(List.class, newParameterizedType(List.class, String.class), null, new MediaType("text", "plain"));
        when(reader.readFrom(eq(List.class), eq(newParameterizedType(List.class, String.class)), isNull(Annotation[].class), eq(new MediaType("text", "plain")), isA(MultivaluedMap.class), isA(InputStream.class)))
                .thenAnswer(readEntityAsListOfStrings());
        ResponseImpl response = new ResponseImpl(200, in, null, headers, providers, properties);

        List<String> readEntity = response.readEntity(new GenericType<List<String>>(){});

        assertEquals(newArrayList("to", "be", "or", "not", "to", "be"), readEntity);
        assertEquals(newArrayList("to", "be", "or", "not", "to", "be"), response.getEntity());
    }

    @Test
    public void readsEntityOfTypeWithAnnotations() throws Exception {
        String entity = "to be or not to be";
        Annotation[] annotations = new Annotation[]{new A8Impl()};
        InputStream in = new ByteArrayInputStream(entity.getBytes());
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(CONTENT_TYPE, "text/plain");
        MessageBodyReader<String> reader = mockMessageBodyReaderFor(String.class, String.class, annotations, new MediaType("text", "plain"));
        when(reader.readFrom(eq(String.class), eq(String.class), aryEq(annotations), eq(new MediaType("text", "plain")), isA(MultivaluedMap.class), isA(InputStream.class)))
                .thenAnswer(readEntityAsString());
        ResponseImpl response = new ResponseImpl(200, in, null, headers, providers, properties);

        String readEntity = response.readEntity(String.class, annotations);

        assertEquals(entity, readEntity);
        assertEquals(entity, response.getEntity());
    }

    @Test
    public void readsEntityOfGenericTypeWithAnnotations() throws Exception {
        Annotation[] annotations = new Annotation[]{new A8Impl()};
        InputStream in = new ByteArrayInputStream("to be or not to be".getBytes());
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(CONTENT_TYPE, "text/plain");
        MessageBodyReader<List> reader = mockMessageBodyReaderFor(List.class, newParameterizedType(List.class, String.class), annotations, new MediaType("text", "plain"));
        when(reader.readFrom(eq(List.class), eq(newParameterizedType(List.class, String.class)), aryEq(annotations), eq(new MediaType("text", "plain")), isA(MultivaluedMap.class), isA(InputStream.class)))
                .thenAnswer(readEntityAsListOfStrings());
        ResponseImpl response = new ResponseImpl(200, in, null, headers, providers, properties);

        List<String> readEntity = response.readEntity(new GenericType<List<String>>(){}, annotations);

        assertEquals(newArrayList("to", "be", "or", "not", "to", "be"), readEntity);
        assertEquals(newArrayList("to", "be", "or", "not", "to", "be"), response.getEntity());
    }

    @Test
    public void buffersEntity() throws Exception {
        String entity = "to be or not to be";
        InputStream in = new ByteArrayInputStream(entity.getBytes());
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ResponseImpl response = new ResponseImpl(200, in, null, headers, providers, properties);

        assertTrue(response.bufferEntity());
        assertTrue(response.isEntityStreamBuffered());
        assertNotSame(in, response.getEntityStream());
        assertEquals(entity, CharStreams.toString(new InputStreamReader(response.getEntityStream())));
    }

    @Test
    public void doesNotBufferEmptyStream() throws Exception {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ResponseImpl response = new ResponseImpl(200, in, null, headers, providers, properties);

        assertFalse(response.bufferEntity());
        assertFalse(response.isEntityStreamBuffered());
        assertSame(in, response.getEntityStream());
    }

    @Test
    public void checksEntityPresenceWhenEntityIsSet() {
        String entity = "to be or not to be";
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ResponseImpl response = new ResponseImpl(200, entity, null, headers);
        assertTrue(response.hasEntity());
    }

    @Test
    public void closes() {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ResponseImpl response = new ResponseImpl(200, null, null, headers);
        response.close();
        assertTrue(response.isClosed());
    }

    private static Answer<String> readEntityAsString() {
        return invocation -> {
            InputStream in = (InputStream) invocation.getArguments()[5];
            return CharStreams.toString(new InputStreamReader(in));
        };
    }

    private static Answer<List<String>> readEntityAsListOfStrings() {
        return invocation -> {
            InputStream in = (InputStream) invocation.getArguments()[5];
            return Splitter.on(' ').splitToList(CharStreams.toString(new InputStreamReader(in)));
        };
    }

    @SuppressWarnings("unchecked")
    private <T> MessageBodyReader<T> mockMessageBodyReaderFor(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        MessageBodyReader<T> reader = mock(MessageBodyReader.class);
        when(providers.getMessageBodyReader(eq(type), eq(genericType), aryEq(annotations), eq(mediaType))).thenReturn(reader);
        return reader;
    }

    @interface A8 {
    }

    static class A8Impl implements A8 {
        @Override
        public Class<? extends Annotation> annotationType() {
            return A8.class;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof A8;
        }
    }
}
