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

import com.google.common.collect.ImmutableMap;
import org.everrest.core.ConfigurationProperties;
import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.provider.StringEntityProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DefaultFeatureContextTest {
    @Mock private ProviderBinder providers;
    @Mock private ConfigurationProperties properties;
    @InjectMocks private DefaultFeatureContext featureContext;

    @Test
    public void setsProperty() {
        featureContext.property("name", "value");
        verify(properties).setProperty("name", "value");
    }

    @Test
    public void registersComponentClass() {
        featureContext.register(StringEntityProvider.class);
        verify(providers).register(StringEntityProvider.class);
    }

    @Test
    public void registersComponentClassWithPriority() {
        featureContext.register(StringEntityProvider.class, 100);
        verify(providers).register(StringEntityProvider.class, 100);
    }

    @Test
    public void registersComponentClassWithContracts() {
        featureContext.register(StringEntityProvider.class, MessageBodyReader.class, MessageBodyWriter.class);
        verify(providers).register(StringEntityProvider.class, MessageBodyReader.class, MessageBodyWriter.class);
    }

    @Test
    public void registersComponentClassWithContractAndPriorities() {
        featureContext.register(StringEntityProvider.class, ImmutableMap.of(MessageBodyWriter.class, 101));
        verify(providers).register(StringEntityProvider.class, ImmutableMap.of(MessageBodyWriter.class, 101));
    }

    @Test
    public void registersComponent() {
        Object component = new StringEntityProvider();
        featureContext.register(component);
        verify(providers).register(component);
    }

    @Test
    public void registersComponentWithPriority() {
        Object component = new StringEntityProvider();
        featureContext.register(component, 100);
        verify(providers).register(component, 100);
    }

    @Test
    public void registersComponentWithContracts() {
        Object component = new StringEntityProvider();
        featureContext.register(component, MessageBodyReader.class, MessageBodyWriter.class);
        verify(providers).register(component, MessageBodyReader.class, MessageBodyWriter.class);
    }

    @Test
    public void registersComponentWithContractAndPriorities() {
        Object component = new StringEntityProvider();
        featureContext.register(component, ImmutableMap.of(MessageBodyWriter.class, 101));
        verify(providers).register(component, ImmutableMap.of(MessageBodyWriter.class, 101));
    }
}