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
package org.everrest.core.async;

import org.everrest.core.resource.ResourceMethodDescriptor;

import java.util.List;

public interface AsynchronousJobPool {
    String getAsynchronousServicePath();

    int getMaxCacheSize();

    int getMaxQueueSize();

    int getThreadPoolSize();

    int getJobTimeout();

    /**
     * @param resource       object that contains resource method
     * @param resourceMethod resource or sub-resource method to invoke
     * @param params         method parameters
     * @return asynchronous job
     * @throws AsynchronousJobRejectedException if this task cannot be added to pool
     */
    AsynchronousJob addJob(Object resource, ResourceMethodDescriptor resourceMethod, Object[] params) throws AsynchronousJobRejectedException;

    AsynchronousJob getJob(Long jobId);

    AsynchronousJob removeJob(Long jobId);

    List<AsynchronousJob> getAll();

    /**
     * Registers new listener if it is not registered yet.
     *
     * @param listener listener
     * @return {@code true} if new listener registered and {@code false} otherwise.
     * @see AsynchronousJobListener
     */
    boolean registerListener(AsynchronousJobListener listener);

    /**
     * Unregisters listener.
     *
     * @param listener listener to unregister
     * @return {@code true} if listener unregistered and {@code false} otherwise.
     * @see AsynchronousJobListener
     */
    boolean unregisterListener(AsynchronousJobListener listener);

    void stop();
}
