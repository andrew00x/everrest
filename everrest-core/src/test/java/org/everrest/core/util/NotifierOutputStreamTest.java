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

import org.everrest.core.util.NotifierOutputStream.EntityStreamListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.OutputStream;
import java.util.EventObject;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class NotifierOutputStreamTest {
    @Captor private ArgumentCaptor<EventObject> eventCaptor;
    @Mock private OutputStream output;
    @Mock private EntityStreamListener writeListener;
    @InjectMocks private NotifierOutputStream notifierOutput;

    @Test
    public void writesSingleByteAndNotifiesListener() throws Exception {
        byte b = 7;
        notifierOutput.write(b);
        verify(output).write(b);
        verify(writeListener).onChange(eventCaptor.capture());
        assertSame(notifierOutput, eventCaptor.getValue().getSource());
    }

    @Test
    public void writeBytesAndNotifiesListener() throws Exception {
        byte[] b = {3, 7, 9};
        notifierOutput.write(b);
        verify(output).write(b);
        verify(writeListener).onChange(eventCaptor.capture());
        assertSame(notifierOutput, eventCaptor.getValue().getSource());
    }

    @Test
    public void writeBytesWithOffsetAndNotifiesListener() throws Exception {
        byte[] b = {3, 7, 9};
        notifierOutput.write(b, 1, 1);
        verify(output).write(b, 1, 1);
        verify(writeListener).onChange(eventCaptor.capture());
        assertSame(notifierOutput, eventCaptor.getValue().getSource());
    }

    @Test
    public void flushAndNotifiesListener() throws Exception {
        notifierOutput.flush();
        verify(output).flush();
        verify(writeListener).onChange(eventCaptor.capture());
        assertSame(notifierOutput, eventCaptor.getValue().getSource());
    }

    @Test
    public void closesAndNotifiesListener() throws Exception {
        notifierOutput.close();
        verify(output).close();
        verify(writeListener).onChange(eventCaptor.capture());
        assertSame(notifierOutput, eventCaptor.getValue().getSource());
    }
}