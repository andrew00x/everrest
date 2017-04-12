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
package org.everrest.guice;

import com.google.inject.Provider;
import org.everrest.core.ObjectFactory;
import org.everrest.core.ObjectModel;
import org.everrest.core.impl.ApplicationContext;

/**
 * @author andrew00x
 */
public class GuiceObjectFactory<T extends ObjectModel> implements ObjectFactory<T> {
    protected final T model;

    protected final Provider<?> provider;

    public GuiceObjectFactory(T model, Provider<?> provider) {
        this.model = model;
        this.provider = provider;
    }

    @Override
    public Object getInstance(ApplicationContext context) {
        return provider.get();
    }

    @Override
    public T getObjectModel() {
        return model;
    }
}
