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
package org.everrest.core.impl.provider;

import com.google.common.base.Throwables;
import org.everrest.core.impl.FileCollector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class IOHelper {
    private IOHelper() {
    }

    /**
     * Buffer input stream in memory of in file. If size of stream is less then {@code maxMemSize} all data stored
     * in memory otherwise stored in file.
     *
     * @param in
     *         source stream
     * @param maxMemSize
     *         max size of data to keep in memory
     * @return stream buffered in memory or in file
     * @throws IOException
     *         if any i/o error occurs
     */
    public static InputStream bufferStream(InputStream in, int maxMemSize) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesNum;
        boolean overflow = false;
        while ((!overflow) && (bytesNum = in.read(buffer)) != -1) {
            bos.write(buffer, 0, bytesNum);
            overflow = bos.size() > maxMemSize;
        }

        if (overflow) {
            File file = FileCollector.getInstance().createFile();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                bos.writeTo(fos);
                while ((bytesNum = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesNum);
                }
            }
            return new DeleteOnCloseFileInputStream(file);
        }
        return new ByteArrayInputStream(bos.toByteArray());
    }

    public static boolean isEmpty(InputStream in) {
        if (in != null) {
            try {
                if (in.markSupported()) {
                    in.mark(1);
                    boolean empty = in.read() == -1;
                    in.reset();
                    return empty;
                }
                return in.available() == 0;
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
        return true;
    }

    private static final class DeleteOnCloseFileInputStream extends FileInputStream {
        private final File file;

        DeleteOnCloseFileInputStream(File file) throws FileNotFoundException {
            super(file);
            this.file = file;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (file.exists()) {
                    file.delete();
                }
            }
        }
    }
}
