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
package org.everrest.core.util;

import org.everrest.core.uri.UriPattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class UriPatternComparatorTest {
    @Parameterized.Parameters(name = "{index} => Left: {0}; Right: {1}")
    public static List<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {null,                   null,                   0},
                {null,                   mockEmpty(),            1},
                {mockEmpty(),            null,                  -1},
                {mockEmpty(),            mockEmpty(),            0},
                {mockEmpty(),            mockWithTemplate("/a"), 1},
                {mockWithTemplate("/a"), mockEmpty(),           -1},
                {mockEmpty(),            mockWithTemplate("/a"), 1},
                {mockWithTemplate("/a"), mockEmpty(),           -1},

                {mockWithTemplateNumberOfLiteralCharactersAndParameterSize("/a/b", 4, 0),   mockWithTemplateNumberOfLiteralCharactersAndParameterSize("/a", 2, 0),    -1},
                {mockWithTemplateNumberOfLiteralCharactersAndParameterSize("/a", 2, 0),     mockWithTemplateNumberOfLiteralCharactersAndParameterSize("/a/b", 4, 0),   1},
                {mockWithTemplateNumberOfLiteralCharactersAndParameterSize("/a/{b}", 3, 1), mockWithTemplateNumberOfLiteralCharactersAndParameterSize("/a", 2, 0),    -1},
                {mockWithTemplateNumberOfLiteralCharactersAndParameterSize("/a", 2, 0),     mockWithTemplateNumberOfLiteralCharactersAndParameterSize("/a/{b}", 3, 1), 1},
                {mockWithTemplateNumberOfLiteralCharactersAndParameterSize("/a", 2, 0),     mockWithTemplateNumberOfLiteralCharactersAndParameterSize("/a", 2, 0),     0},
                {mockWithTemplateNumberOfLiteralCharactersAndParameterSize("/a", 2, 0),     mockWithTemplateNumberOfLiteralCharactersAndParameterSize("/x", 2, 0),     "/a".compareTo("/x")}
        });
    }

    private static UriPattern mockEmpty() {
        return mockWithTemplate("");
    }

    private static UriPattern mockWithTemplate(String template) {
        UriPattern pattern = mock(UriPattern.class);
        when(pattern.getTemplate()).thenReturn(template);
        return pattern;
    }

    private static UriPattern mockWithTemplateNumberOfLiteralCharactersAndParameterSize(String template, int numberOfLiteralCharacters, int parameterSize) {
        UriPattern pattern = mock(UriPattern.class, RETURNS_DEEP_STUBS);
        when(pattern.getTemplate()).thenReturn(template);
        when(pattern.getNumberOfLiteralCharacters()).thenReturn(numberOfLiteralCharacters);
        when(pattern.getParameterNames().size()).thenReturn(parameterSize);
        when(pattern.getRegex()).thenReturn(template);
        return pattern;
    }

    @Parameterized.Parameter(0) public UriPattern left;
    @Parameterized.Parameter(1) public UriPattern right;
    @Parameterized.Parameter(2) public int expectedResult;

    private UriPatternComparator comparator = new UriPatternComparator();

    @Test
    public void testCompare() throws Exception {
        assertEquals(expectedResult, comparator.compare(left, right));
    }
}