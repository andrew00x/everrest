package org.everrest.assured;

import org.everrest.sample.book.Book;
import org.everrest.sample.book.BookService;
import org.everrest.sample.book.BookStorage;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import java.lang.annotation.Annotation;

import static com.jayway.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class ResponseFilterTest {

    @Provider
    public static class ResponseFilter1 implements ContainerResponseFilter {
        @Context private UriInfo uriInfo;
        @Context private HttpHeaders httpHeaders;
        private Providers providers;
        private HttpServletRequest httpRequest;

        public ResponseFilter1(@Context Providers providers, @Context HttpServletRequest httpRequest) {
            this.providers = providers;
            this.httpRequest = httpRequest;
        }

        @Override
        public void filter(ContainerRequestContext request, ContainerResponseContext response) {
            if (uriInfo != null && httpHeaders != null && providers != null && httpRequest != null) {
                response.setStatus(200);
                response.setEntity("to be or not to be", new Annotation[0], TEXT_PLAIN_TYPE);
            }
        }
    }

    ResponseFilter1 responseFilter1;

    @Mock private BookStorage bookStorage;
    @InjectMocks private BookService bookService;

    @Test
    public void changesResponse() {
        Book book = new Book();
        book.setId("123-1235-555");
        when(bookStorage.getBook("123-1235-555")).thenReturn(book);

        given().pathParam("id", "123-1235-555")
               .when()
               .get("/books/{id}")
               .then()
               .statusCode(200).contentType("text/plain").body(equalTo("to be or not to be"));

        verify(bookStorage).getBook("123-1235-555");
    }

    @Test
    public void worksTwice() {
        Book book = new Book();
        book.setId("123-1235-555");
        when(bookStorage.getBook("123-1235-555")).thenReturn(book);

        given().pathParam("id", "123-1235-555")
               .when()
               .get("/books/{id}")
               .then()
               .statusCode(200).contentType("text/plain").body(equalTo("to be or not to be"));

        verify(bookStorage).getBook("123-1235-555");
    }
}
