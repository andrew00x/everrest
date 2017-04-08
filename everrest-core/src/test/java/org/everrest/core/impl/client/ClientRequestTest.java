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
import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.DefaultProviderBinder;
import org.everrest.core.impl.provider.StringEntityProvider;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
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
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LANGUAGE;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.COOKIE;
import static javax.ws.rs.core.HttpHeaders.DATE;
import static org.everrest.core.util.ParameterizedTypeImpl.newParameterizedType;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientRequestTest {
    private ProviderBinder providers;
    private ConfigurationProperties properties;
    private EverrestClient client;
    private URI uri = URI.create("http://localhost:8080/a/b");
    private ClientRequest request;

    @Before
    public void setUp() throws Exception {
        client = mock(EverrestClient.class);
        providers = mock(DefaultProviderBinder.class);
        properties = mock(ConfigurationProperties.class);
        request = new ClientRequest(uri, client, providers, properties);
    }

    @Test
    public void getsProviderBinder() {
        assertSame(providers, request.getProviders());
    }

    @Test
    public void getsConfiguration() {
        when(properties.getProperties()).thenReturn(ImmutableMap.of("name", "value"));
        when(providers.isRegistered(StringEntityProvider.class)).thenReturn(true);
        Configuration configuration = request.getConfiguration();

        assertEquals(ImmutableMap.of("name", "value"), configuration.getProperties());
        assertTrue(configuration.isRegistered(StringEntityProvider.class));
    }

    @Test
    public void getsConfigurationProperty() {
        when(properties.getProperty("name")).thenReturn("value");
        assertEquals("value", request.getProperty("name"));
    }

    @Test
    public void getsNamesOfConfigurationProperties() {
        when(properties.getPropertyNames()).thenReturn(newHashSet("name1", "name2"));
        assertEquals(newHashSet("name1", "name2"), newHashSet(request.getPropertyNames()));
    }

    @Test
    public void setsProperty() {
        request.setProperty("name", "value");
        verify(properties).setProperty("name", "value");
    }

    @Test
    public void removesProperty() {
        request.removeProperty("name");
        verify(properties).removeProperty("name");
    }

    @Test
    public void getsUri() {
        assertEquals(uri, request.getUri());
    }

    @Test
    public void setsUri() {
        request.setUri(URI.create("http://updated.com"));
        assertEquals(URI.create("http://updated.com"), request.getUri());
    }

    @Test
    public void setsMethod() {
        request.setMethod("POST");
        assertEquals("POST", request.getMethod());
    }

    @Test
    public void getsMutableHeadersMap() {
        MultivaluedMap<String, Object> headers = request.getHeaders();
        assertNull(request.getHeaders().getFirst(CONTENT_TYPE));
        headers.putSingle(CONTENT_TYPE, "text/plain");
        assertEquals("text/plain", request.getHeaders().getFirst(CONTENT_TYPE));
    }

    @Test
    public void getsStringHeaders() {
        request.getHeaders().put(ACCEPT, newArrayList(new MediaType("text", "plain"), new MediaType("text", "xml")));
        request.getHeaders().putSingle(CONTENT_TYPE, new MediaType("text", "xml"));

        MultivaluedMap<String, String> expectedResult = new MultivaluedHashMap<>();
        expectedResult.put(ACCEPT, newArrayList("text/plain", "text/xml"));
        expectedResult.putSingle(CONTENT_TYPE, "text/xml");

        MultivaluedMap<String, String> stringHeaders = request.getStringHeaders();
        assertEquals(expectedResult, stringHeaders);
    }

    @Test
    public void changesInHeadersReflectedInStringHeadersView() {
        MultivaluedMap<String, String> expectedResult = new MultivaluedHashMap<>();
        MultivaluedMap<String, String> stringHeaders = request.getStringHeaders();
        assertEquals(expectedResult, stringHeaders);

        request.getHeaders().putSingle(ACCEPT_LANGUAGE, new Locale("ua", "ua"));
        expectedResult.putSingle(ACCEPT_LANGUAGE, "ua-ua");

        assertEquals(expectedResult, stringHeaders);
    }

    @Test
    public void getsStringHeader() {
        request.getHeaders().putSingle(CONTENT_TYPE, new MediaType("text", "xml"));
        assertEquals("text/xml", request.getHeaderString(CONTENT_TYPE));
    }

    @Test
    public void getsDate() {
        request.getHeaders().putSingle(DATE, "Fri, 08 Jan 2010 02:05:00 EET");
        assertTrue(Math.abs(date(2010, 1, 8, 2, 5, 0, "EET").getTime() - request.getDate().getTime()) < 1000);
    }

    @Test
    public void getsLanguage() {
        request.getHeaders().putSingle(CONTENT_LANGUAGE, "en-us");
        assertEquals(new Locale("en", "us"), request.getLanguage());
    }

    @Test
    public void getsContentType() {
        request.getHeaders().putSingle(CONTENT_TYPE, "text/plain");
        assertEquals(new MediaType("text", "plain"), request.getMediaType());
    }

    @Test
    public void getsAcceptableMediaTypes() {
        request.getHeaders().addAll(ACCEPT, "text/plain", "text/html");
        assertEquals(newArrayList(new MediaType("text", "plain"), new MediaType("text", "html")), request.getAcceptableMediaTypes());
    }

    @Test
    public void getsAcceptableLanguages() {
        request.getHeaders().addAll(ACCEPT_LANGUAGE, "en-us", "ua-ua");
        assertEquals(newArrayList(new Locale("en", "us"), new Locale("ua", "ua")), request.getAcceptableLanguages());
    }

    @Test
    public void getsCookies() {
        request.getHeaders().addAll(COOKIE, "$Version=1;name1=value1", "$Version=1;name2=value2");
        assertEquals(ImmutableMap.of("name1", new Cookie("name1", "value1"), "name2", new Cookie("name2", "value2")),
                request.getCookies());
    }

    @Test
    public void checksThatHasEntity() {
        assertFalse(request.hasEntity());
        request.setEntity("entity");
        assertTrue(request.hasEntity());
    }

    @Test
    public void getsEntity() {
        request.setEntity("entity");
        assertEquals("entity", request.getEntity());
    }

    @Test
    public void getsEntityClass() {
        request.setEntity("entity");
        assertEquals(String.class, request.getEntityClass());
    }

    @Test
    public void getsEntityType() {
        request.setEntity("entity");
        assertEquals(String.class, request.getEntityType());
    }

    @Test
    public void setsEntity() {
        request.setEntity("entity");
        assertEquals("entity", request.getEntity());
        assertEquals(String.class, request.getEntityClass());
        assertEquals(String.class, request.getEntityType());
    }

    @Test
    public void setsGenericEntity() {
        List<String> entity = newArrayList("entity");
        request.setEntity(new GenericEntity<List<String>>(entity){});
        assertEquals(entity, request.getEntity());
        assertEquals(ArrayList.class, request.getEntityClass());
        assertEquals(newParameterizedType(List.class, String.class), request.getEntityType());
    }

    @Test
    public void nullEntityResetsEntityType() {
        request.setEntity("entity");
        request.setEntity(null);
        assertNull(request.getEntity());
        assertNull(request.getEntityClass());
        assertNull(request.getEntityType());
    }

    @Test
    public void setsEntityItsAnnotationsAndContentType() {
        request.setEntity("entity", new Annotation[]{new A1Impl()}, new MediaType("text", "plain"));
        assertEquals("entity", request.getEntity());
        assertEquals(String.class, request.getEntityClass());
        assertEquals(String.class, request.getEntityType());
        assertArrayEquals(new Annotation[]{new A1Impl()}, request.getEntityAnnotations());
        assertEquals(new MediaType("text", "plain"), request.getMediaType());
    }

    @Test
    public void preservesMediaTypeAndAnnotationsWhenSetEntity() {
        request.setEntity("entity", new Annotation[]{new A1Impl()}, new MediaType("text", "plain"));
        request.setEntity("updated entity");
        assertEquals("updated entity", request.getEntity());
        assertArrayEquals(new Annotation[]{new A1Impl()}, request.getEntityAnnotations());
        assertEquals(new MediaType("text", "plain"), request.getMediaType());
    }

    @Test
    public void getsAbortResponse() {
        assertNull(request.getAbortResponse());
        Response response = mock(Response.class);
        request.abortWith(response);
        assertEquals(response, request.getAbortResponse());
    }

    @Test
    public void getsAssociatedClient() {
        assertEquals(client, request.getClient());
    }

    @Test
    public void getsEntityStream() {
        OutputStream entityStream = mock(OutputStream.class);
        request.setEntityStream(entityStream);
        assertSame(entityStream, request.getEntityStream());
    }

    @Test
    public void closesPreviewsEntityStreamWhenSetsNew() throws Exception {
        OutputStream entityStream = mock(OutputStream.class);
        request.setEntityStream(entityStream);
        request.setEntityStream(mock(OutputStream.class));

        verify(entityStream).close();
    }

    @interface A1 {
    }

    private static class A1Impl implements A1 {
        @Override
        public Class<? extends Annotation> annotationType() {
            return A1.class;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof A1;
        }
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