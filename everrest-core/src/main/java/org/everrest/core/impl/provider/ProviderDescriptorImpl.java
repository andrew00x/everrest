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
package org.everrest.core.impl.provider;

import com.google.common.base.MoreObjects;
import org.everrest.core.BaseObjectModel;
import org.everrest.core.provider.ProviderDescriptor;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;

import static org.everrest.core.impl.header.MediaTypeHelper.createConsumesList;
import static org.everrest.core.impl.header.MediaTypeHelper.createProducesList;

/**
 * @author andrew00x
 */
public class ProviderDescriptorImpl extends BaseObjectModel implements ProviderDescriptor {
    /** List of media types which this method can consume. See {@link javax.ws.rs.Consumes} */
    private final List<MediaType> consumes;

    /** List of media types which this method can produce. See {@link javax.ws.rs.Produces} */
    private final List<MediaType> produces;

    private final Integer priority;
    private final Class<?> contract;

    public ProviderDescriptorImpl(Class<?> providerClass) {
        this(providerClass, null, null);
    }

    public ProviderDescriptorImpl(Class<?> providerClass, Class<?> contract) {
        this(providerClass, contract, null);
    }

    public ProviderDescriptorImpl(Class<?> providerClass, Class<?> contract, Integer priority) {
        super(providerClass);
        this.contract = contract;
        this.priority = priority;
        this.consumes = createConsumesList(providerClass.getAnnotation(Consumes.class));
        this.produces = createProducesList(providerClass.getAnnotation(Produces.class));
    }

    public ProviderDescriptorImpl(Object provider) {
        this(provider, null, null);
    }

    public ProviderDescriptorImpl(Object provider, Class<?> contract) {
        this(provider, contract, null);
    }

    public ProviderDescriptorImpl(Object provider, Class<?> contract, Integer priority) {
        super(provider);
        final Class<?> providerClass = provider.getClass();
        this.contract = contract;
        this.priority = priority;
        this.consumes = createConsumesList(providerClass.getAnnotation(Consumes.class));
        this.produces = createProducesList(providerClass.getAnnotation(Produces.class));
    }

    @Override
    public List<MediaType> consumes() {
        return consumes;
    }

    @Override
    public List<MediaType> produces() {
        return produces;
    }

    @Override
    public Optional<Integer> getPriority() {
        return Optional.ofNullable(priority);
    }

    @Override
    public Class<?> getContract() {
        return contract;
    }

    public String toString() {
        return MoreObjects.toStringHelper(ProviderDescriptorImpl.class)
                          .add("provider class", clazz)
                          .add("contract", contract)
                          .add("priority", priority)
                          .add("produces media type", produces.isEmpty() ? null : produces)
                          .add("consumes media type", consumes.isEmpty() ? null : consumes)
                          .addValue(constructors.isEmpty() ? null : constructors)
                          .addValue(fieldInjectors.isEmpty() ? null : fieldInjectors)
                          .omitNullValues()
                          .toString();
    }
}
