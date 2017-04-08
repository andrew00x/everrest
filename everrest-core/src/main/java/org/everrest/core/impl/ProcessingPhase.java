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

public enum ProcessingPhase {
    PRE_MATCHED(0),
    MATCHED(10),
    NOT_MATCHED(20),
    SENDING_RESPONSE(30),
    ENDED(40);

    private final int order;

    ProcessingPhase(int order) {
        this.order = order;
    }

    public int order() {
        return order;
    }
}
