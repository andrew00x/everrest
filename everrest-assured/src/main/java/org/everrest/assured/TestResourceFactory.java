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

import com.google.common.base.Throwables;
import org.everrest.core.ObjectFactory;
import org.everrest.core.ObjectModel;
import org.everrest.core.impl.ApplicationContext;
import org.everrest.core.impl.PerRequestObjectFactory;
import org.everrest.core.impl.SingletonObjectFactory;
import org.everrest.core.impl.provider.ProviderDescriptorImpl;
import org.everrest.core.impl.resource.AbstractResourceDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import java.lang.reflect.Field;

/**
 * Get instance of the REST resource from test class in request time.
 */
public class TestResourceFactory<T extends ObjectModel> implements ObjectFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TestResourceFactory.class);

    private final Object test;
    private final Field restComponentField;
    private final T model;

    public TestResourceFactory(T model, Object test, Field restComponentField) {
        this.model = model;
        this.test = test;
        this.restComponentField = restComponentField;
        this.restComponentField.setAccessible(true);
    }

    @Override
    public Object getInstance(ApplicationContext context) {
        try {
            Object object = restComponentField.get(test);
            ObjectModel descriptor;
            if (restComponentField.getType().isAnnotationPresent(Path.class)) {
                descriptor = new AbstractResourceDescriptor(restComponentField.getType());
            } else {
                descriptor = new ProviderDescriptorImpl(restComponentField.getType());
            }
            if (object != null) {
                model.getFieldInjectors().stream()
                        .filter(injector -> injector.getAnnotation() != null)
                        .forEach(injector -> injector.inject(object, context));
                return new SingletonObjectFactory<>(descriptor, object).getInstance(context);
            } else {
                return new PerRequestObjectFactory<>(descriptor).getInstance(context);
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw Throwables.propagate(e);
        }
    }

    @Override
    public T getObjectModel() {
        return model;
    }
}
