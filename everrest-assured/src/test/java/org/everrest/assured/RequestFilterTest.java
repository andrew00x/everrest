package org.everrest.assured;

import org.everrest.sample.book.Book;
import org.everrest.sample.book.BookService;
import org.everrest.sample.book.BookStorage;
import org.hamcrest.Matchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.restassured.RestAssured.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class RequestFilterTest {

    @PreMatching
    @Provider
    public static class RequestFilter1 implements ContainerRequestFilter {
        @Context private UriInfo uriInfo;
        @Context private HttpHeaders httpHeaders;
        private Providers providers;
        private HttpServletRequest httpRequest;

        public RequestFilter1(@Context Providers providers, @Context HttpServletRequest httpRequest) {
            this.providers = providers;
            this.httpRequest = httpRequest;
        }

        @Override
        public void filter(ContainerRequestContext request) {
            if (uriInfo != null && httpHeaders != null && providers != null && httpRequest != null) {
                request.setMethod("GET");
            }
        }
    }

    RequestFilter1 requestFilter1;

    @Mock private BookStorage bookStorage;
    @InjectMocks private BookService bookService;

    @Test
    public void changesHttpMethod() {
        Book book = new Book();
        book.setId("123-1235-555");
        when(bookStorage.getAll()).thenReturn(newArrayList(book));

        expect().body("id", Matchers.hasItem("123-1235-555"))
                .when().post("/books");

        verify(bookStorage).getAll();
    }

    @Test
    public void worksTwice() {
        Book book = new Book();
        book.setId("123-1235-555");
        when(bookStorage.getAll()).thenReturn(newArrayList(book));

        expect().body("id", Matchers.hasItem("123-1235-555"))
                .when().post("/books");

        verify(bookStorage).getAll();
    }
}
