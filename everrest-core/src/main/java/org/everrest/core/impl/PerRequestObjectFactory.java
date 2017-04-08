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
import org.everrest.core.FieldInjector;
import org.everrest.core.ObjectModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Provide object's instance of component that support per-request lifecycle.
 *
 * @param <T>
 *         ObjectModel extensions
 * @author andrew00x
 * @see ObjectModel
 */
public class PerRequestObjectFactory<T extends ObjectModel> extends BaseObjectFactory<T> {
    /**
     * @param model
     *         any extension of ObjectModel
     */
    public PerRequestObjectFactory(T model) {
        super(model);
    }

    @Override
    public Object getInstance(ApplicationContext context) {
        ConstructorDescriptor constructorDescriptor = model.getConstructorDescriptors().get(0);
        Object object = constructorDescriptor.createInstance(context);

        List<FieldInjector> fieldInjectors = model.getFieldInjectors();
        if (fieldInjectors != null && fieldInjectors.size() > 0) {
            for (FieldInjector injector : fieldInjectors) {
                injector.inject(object, context);
            }
        }
        doPostConstruct(object, context);
        return object;
    }

    protected final void doPostConstruct(Object object, ApplicationContext context) {
        LifecycleComponent lifecycleComponent = new LifecycleComponent(object);
        lifecycleComponent.initialize();
        @SuppressWarnings("unchecked")
        List<LifecycleComponent> perRequest = (List<LifecycleComponent>)context.getAttributes().get("org.everrest.lifecycle.PerRequest");
        if (perRequest == null) {
            perRequest = new ArrayList<>();
            context.getAttributes().put("org.everrest.lifecycle.PerRequest", perRequest);
        }
        perRequest.add(lifecycleComponent);
    }
}
