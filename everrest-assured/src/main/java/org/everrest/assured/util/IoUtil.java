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
package org.everrest.assured.util;

import com.google.common.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import static com.google.common.io.Closeables.closeQuietly;

/** Utility class for io operations. */
public class IoUtil {

    private static final Logger LOG = LoggerFactory.getLogger(IoUtil.class);

    public static String getResource(String resourceName) {
        Reader reader = null;
        try {
            InputStream stream;
            File file = new File(resourceName);
            if (file.isFile() && file.exists()) {
                stream = new FileInputStream(file);
            } else {
                stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
            }
            reader = new BufferedReader(new InputStreamReader(stream));
            return CharStreams.toString(reader);
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        } finally {
            closeQuietly(reader);
        }
        return "";
    }
}
