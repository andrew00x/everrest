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
package org.everrest.core.impl;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServerConfigurationPropertiesTest {
    private ServerConfigurationProperties serverConfiguration;

    @Before
    public void setUp() throws Exception {
        serverConfiguration = new ServerConfigurationProperties();
    }

    @Test
    public void testDefaultEverrestConfiguration() {
        assertTrue(serverConfiguration.isHttpMethodOverrideEnabled());
        assertTrue(serverConfiguration.isAsynchronousEnabled());
        assertEquals("/async", serverConfiguration.getAsynchronousServicePath());
        assertEquals(10, serverConfiguration.getAsynchronousPoolSize());
        assertTrue(serverConfiguration.isCheckSecurityEnabled());
        assertFalse(serverConfiguration.isUriNormalizationEnabled());
        assertEquals(100, serverConfiguration.getAsynchronousQueueSize());
        assertEquals(512, serverConfiguration.getAsynchronousCacheSize());
        assertEquals(60, serverConfiguration.getAsynchronousJobTimeout());
        assertEquals(204800, serverConfiguration.getMaxBufferSize());
    }

    @Test
    public void copiesAllPropertiesFromOtherEverrestConfiguration() {
        serverConfiguration.setProperty("foo", "bar");
        serverConfiguration.setProperty("foo2", "bar2");
        ServerConfigurationProperties newServerConfiguration = new ServerConfigurationProperties(serverConfiguration);

        assertEquals(serverConfiguration.getProperties(), newServerConfiguration.getProperties());
    }

    @Test
    public void setsCustomPropertiesForConfiguration() {
        serverConfiguration.setCheckSecurity(false);
        serverConfiguration.setHttpMethodOverride(false);
        serverConfiguration.setNormalizeUri(true);
        serverConfiguration.setAsynchronousSupported(false);
        serverConfiguration.setAsynchronousServicePath("/async2");
        serverConfiguration.setAsynchronousPoolSize(20);
        serverConfiguration.setAsynchronousQueueSize(256);
        serverConfiguration.setAsynchronousCacheSize(100);
        serverConfiguration.setAsynchronousJobTimeout(10);
        serverConfiguration.setMaxBufferSize(2048);

        assertFalse(serverConfiguration.isHttpMethodOverrideEnabled());
        assertFalse(serverConfiguration.isAsynchronousEnabled());
        assertEquals("/async2", serverConfiguration.getAsynchronousServicePath());
        assertEquals(20, serverConfiguration.getAsynchronousPoolSize());
        assertFalse(serverConfiguration.isCheckSecurityEnabled());
        assertTrue(serverConfiguration.isUriNormalizationEnabled());
        assertEquals(256, serverConfiguration.getAsynchronousQueueSize());
        assertEquals(100, serverConfiguration.getAsynchronousCacheSize());
        assertEquals(10, serverConfiguration.getAsynchronousJobTimeout());
        assertEquals(2048, serverConfiguration.getMaxBufferSize());
    }
}