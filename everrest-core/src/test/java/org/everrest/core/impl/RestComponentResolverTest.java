package org.everrest.core.impl;

import org.everrest.core.ProviderBinder;
import org.everrest.core.ResourceBinder;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RestComponentResolverTest {
    private ResourceBinder resources;
    private ProviderBinder providers;

    private RestComponentResolver componentResolver;

    @Before
    public void setUp() {
        resources = mock(ResourceBinder.class);
        providers = mock(DefaultProviderBinder.class);

        componentResolver = new RestComponentResolver(resources, providers);
    }

    @Test
    public void resolvesPerRequestResource() {
        componentResolver.addPerRequest(Resource.class);
        verify(resources).addResource(Resource.class, null);
    }

    @Test
    public void resolvesSingletonResource() {
        Resource resource = new Resource();
        componentResolver.addSingleton(resource);
        verify(resources).addResource(resource, null);
    }

    @Path("a")
    public static class Resource {
    }

    @Test
    public void resolvesPerRequestProvider() {
        componentResolver.addPerRequest(RuntimeExceptionMapper.class);
        verify(providers).register(RuntimeExceptionMapper.class);
    }

    @Test
    public void resolvesSingletonProvider() {
        ExceptionMapper exceptionMapper = new RuntimeExceptionMapper();
        componentResolver.addSingleton(exceptionMapper);
        verify(providers).register(exceptionMapper);
    }

    @Provider
    public static class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {
        @Override
        public Response toResponse(RuntimeException exception) {
            return null;
        }
    }
}