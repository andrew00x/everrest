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
package org.everrest.core.method;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Creates object from String.
 *
 * @author andrew00x
 */
public interface TypeProducer<T> {
    /**
     * Creates object from single or multiple String values from specified map.
     *
     * @param param
     *         parameter name, parameter name is retrieved from parameter annotation
     * @param values
     *         all value which can be used for construct object, it can be header parameters, path parameters, query parameters, etc
     * @param defaultValue
     *         default value which can be used if value can't be found in map
     * @return newly created object
     * @throws Exception
     *         if any errors occurs
     */
    T createValue(String param, MultivaluedMap<String, String> values, String defaultValue) throws Exception;
}
