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

import org.everrest.sample.book.BookNotFoundExceptionMapper;
import org.everrest.sample.book.BookService;
import org.everrest.sample.book.BookStorage;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class ExceptionMapperTest {
    @Mock private BookStorage bookStorage;
    @InjectMocks private BookService bookService;

    private BookNotFoundExceptionMapper notFoundMapper = new BookNotFoundExceptionMapper();

    @Test
    public void receives404ResponseWhenBookNotFound() {
        when(bookStorage.getBook("123-1235-555")).thenReturn(null);

        given().pathParam("id", "123-1235-555")
               .expect().statusCode(404)
               .when().get("/books/{id}");

        verify(bookStorage).getBook("123-1235-555");
    }

    @Test
    public void worksTwice() {
        when(bookStorage.getBook("123-1235-555")).thenReturn(null);

        given().pathParam("id", "123-1235-555")
               .expect().statusCode(404)
               .when().get("/books/{id}");

        verify(bookStorage).getBook("123-1235-555");
    }
}
