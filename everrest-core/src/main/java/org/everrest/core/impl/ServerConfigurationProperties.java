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

import org.everrest.core.SimpleConfigurationProperties;

import java.util.Map;

/**
 * @author andrew00x
 */
public class ServerConfigurationProperties extends SimpleConfigurationProperties {
    public static final String EVERREST_HTTP_METHOD_OVERRIDE      = "org.everrest.http.method.override";
    public static final String EVERREST_NORMALIZE_URI             = "org.everrest.normalize.uri";
    public static final String EVERREST_CHECK_SECURITY            = "org.everrest.security";
    public static final String EVERREST_ASYNCHRONOUS              = "org.everrest.asynchronous";
    public static final String EVERREST_ASYNCHRONOUS_SERVICE_PATH = "org.everrest.asynchronous.service.path";
    public static final String EVERREST_ASYNCHRONOUS_POOL_SIZE    = "org.everrest.asynchronous.pool.size";
    public static final String EVERREST_ASYNCHRONOUS_QUEUE_SIZE   = "org.everrest.asynchronous.queue.size";
    public static final String EVERREST_ASYNCHRONOUS_CACHE_SIZE   = "org.everrest.asynchronous.cache.size";
    public static final String EVERREST_ASYNCHRONOUS_JOB_TIMEOUT  = "org.everrest.asynchronous.job.timeout";
    public static final String METHOD_INVOKER_DECORATOR_FACTORY   = "org.everrest.core.impl.method.MethodInvokerDecoratorFactory";
    /**
     * Max buffer size configuration parameter. Entities that has size greater then specified will be stored in temporary directory on file
     * system during entity processing.
     */
    public static final String EVERREST_MAX_BUFFER_SIZE           = "org.everrest.max.buffer.size";


    public static final boolean DEFAULT_CHECK_SECURITY         = true;
    public static final boolean DEFAULT_HTTP_METHOD_OVERRIDE   = true;
    public static final boolean DEFAULT_NORMALIZE_URI          = false;
    public static final boolean defaultAsynchronousSupported   = true;
    public static final int     defaultAsynchronousPoolSize    = 10;
    public static final String  defaultAsynchronousServicePath = "/async";
    public static final int     defaultAsynchronousQueueSize   = 100;
    public static final int     defaultAsynchronousCacheSize   = 512;
    public static final int     defaultAsynchronousJobTimeout  = 60;
    public static final int     DEFAULT_MAX_BUFFER_SIZE = 204800;

    public ServerConfigurationProperties() {
    }

    public ServerConfigurationProperties(Map<String, Object> properties) {
        super(properties);
    }

    public ServerConfigurationProperties(ServerConfigurationProperties other) {
        super(other);
    }

    public boolean isCheckSecurityEnabled() {
        return getBooleanProperty(EVERREST_CHECK_SECURITY, DEFAULT_CHECK_SECURITY);
    }

    public void setCheckSecurity(boolean checkSecurity) {
        setProperty(EVERREST_CHECK_SECURITY, Boolean.toString(checkSecurity));
    }

    public boolean isHttpMethodOverrideEnabled() {
        return getBooleanProperty(EVERREST_HTTP_METHOD_OVERRIDE, DEFAULT_HTTP_METHOD_OVERRIDE);
    }

    public void setHttpMethodOverride(boolean httpMethodOverride) {
        setProperty(EVERREST_HTTP_METHOD_OVERRIDE, Boolean.toString(httpMethodOverride));
    }

    public boolean isUriNormalizationEnabled() {
        return getBooleanProperty(EVERREST_NORMALIZE_URI, DEFAULT_NORMALIZE_URI);
    }

    public void setNormalizeUri(boolean normalizeUri) {
        setProperty(EVERREST_NORMALIZE_URI, Boolean.toString(normalizeUri));
    }

    public boolean isAsynchronousEnabled() {
        return getBooleanProperty(EVERREST_ASYNCHRONOUS, defaultAsynchronousSupported);
    }

    public void setAsynchronousSupported(boolean asynchronousSupported) {
        setProperty(EVERREST_ASYNCHRONOUS, Boolean.toString(asynchronousSupported));
    }

    public String getAsynchronousServicePath() {
        return getStringProperty(EVERREST_ASYNCHRONOUS_SERVICE_PATH, defaultAsynchronousServicePath);
    }

    public void setAsynchronousServicePath(String servicePath) {
        setProperty(EVERREST_ASYNCHRONOUS_SERVICE_PATH, servicePath);
    }

    public int getAsynchronousPoolSize() {
        return getIntegerProperty(EVERREST_ASYNCHRONOUS_POOL_SIZE, defaultAsynchronousPoolSize);
    }

    public void setAsynchronousPoolSize(int asynchronousPoolSize) {
        setProperty(EVERREST_ASYNCHRONOUS_POOL_SIZE, Integer.toString(asynchronousPoolSize));
    }

    public int getAsynchronousQueueSize() {
        return getIntegerProperty(EVERREST_ASYNCHRONOUS_QUEUE_SIZE, defaultAsynchronousQueueSize);
    }

    public void setAsynchronousQueueSize(int asynchronousQueueSize) {
        setProperty(EVERREST_ASYNCHRONOUS_QUEUE_SIZE, Integer.toString(asynchronousQueueSize));
    }

    public int getAsynchronousCacheSize() {
        return getIntegerProperty(EVERREST_ASYNCHRONOUS_CACHE_SIZE, defaultAsynchronousCacheSize);
    }

    public void setAsynchronousCacheSize(int asynchronousCacheSize) {
        setProperty(EVERREST_ASYNCHRONOUS_CACHE_SIZE, Integer.toString(asynchronousCacheSize));
    }

    public int getAsynchronousJobTimeout() {
        return getIntegerProperty(EVERREST_ASYNCHRONOUS_JOB_TIMEOUT, defaultAsynchronousJobTimeout);
    }

    public void setAsynchronousJobTimeout(int asynchronousJobTimeout) {
        setProperty(EVERREST_ASYNCHRONOUS_JOB_TIMEOUT, Integer.toString(asynchronousJobTimeout));
    }

    public int getMaxBufferSize() {
        return getIntegerProperty(EVERREST_MAX_BUFFER_SIZE, DEFAULT_MAX_BUFFER_SIZE);
    }

    public void setMaxBufferSize(int maxBufferSize) {
        setProperty(EVERREST_MAX_BUFFER_SIZE, Integer.toString(maxBufferSize));
    }
}
