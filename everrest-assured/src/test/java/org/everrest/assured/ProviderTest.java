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

import com.jayway.restassured.response.Response;
import org.everrest.sample.book.Book;
import org.everrest.sample.book.BookService;
import org.everrest.sample.book.BookStorage;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class ProviderTest {
    @Mock private BookStorage bookStorage;
    @InjectMocks private BookService bookService;

    private BookJsonProvider bookJsonProvider;

    @Test
    public void usesCustomMessageBodyWriter() {
        when(bookStorage.getBook("123-1235-555")).thenReturn(new Book());

        final Response response = given().pathParam("id", "123-1235-555")
                                         .expect()
                                         .statusCode(200)
                                         .when()
                                         .get("/books/{id}");
        assertEquals(response.getBody().print(), "ping");
    }


    @Provider
    @Produces(MediaType.APPLICATION_JSON)
    public static class BookJsonProvider implements MessageBodyWriter<Book> {
        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type.isAssignableFrom(Book.class);
        }

        @Override
        public long getSize(Book book, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(Book book, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
            entityStream.write("ping".getBytes());
        }
    }
}
