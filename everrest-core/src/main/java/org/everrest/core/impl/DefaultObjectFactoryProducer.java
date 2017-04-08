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
import org.everrest.core.ObjectFactoryProducer;
import org.everrest.core.ObjectModel;

import javax.ws.rs.core.Feature;

public class DefaultObjectFactoryProducer implements ObjectFactoryProducer {
    @Override
    public <M extends ObjectModel> ObjectFactory<M> create(M model) {
        if (Feature.class.isAssignableFrom(model.getObjectClass())) {
            return new SimpleObjectFactory<>(model);
        }
        return new PerRequestObjectFactory<>(model);
    }
}
