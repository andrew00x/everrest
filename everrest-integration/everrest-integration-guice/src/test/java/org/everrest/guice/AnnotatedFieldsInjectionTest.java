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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Module;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.everrest.core.impl.ContainerResponse;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(DataProviderRunner.class)
public class AnnotatedFieldsInjectionTest extends BaseTest {
    @Override
    protected List<Module> getModules() {
        return newArrayList((Module) binder -> {
            binder.bind(StringPathParamResource.class);
            binder.bind(EncodedStringPathParamResource.class);
            binder.bind(StringPathParamResource.class);
            binder.bind(ListOfStringsPathParamResource.class);
            binder.bind(SetOfStringsPathParamResource.class);
            binder.bind(SortedSetOfStringsPathParamResource.class);
            binder.bind(StringValueOfPathParamResource.class);
            binder.bind(MultiplePathParamResource.class);
            binder.bind(PrimitivePathParamResource.class);
            binder.bind(StringQueryParamResource.class);
            binder.bind(EncodedStringQueryParamResource.class);
            binder.bind(StringQueryParamResource.class);
            binder.bind(DefaultValueQueryParamResource.class);
            binder.bind(ListOfStringsQueryParamResource.class);
            binder.bind(SetOfStringsQueryParamResource.class);
            binder.bind(SortedSetOfStringsQueryParamResource.class);
            binder.bind(StringValueOfQueryParamResource.class);
            binder.bind(MultipleQueryParamResource.class);
            binder.bind(PrimitiveQueryParamResource.class);
            binder.bind(StringMatrixParamResource.class);
            binder.bind(EncodedStringMatrixParamResource.class);
            binder.bind(StringMatrixParamResource.class);
            binder.bind(DefaultValueMatrixParamResource.class);
            binder.bind(ListOfStringsMatrixParamResource.class);
            binder.bind(SetOfStringsMatrixParamResource.class);
            binder.bind(SortedSetOfStringsMatrixParamResource.class);
            binder.bind(StringValueOfMatrixParamResource.class);
            binder.bind(MultipleMatrixParamResource.class);
            binder.bind(PrimitiveMatrixParamResource.class);
            binder.bind(CookieCookieParamResource.class);
            binder.bind(StringCookieParamResource.class);
            binder.bind(DefaultValueCookieParamResource.class);
            binder.bind(ListOfStringsCookieParamResource.class);
            binder.bind(SetOfStringsCookieParamResource.class);
            binder.bind(SortedSetOfStringsCookieParamResource.class);
            binder.bind(StringValueOfCookieParamResource.class);
            binder.bind(MultipleCookieParamResource.class);
            binder.bind(PrimitiveCookieParamResource.class);
            binder.bind(StringHeaderParamResource.class);
            binder.bind(DefaultValueHeaderParamResource.class);
            binder.bind(ListOfStringsHeaderParamResource.class);
            binder.bind(SetOfStringsHeaderParamResource.class);
            binder.bind(SortedSetOfStringsHeaderParamResource.class);
            binder.bind(StringValueOfHeaderParamResource.class);
            binder.bind(MultipleHeaderParamResource.class);
            binder.bind(PrimitiveHeaderParamResource.class);
            binder.bind(RequestResource.class);
            binder.bind(UriInfoResource.class);
            binder.bind(HttpHeadersResource.class);
            binder.bind(SecurityContextResource.class);
            binder.bind(ProvidersResource.class);
            binder.bind(ApplicationResource.class);
        });
    }

    @UseDataProvider("injectParametersTestData")
    @Test
    public void injectsParameters(String path, Map<String, List<String>> requestHeaders, Object responseEntity) throws Exception {
        ContainerResponse response = launcher.service("POST", path, "", requestHeaders, null, null);

        assertEquals(responseEntity, response.getEntity());
    }

    @DataProvider
    public static Object[][] injectParametersTestData() {
        return new Object[][]{
                {"/a1/test/1",    null, "test"},
                {"/a2/te%20st/1", null, "te%20st"},
                {"/a1/te%20st/1", null, "te st"},
                {"/a3/test/1",    null, newArrayList("test")},
                {"/a4/test/1",    null, newHashSet("test")},
                {"/a5/test/1",    null, newTreeSet(newArrayList("test"))},
                {"/a6/123/1",     null, 123},
                {"/a7/foo/1/bar", null, "foobar"},
                {"/a8/123/1",     null, 123},

                {"/b1/1?x=test",      null, "test"},
                {"/b2/1?x=te%20st",   null, "te%20st"},
                {"/b1/1?x=te%20st",   null, "te st"},
                {"/b3/1",             null, "default"},
                {"/b4/1?x=foo&x=bar", null, newArrayList("foo", "bar")},
                {"/b5/1?x=foo&x=bar", null, newHashSet("foo", "bar")},
                {"/b6/1?x=foo&x=bar", null, newTreeSet(newArrayList("foo", "bar"))},
                {"/b7/1?x=123",       null, 123},
                {"/b8/1?x=foo&y=bar", null, "foobar"},
                {"/b9/1?x=123",       null, 123},

                {"/c1/1;x=test",      null, "test"},
                {"/c2/1;x=te%20st",   null, "te%20st"},
                {"/c1/1;x=te%20st",   null, "te st"},
                {"/c3/1",             null, "default"},
                {"/c4/1;x=foo;x=bar", null, newArrayList("foo", "bar")},
                {"/c5/1;x=foo;x=bar", null, newHashSet("foo", "bar")},
                {"/c6/1;x=foo;x=bar", null, newTreeSet(newArrayList("foo", "bar"))},
                {"/c7/1;x=123",       null, 123},
                {"/c8/1;x=foo;y=bar", null, "foobar"},
                {"/c9/1;x=123",       null, 123},

                {"/d1/1", ImmutableMap.of("Cookie", newArrayList("x=test")),      new Cookie("x", "test")},
                {"/d2/1", ImmutableMap.of("Cookie", newArrayList("x=test")),      "test"},
                {"/d3/1", null,                                                   "default"},
                {"/d4/1", ImmutableMap.of("Cookie", newArrayList("x=test")),      newArrayList("test")},
                {"/d5/1", ImmutableMap.of("Cookie", newArrayList("x=test")),      newHashSet("test")},
                {"/d6/1", ImmutableMap.of("Cookie", newArrayList("x=test")),      newTreeSet(newArrayList("test"))},
                {"/d7/1", ImmutableMap.of("Cookie", newArrayList("x=123")),       123},
                {"/d8/1", ImmutableMap.of("Cookie", newArrayList("x=foo,y=bar")), "foobar"},
                {"/d9/1", ImmutableMap.of("Cookie", newArrayList("x=123")),       123},

                {"/e1/1", ImmutableMap.of("x", newArrayList("test")),                          "test"},
                {"/e2/1", null,                                                                "default"},
                {"/e3/1", ImmutableMap.of("x", newArrayList("foo", "bar")),                    newArrayList("foo", "bar")},
                {"/e4/1", ImmutableMap.of("x", newArrayList("foo", "bar")),                    newHashSet("foo", "bar")},
                {"/e5/1", ImmutableMap.of("x", newArrayList("foo", "bar")),                    newTreeSet(newArrayList("foo", "bar"))},
                {"/e6/1", ImmutableMap.of("x", newArrayList("123")),                           123},
                {"/e7/1", ImmutableMap.of("x", newArrayList("foo"), "y", newArrayList("bar")), "foobar"},
                {"/e8/1", ImmutableMap.of("x", newArrayList("123")),                           123},
        };
    }

    @Test
    public void injectsUriInfo() throws Exception {
        ContainerResponse response = launcher.service("POST", "/f/1", "", null, null, null);

        assertEquals(String.format("Expected %s injected", UriInfo.class), "UriInfo", response.getEntity());
    }

    @Test
    public void injectsRequest() throws Exception {
        ContainerResponse response = launcher.service("POST", "/g/1", "", null, null, null);

        assertEquals(String.format("Expected %s injected", Request.class), "Request", response.getEntity());
    }

    @Test
    public void injectsHttpHeaders() throws Exception {
        ContainerResponse response = launcher.service("POST", "/h/1", "", null, null, null);

        assertEquals(String.format("Expected %s injected", HttpHeaders.class), "HttpHeaders", response.getEntity());
    }

    @Test
    public void injectsSecurityContext() throws Exception {
        ContainerResponse response = launcher.service("POST", "/i/1", "", null, null, null);

        assertEquals(String.format("Expected %s injected", SecurityContext.class), "SecurityContext", response.getEntity());
    }

    @Test
    public void injectsProviders() throws Exception {
        ContainerResponse response = launcher.service("POST", "/j/1", "", null, null, null);

        assertEquals(String.format("Expected %s injected", Providers.class), "Providers", response.getEntity());
    }

    @Test
    public void injectsApplication() throws Exception {
        ContainerResponse response = launcher.service("POST", "/k/1", "", null, null, null);

        assertEquals(String.format("Expected %s injected", Application.class), "Application", response.getEntity());
    }

    @Path("a1/{x}")
    public static class StringPathParamResource {
        @PathParam("x")
        private String x;

        @Path("1")
        @POST
        public String m1() {
            return x;
        }
    }

    @Path("a2/{x}")
    public static class EncodedStringPathParamResource {
        @PathParam("x")
        @Encoded
        private String x;

        @Path("1")
        @POST
        public String m1() {
            return x;
        }
    }

    @Path("a3/{x}")
    public static class ListOfStringsPathParamResource {
        @PathParam("x")
        private List<String> x;

        @Path("1")
        @POST
        public GenericEntity<List<String>> m1() {
            return new GenericEntity<List<String>>(x) {
            };
        }
    }

    @Path("a4/{x}")
    public static class SetOfStringsPathParamResource {
        @PathParam("x")
        private Set<String> x;

        @Path("1")
        @POST
        public GenericEntity<Set<String>> m1() {
            return new GenericEntity<Set<String>>(x) {
            };
        }
    }

    @Path("a5/{x}")
    public static class SortedSetOfStringsPathParamResource {
        @PathParam("x")
        private SortedSet<String> x;

        @Path("1")
        @POST
        public GenericEntity<SortedSet<String>> m1() {
            return new GenericEntity<SortedSet<String>>(x) {
            };
        }
    }

    @Path("a6/{x}")
    public static class StringValueOfPathParamResource {
        @PathParam("x")
        private Integer x;

        @Path("1")
        @POST
        public Integer m1() {
            return x;
        }
    }

    @Path("a7/{x}/1/{y}")
    public static class MultiplePathParamResource {
        @PathParam("x")
        private String x;
        @PathParam("y")
        private String y;

        @POST
        public String m1() {
            return x + y;
        }
    }

    @Path("a8/{x}")
    public static class PrimitivePathParamResource {
        @PathParam("x")
        private int x;

        @Path("1")
        @POST
        public int m1() {
            return x;
        }
    }


    @Path("b1")
    public static class StringQueryParamResource {
        @QueryParam("x")
        private String x;

        @Path("1")
        @POST
        public String m1() {
            return x;
        }
    }

    @Path("b2")
    public static class EncodedStringQueryParamResource {
        @QueryParam("x")
        @Encoded
        private String x;

        @Path("1")
        @POST
        public String m1() {
            return x;
        }
    }

    @Path("b3")
    public static class DefaultValueQueryParamResource {
        @QueryParam("x")
        @DefaultValue("default")
        private String x;

        @Path("1")
        @POST
        public String m1() {
            return x;
        }
    }

    @Path("b4")
    public static class ListOfStringsQueryParamResource {
        @QueryParam("x")
        private List<String> x;

        @Path("1")
        @POST
        public GenericEntity<List<String>> m1() {
            return new GenericEntity<List<String>>(x) {
            };
        }
    }

    @Path("b5")
    public static class SetOfStringsQueryParamResource {
        @QueryParam("x")
        private Set<String> x;

        @Path("1")
        @POST
        public GenericEntity<Set<String>> m1() {
            return new GenericEntity<Set<String>>(x) {
            };
        }
    }

    @Path("b6")
    public static class SortedSetOfStringsQueryParamResource {
        @QueryParam("x")
        private SortedSet<String> x;

        @Path("1")
        @POST
        public GenericEntity<SortedSet<String>> m1() {
            return new GenericEntity<SortedSet<String>>(x) {
            };
        }
    }

    @Path("b7")
    public static class StringValueOfQueryParamResource {
        @QueryParam("x")
        private Integer x;

        @Path("1")
        @POST
        public Integer m1() {
            return x;
        }
    }

    @Path("b8")
    public static class MultipleQueryParamResource {
        @QueryParam("x")
        private String x;
        @QueryParam("y")
        private String y;

        @Path("1")
        @POST
        public String m1() {
            return x + y;
        }
    }

    @Path("b9")
    public static class PrimitiveQueryParamResource {
        @QueryParam("x")
        private int x;

        @Path("1")
        @POST
        public int m1() {
            return x;
        }
    }


    @Path("c1")
    public static class StringMatrixParamResource {
        @MatrixParam("x")
        private String x;

        @Path("1")
        @POST
        public String m1() {
            return x;
        }
    }

    @Path("c2")
    public static class EncodedStringMatrixParamResource {
        @MatrixParam("x")
        @Encoded
        private String x;

        @Path("1")
        @POST
        public String m1() {
            return x;
        }
    }

    @Path("c3")
    public static class DefaultValueMatrixParamResource {
        @MatrixParam("x")
        @DefaultValue("default")
        private String x;

        @Path("1")
        @POST
        public String m1() {
            return x;
        }
    }

    @Path("c4")
    public static class ListOfStringsMatrixParamResource {
        @MatrixParam("x")
        private List<String> x;

        @Path("1")
        @POST
        public GenericEntity<List<String>> m1() {
            return new GenericEntity<List<String>>(x) {
            };
        }
    }

    @Path("c5")
    public static class SetOfStringsMatrixParamResource {
        @MatrixParam("x")
        private Set<String> x;

        @Path("1")
        @POST
        public GenericEntity<Set<String>> m1() {
            return new GenericEntity<Set<String>>(x) {
            };
        }
    }

    @Path("c6")
    public static class SortedSetOfStringsMatrixParamResource {
        @MatrixParam("x")
        private SortedSet<String> x;

        @Path("1")
        @POST
        public GenericEntity<SortedSet<String>> m1() {
            return new GenericEntity<SortedSet<String>>(x) {
            };
        }
    }

    @Path("c7")
    public static class StringValueOfMatrixParamResource {
        @MatrixParam("x")
        private Integer x;

        @Path("1")
        @POST
        public Integer m1() {
            return x;
        }
    }

    @Path("c8")
    public static class MultipleMatrixParamResource {
        @MatrixParam("x")
        private String x;
        @MatrixParam("y")
        private String y;

        @Path("1")
        @POST
        public String m1() {
            return x + y;
        }
    }

    @Path("c9")
    public static class PrimitiveMatrixParamResource {
        @MatrixParam("x")
        private int x;

        @Path("1")
        @POST
        public int m1() {
            return x;
        }
    }


    @Path("d1")
    public static class CookieCookieParamResource {
        @CookieParam("x")
        private Cookie x;

        @Path("1")
        @POST
        public Cookie m1() {
            return x;
        }
    }

    @Path("d2")
    public static class StringCookieParamResource {
        @CookieParam("x")
        private String x;

        @Path("1")
        @POST
        public String m1() {
            return x;
        }
    }

    @Path("d3")
    public static class DefaultValueCookieParamResource {
        @CookieParam("x")
        @DefaultValue("default")
        private String x;

        @Path("1")
        @POST
        public String m1() {
            return x;
        }
    }

    @Path("d4")
    public static class ListOfStringsCookieParamResource {
        @CookieParam("x")
        private List<String> x;

        @Path("1")
        @POST
        public GenericEntity<List<String>> m1() {
            return new GenericEntity<List<String>>(x) {
            };
        }
    }

    @Path("d5")
    public static class SetOfStringsCookieParamResource {
        @CookieParam("x")
        private Set<String> x;

        @Path("1")
        @POST
        public GenericEntity<Set<String>> m1() {
            return new GenericEntity<Set<String>>(x) {
            };
        }
    }

    @Path("d6")
    public static class SortedSetOfStringsCookieParamResource {
        @CookieParam("x")
        private SortedSet<String> x;

        @Path("1")
        @POST
        public GenericEntity<SortedSet<String>> m1() {
            return new GenericEntity<SortedSet<String>>(x) {
            };
        }
    }

    @Path("d7")
    public static class StringValueOfCookieParamResource {
        @CookieParam("x")
        private Integer x;

        @Path("1")
        @POST
        public Integer m1() {
            return x;
        }
    }

    @Path("d8")
    public static class MultipleCookieParamResource {
        @CookieParam("x")
        private String x;
        @CookieParam("y")
        private String y;

        @Path("1")
        @POST
        public String m1() {
            return x + y;
        }
    }

    @Path("d9")
    public static class PrimitiveCookieParamResource {
        @CookieParam("x")
        private int x;

        @Path("1")
        @POST
        public int m1() {
            return x;
        }
    }


    @Path("e1")
    public static class StringHeaderParamResource {
        @HeaderParam("x")
        private String x;

        @Path("1")
        @POST
        public String m1() {
            return x;
        }
    }

    @Path("e2")
    public static class DefaultValueHeaderParamResource {
        @HeaderParam("x")
        @DefaultValue("default")
        private String x;

        @Path("1")
        @POST
        public String m1() {
            return x;
        }
    }

    @Path("e3")
    public static class ListOfStringsHeaderParamResource {
        @HeaderParam("x")
        private List<String> x;

        @Path("1")
        @POST
        public GenericEntity<List<String>> m1() {
            return new GenericEntity<List<String>>(x) {
            };
        }
    }

    @Path("e4")
    public static class SetOfStringsHeaderParamResource {
        @HeaderParam("x")
        private Set<String> x;

        @Path("1")
        @POST
        public GenericEntity<Set<String>> m1() {
            return new GenericEntity<Set<String>>(x) {
            };
        }
    }

    @Path("e5")
    public static class SortedSetOfStringsHeaderParamResource {
        @HeaderParam("x")
        private SortedSet<String> x;

        @Path("1")
        @POST
        public GenericEntity<SortedSet<String>> m1() {
            return new GenericEntity<SortedSet<String>>(x) {
            };
        }
    }

    @Path("e6")
    public static class StringValueOfHeaderParamResource {
        @HeaderParam("x")
        private Integer x;

        @Path("1")
        @POST
        public Integer m1() {
            return x;
        }
    }

    @Path("e7")
    public static class MultipleHeaderParamResource {
        @HeaderParam("x")
        private String x;
        @HeaderParam("y")
        private String y;

        @Path("1")
        @POST
        public String m1() {
            return x + y;
        }
    }

    @Path("e8")
    public static class PrimitiveHeaderParamResource {
        @HeaderParam("x")
        private int x;

        @Path("1")
        @POST
        public int m1() {
            return x;
        }
    }


    @Path("f")
    public static class UriInfoResource {
        @Context
        private UriInfo uriInfo;

        @Path("1")
        @POST
        public String m1() {
            assertNotNull(uriInfo);
            return "UriInfo";
        }
    }

    @Path("g")
    public static class RequestResource {
        @Context
        private Request request;

        @Path("1")
        @POST
        public String m1() {
            assertNotNull(request);
            return "Request";
        }
    }

    @Path("h")
    public static class HttpHeadersResource {
        @Context
        private HttpHeaders httpHeaders;

        @Path("1")
        @POST
        public String m1() {
            assertNotNull(httpHeaders);
            return "HttpHeaders";
        }
    }

    @Path("i")
    public static class SecurityContextResource {
        @Context
        private SecurityContext securityContext;

        @Path("1")
        @POST
        public String m1() {
            assertNotNull(securityContext);
            return "SecurityContext";
        }
    }

    @Path("j")
    public static class ProvidersResource {
        @Context
        private Providers providers;

        @Path("1")
        @POST
        public String m1() {
            assertNotNull(providers);
            return "Providers";
        }
    }

    @Path("k")
    public static class ApplicationResource {
        @Context
        private Application application;

        @Path("1")
        @POST
        public String m1() {
            assertNotNull(application);
            return "Application";
        }
    }
}
