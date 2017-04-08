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

import java.io.IOException;
import java.io.OutputStream;
import java.util.EventListener;
import java.util.EventObject;
import java.util.function.Supplier;

/**
 * Use underlying output stream as data stream. Pass all invocations to the back-end stream and notify
 * EntityStreamListener about changes in back-end stream.
 */
public class NotifierOutputStream extends OutputStream {

    /** Listen any changes in underlying stream in NotifierOutputStream, e.g. write, flush, close, */
    public interface EntityStreamListener extends EventListener {
        void onChange(EventObject event) throws IOException;
    }

    private final Supplier<OutputStream> output;
    private final EntityStreamListener writeListener;

    public NotifierOutputStream(OutputStream output, EntityStreamListener writeListener) {
        this(()-> output, writeListener);
    }

    public NotifierOutputStream(Supplier<OutputStream> output, EntityStreamListener writeListener) {
        this.output = output;
        this.writeListener = writeListener;
    }

    @Override
    public void write(int b) throws IOException {
        writeListener.onChange(new EventObject(this));
        output.get().write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        writeListener.onChange(new EventObject(this));
        output.get().write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        writeListener.onChange(new EventObject(this));
        output.get().write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        writeListener.onChange(new EventObject(this));
        output.get().flush();
    }

    @Override
    public void close() throws IOException {
        writeListener.onChange(new EventObject(this));
        output.get().close();
    }
}
