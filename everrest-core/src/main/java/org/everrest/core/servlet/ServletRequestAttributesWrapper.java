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
package org.everrest.core.servlet;

import org.everrest.core.ConfigurationProperties;

import javax.servlet.ServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

class ServletRequestAttributesWrapper extends ConfigurationProperties {
    private final ServletRequest request;

    ServletRequestAttributesWrapper(ServletRequest request) {
        this.request = request;
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> allAttributes = new HashMap<>();
        Enumeration<String> attributeNames = request.getAttributeNames();
        while(attributeNames.hasMoreElements()) {
            String name = attributeNames.nextElement();
            allAttributes.put(name, request.getAttribute(name));
        }
        return allAttributes;
    }

    @Override
    public Object getProperty(String name) {
        return request.getAttribute(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return Collections.list(request.getAttributeNames());
    }

    @Override
    public void setProperty(String name, Object value) {
        request.setAttribute(name, value);
    }

    @Override
    public void removeProperty(String name) {
        request.removeAttribute(name);
    }
}
