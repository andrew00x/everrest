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
import org.everrest.core.impl.provider.ByteEntityProvider;
import org.everrest.core.impl.provider.StringEntityProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static javax.ws.rs.RuntimeType.SERVER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EverrestConfigurationTest {
    @Mock private ProviderBinder providers;
    @Mock private ConfigurationProperties configurationProperties;
    @InjectMocks private EverrestConfiguration configuration;

    @Test
    public void getsRuntimeType() {
        when(providers.getRuntimeType()).thenReturn(SERVER);
        assertEquals(SERVER, configuration.getRuntimeType());
    }

    @Test
    public void getsProperties() {
        Map<String, Object> properties = ImmutableMap.of("name", "value");
        when(configurationProperties.getProperties()).thenReturn(properties);
        assertEquals(properties, configuration.getProperties());
    }

    @Test
    public void getsProperty() {
        when(configurationProperties.getProperty("name")).thenReturn("value");
        assertEquals("value", configuration.getProperty("name"));
    }

    @Test
    public void getsPropertyNames() {
        Collection<String> names = newArrayList("name1", "name2");
        when(configurationProperties.getPropertyNames()).thenReturn(names);
        assertEquals(names, configuration.getPropertyNames());
    }

    @Test
    public void checkIsFeatureEnabled() {
        Feature feature = mock(Feature.class);
        when(providers.isEnabled(feature)).thenReturn(true);
        assertTrue(configuration.isEnabled(feature));
    }

    @Test
    public void checkIsFeatureClassEnabled() {
        when(providers.isEnabled(SomeFeature.class)).thenReturn(true);
        assertTrue(configuration.isEnabled(SomeFeature.class));
    }

    @Test
    public void checkIsComponentRegistered() {
        Object component = new StringEntityProvider();
        when(providers.isRegistered(component)).thenReturn(true);
        assertTrue(configuration.isRegistered(component));
    }

    @Test
    public void checkIsComponentClassRegistered() {
        when(providers.isRegistered(StringEntityProvider.class)).thenReturn(true);
        assertTrue(configuration.isRegistered(StringEntityProvider.class));
    }

    @Test
    public void getsContractsOfComponentClass() {
        Map<Class<?>, Integer> contracts = ImmutableMap.of(MessageBodyReader.class, 1,
                                                           MessageBodyWriter.class, 2);
        when(providers.getContracts(StringEntityProvider.class)).thenReturn(contracts);
        assertEquals(contracts, configuration.getContracts(StringEntityProvider.class));
    }

    @Test
    public void getsComponentClasses() {
        Set<Class<?>> componentClasses = newHashSet(ByteEntityProvider.class, StringEntityProvider.class);
        when(providers.getClasses()).thenReturn(componentClasses);
        assertEquals(componentClasses, configuration.getClasses());
    }

    @Test
    public void getsComponentInstances() {
        Set<Object> components = newHashSet(new ByteEntityProvider(), new StringEntityProvider());
        when(providers.getInstances()).thenReturn(components);
        assertEquals(components, configuration.getInstances());
    }

    @Test
    public void setsProperty() {
        configuration.setProperty("name", "value");
        verify(configurationProperties).setProperty("name", "value");
    }

    @Test
    public void removesProperty() {
        configuration.removeProperty("name");
        verify(configurationProperties).removeProperty("name");
    }

    static class SomeFeature implements Feature {
        @Override
        public boolean configure(FeatureContext context) {
            return false;
        }
    }
}
