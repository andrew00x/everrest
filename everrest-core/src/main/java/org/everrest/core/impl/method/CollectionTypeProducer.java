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
package org.everrest.core.impl.method;

import org.everrest.core.method.TypeProducer;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ParamConverter;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

/**
 * Creates collection of objects from collection of strings.
 *
 * @param <C> type of {@code Collection} this class produces
 * @param <E> type of elements in {@code Collection}
 */
public class CollectionTypeProducer<C extends Collection<E>, E> implements TypeProducer<C> {
    private final Supplier<C> collectionFactory;
    private final ParamConverter<E> paramConverter;

    /**
     * @param collectionFactory {@code Supplier} which creates empty {@code Collection} of appropriate type
     * @param paramConverter    {@code ParamConverter} which able to convert single {@code String} to appropriate type
     */
    CollectionTypeProducer(Supplier<C> collectionFactory, ParamConverter<E> paramConverter) {
        this.collectionFactory = collectionFactory;
        this.paramConverter = paramConverter;
    }

    @Override
    public C createValue(String param, MultivaluedMap<String, String> values, String defaultValue) throws Exception {
        C result = null;
        List<String> list = values.get(param);
        if (list != null) {
            result = collectionFactory.get();
            result.addAll(list.stream().map(paramConverter::fromString).collect(toList()));
        } else if (defaultValue != null) {
            result = collectionFactory.get();
            result.add(paramConverter.fromString(defaultValue));
        }
        return result;
    }
}
