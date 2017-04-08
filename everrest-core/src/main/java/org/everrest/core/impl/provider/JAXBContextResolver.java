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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Provides cache for {@link JAXBContext}.
 *
 * @author andrew00x
 */
@Provider
@Produces({MediaType.APPLICATION_XML, "application/*+xml", MediaType.TEXT_XML, "text/*+xml"})
public class JAXBContextResolver implements ContextResolver<JAXBContextResolver> {
    /** JAXBContext cache. */
    private final LoadingCache<Class<?>, JAXBContext> jaxbContextsCache;

    public JAXBContextResolver() {
        jaxbContextsCache = CacheBuilder.newBuilder()
                .concurrencyLevel(16)
                .maximumSize(256)
                .expireAfterAccess(60, MINUTES)
                .build(new CacheLoader<Class<?>, JAXBContext>() {
                    @Override
                    public JAXBContext load(Class<?> aClass) throws JAXBException {
                        return JAXBContext.newInstance(aClass);
                    }
                });
    }

    @Override
    public JAXBContextResolver getContext(Class<?> type) {
        return this;
    }

    /**
     * Return JAXBContext according to supplied type. If no one context found then try create new context and save it in cache.
     *
     * @param aClass class to be bound
     * @return JAXBContext
     * @throws JAXBException if JAXBContext creation failed
     */
    public JAXBContext getJAXBContext(Class<?> aClass) throws JAXBException {
        try {
            return jaxbContextsCache.get(aClass);
        } catch (ExecutionException e) {
            throw (JAXBException) e.getCause();
        }
    }
}
