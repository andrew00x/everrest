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

import org.everrest.core.ObjectFactory;
import org.everrest.core.ObjectModel;

import java.util.Objects;

public abstract class BaseObjectFactory<T extends ObjectModel> implements ObjectFactory<T> {

    /**
     * Object model that at least gives possibility to create object instance. Should provide full set of available
     * constructors and object fields.
     *
     * @see ObjectModel
     */
    protected final T model;

    protected BaseObjectFactory(T model) {
        this.model = model;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseObjectFactory)) {
            return false;
        }
        BaseObjectFactory other = (BaseObjectFactory) o;
        return Objects.equals(getObjectModel(), other.getObjectModel());
    }

    @Override
    public int hashCode() {
        int hashcode = 8;
        hashcode = 31 * hashcode + Objects.hash(model);
        return hashcode;
    }

    @Override
    public T getObjectModel() {
        return model;
    }
}
