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

import org.everrest.core.ConstructorDescriptor;
import org.everrest.core.ObjectModel;

import java.util.Optional;

/**
 * Simple implementation of {@code ObjectFactory} that creates instance of class with default constructor.
 * This implementation does not initialize any fields of created instance.
 *
 * @param <T> ObjectModel
 */
public class SimpleObjectFactory <T extends ObjectModel> extends BaseObjectFactory<T> {
    public SimpleObjectFactory(T model) {
        super(model);
    }

    @Override
    public Object getInstance(ApplicationContext context) {
        Optional<ConstructorDescriptor> constructor = model.getConstructorDescriptors().stream()
                .filter(c -> c.getParameters().isEmpty())
                .findFirst();
        if (constructor.isPresent()) {
            return constructor.get().createInstance(context);
        }
        throw new IllegalStateException(String.format("Unable create instance of class %s. Class does not have default constructor", model.getObjectClass()));
    }
}
