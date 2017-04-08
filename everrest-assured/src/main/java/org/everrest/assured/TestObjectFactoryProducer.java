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
package org.everrest.assured;

import org.everrest.core.ObjectFactory;
import org.everrest.core.ObjectFactoryProducer;
import org.everrest.core.ObjectModel;
import org.everrest.core.impl.DefaultObjectFactoryProducer;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class TestObjectFactoryProducer implements ObjectFactoryProducer {
    private final Map<Class<?>, TestedComponentEntry> restComponentsEntries = new HashMap<>();
    private final ObjectFactoryProducer delegateProducer = new DefaultObjectFactoryProducer();

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ObjectModel> ObjectFactory<T> create(T model) {
        TestedComponentEntry testedComponentEntry = restComponentsEntries.get(model.getObjectClass());
        if (testedComponentEntry == null) {
            return delegateProducer.create(model);
        }
        return new TestResourceFactory<>(model, testedComponentEntry.test, testedComponentEntry.testedComponentField);
    }

    @Override
    public <T extends ObjectModel> ObjectFactory<T> create(T model, Object instance) {
        return delegateProducer.create(model, instance);
    }

    void registerRestComponent(Object test, Field restComponentField) {
        restComponentsEntries.put(restComponentField.getType(), new TestedComponentEntry(test, restComponentField));
    }

    private static class TestedComponentEntry {
        final Object test;
        final Field testedComponentField;

        TestedComponentEntry(Object test, Field testedComponentField) {
            this.test = test;
            this.testedComponentField = testedComponentField;
        }
    }
}
