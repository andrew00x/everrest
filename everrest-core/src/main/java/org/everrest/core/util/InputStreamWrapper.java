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
import java.io.InputStream;
import java.util.function.Supplier;

public class InputStreamWrapper extends InputStream {
    private final Supplier<InputStream> input;

    public InputStreamWrapper(Supplier<InputStream> input) {
        this.input = input;
    }

    @Override
    public int read() throws IOException {
        return input.get().read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return input.get().read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return input.get().read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return input.get().skip(n);
    }

    @Override
    public int available() throws IOException {
        return input.get().available();
    }

    @Override
    public void close() throws IOException {
        input.get().close();
    }

    @Override
    public synchronized void mark(int limit) {
        input.get().mark(limit);
    }

    @Override
    public synchronized void reset() throws IOException {
        input.get().reset();
    }

    @Override
    public boolean markSupported() {
        return input.get().markSupported();
    }
}
