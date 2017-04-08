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

import org.everrest.sample.book.Book;
import org.everrest.sample.book.BookService;
import org.everrest.sample.book.BookStorage;
import org.hamcrest.Matchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.restassured.RestAssured.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class BookServiceTest {
    @Mock private BookStorage bookStorage;
    @InjectMocks private BookService bookService;

    @Test
    public void receivesResponseFromService() {
        Book book = new Book();
        book.setId("123-1235-555");
        when(bookStorage.getAll()).thenReturn(newArrayList(book));

        expect().body("id", Matchers.hasItem("123-1235-555"))
                .when().get("/books");

        verify(bookStorage).getAll();
    }
}
