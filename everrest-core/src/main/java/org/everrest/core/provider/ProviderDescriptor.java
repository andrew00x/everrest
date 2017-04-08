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
package org.everrest.core.provider;

import org.everrest.core.ObjectModel;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;

/**
 * Descriptor of Provider. Provider is annotated with &#64;Provider and implement interface defined by JAX-RS.
 *
 * @author andrew00x
 */
public interface ProviderDescriptor extends ObjectModel {

    /**
     * Get list of {@link MediaType} which current provider consumes.
     *
     * @return list of media types
     */
    List<MediaType> consumes();

    /**
     * Get list of {@link MediaType} which current provider produces.
     *
     * @return list of media types
     */
    List<MediaType> produces();

    /**
     * @return priority of component. Priority maybe specified on the registered
     * JAX-RS components via {@code javax.annotation.Priority} annotation or directly
     * with corresponding methods in {@code javax.ws.rs.core.Configurable}.
     * If priority is not applicable to provider contract this method returns empty {@code Optional},
     * however if priority is absent for component, for which a priority should be applied this method
     * returns {@code Optional} with value {@link javax.ws.rs.Priorities#USER}
     */
    Optional<Integer> getPriority();

    Class<?> getContract();
}
