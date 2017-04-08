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

import com.google.inject.Injector;
import com.google.inject.Provider;
import org.everrest.core.ObjectFactory;
import org.everrest.core.ObjectFactoryProducer;
import org.everrest.core.ObjectModel;

public class GuiceObjectFactoryProducer implements ObjectFactoryProducer {
    private final Injector injector;

    public GuiceObjectFactoryProducer(Injector injector) {
        this.injector = injector;
    }

    @Override
    public <M extends ObjectModel> ObjectFactory<M> create(M model) {
        Provider<?> provider = injector.getProvider(model.getObjectClass());
        return new GuiceObjectFactory<>(model, provider);
    }
}
