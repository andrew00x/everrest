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
package org.everrest.core.impl.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

// Main purpose of existing this class is ability to write unit tests.
// Don't want to open real connection to resources in tests.
// In tests this class will be mocked.
class HttpURLConnectionFactory {
    HttpURLConnection openConnectionTo(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }
}
