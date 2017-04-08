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

import org.everrest.core.ConfigurationProperties;
import org.everrest.core.DependencySupplier;
import org.everrest.core.GenericContainerRequest;
import org.everrest.core.GenericContainerResponse;
import org.everrest.core.Lifecycle;
import org.everrest.core.ProviderBinder;
import org.everrest.core.async.AsynchronousJobPool;
import org.everrest.core.impl.async.AsynchronousMethodInvoker;
import org.everrest.core.impl.method.DefaultMethodInvoker;
import org.everrest.core.impl.method.DefaultParameterResolverFactory;
import org.everrest.core.impl.method.DefaultTypeProducerFactory;
import org.everrest.core.impl.method.MethodInvokerDecoratorFactory;
import org.everrest.core.impl.method.OptionsRequestMethodInvoker;
import org.everrest.core.impl.uri.UriComponent;
import org.everrest.core.method.MethodInvoker;
import org.everrest.core.method.ParameterResolverFactory;
import org.everrest.core.resource.GenericResourceMethod;
import org.everrest.core.resource.ResourceMethodDescriptor;
import org.everrest.core.servlet.ServletContainerRequest;
import org.everrest.core.tools.SimplePrincipal;
import org.everrest.core.tools.SimpleSecurityContext;
import org.everrest.core.tools.WebApplicationDeclaredRoles;
import org.everrest.core.uri.UriPattern;
import org.everrest.core.wadl.WadlProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.everrest.core.impl.uri.UriComponent.parsePathSegments;

/**
 * @author andrew00x
 */
public class ApplicationContext implements UriInfo, Lifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationContext.class);

    /** {@link ThreadLocal} ApplicationContext. */
    private static ThreadLocal<ApplicationContext> current = new ThreadLocal<>();

    /** @return current ApplicationContext. */
    public static ApplicationContext getCurrent() {
        return current.get();
    }

    /**
     * Set ApplicationContext for current thread.
     *
     * @param context the ApplicationContext.
     */
    public static void setCurrent(ApplicationContext context) {
        current.set(context);
    }

    /** See {@link GenericContainerRequest}. */
    private GenericContainerRequest        request;
    /** See {@link ContainerResponse}. */
    private GenericContainerResponse       response;
    /** Providers. */
    private ProviderBinder                 providers;
    private DependencySupplier             dependencySupplier;
    /** Values of template parameters. */
    private List<String>                   parameterValues;
    /** List of matched resources. */
    private List<Object>                   matchedResources;
    /** List of not decoded matched URIs. */
    private List<String>                   encodedMatchedURIs;
    /** List of decoded matched URIs. */
    private List<String>                   matchedURIs;
    /** Mutable runtime attributes. */
    private Map<String, Object>            attributes;
    /** Absolute path, full requested URI without query string and fragment. */
    private URI                            absolutePath;
    /** Decoded relative path. */
    private String                         path;
    /** Not decoded relative path. */
    private String                         encodedPath;
    /** Not decoded path template parameters. */
    private MultivaluedMap<String, String> encodedPathParameters;
    /** Decoded path template parameters. */
    private MultivaluedMap<String, String> pathParameters;
    /** List of not decoded path segments. */
    private List<PathSegment>              encodedPathSegments;
    /** Decoded path segments. */
    private List<PathSegment>              pathSegments;
    /** Not decoded query parameters. */
    private MultivaluedMap<String, String> encodedQueryParameters;
    /** Decoded query parameters. */
    private MultivaluedMap<String, String> queryParameters;
    private SecurityContext                asynchronousSecurityContext;
    private Application                    application;
    private ConfigurationProperties configuration;
    private MethodInvokerDecoratorFactory  methodInvokerDecoratorFactory;
    private ParameterResolverFactory       parameterResolverFactory;
    private EnvironmentContext             environmentContext;
    private ProcessingPhase                processingPhase;

    private ApplicationContext(ApplicationContextBuilder builder) {
        request = builder.request;
        response = builder.response;
        providers = builder.providers;
        application = builder.application;
        configuration = builder.configuration == null ? new ServerConfigurationProperties() : builder.configuration;
        dependencySupplier = builder.dependencySupplier;
        methodInvokerDecoratorFactory = builder.methodInvokerDecoratorFactory;
        parameterResolverFactory = builder.parameterResolverFactory;
        environmentContext = builder.environmentContext;
        parameterValues = new ArrayList<>();
        matchedResources = new ArrayList<>();
        encodedMatchedURIs = new ArrayList<>();
        matchedURIs = new ArrayList<>();
    }

    /**
     * Add ancestor resource, according to JSR-311:
     * <p>
     * Entries are ordered according in reverse request URI matching order, with the root resource last.
     * </p>
     * So add each new resource at the begin of list.
     *
     * @param resource
     *         the resource e. g. resource class, sub-resource method or sub-resource locator.
     */
    public void addMatchedResource(Object resource) {
        matchedResources.add(0, resource);
    }

    /**
     * Add ancestor resource, according to JSR-311:
     * <p>
     * Entries are ordered in reverse request URI matching order, with the root resource URI last.
     * </p>
     * So add each new URI at the begin of list.
     *
     * @param uri
     *         the partial part of that matched to resource class, sub-resource method or sub-resource locator.
     */
    public void addMatchedURI(String uri) {
        encodedMatchedURIs.add(0, uri);
        matchedURIs.add(0, UriComponent.decode(uri, UriComponent.PATH_SEGMENT));
    }

    @Override
    public URI getAbsolutePath() {
        if (absolutePath == null) {
            absolutePath = getRequestUriBuilder().replaceQuery(null).fragment(null).build();
        }
        return absolutePath;
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
        return UriBuilder.fromUri(getAbsolutePath());
    }

    /** @return get mutable runtime attributes */
    public Map<String, Object> getAttributes() {
        return attributes == null ? attributes = new HashMap<>() : attributes;
    }

    @Override
    public URI getBaseUri() {
        return request.getBaseUri();
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
        return UriBuilder.fromUri(getBaseUri());
    }

    /** @return See {@link GenericContainerRequest} */
    public GenericContainerRequest getContainerRequest() {
        return request;
    }

    /** @return See {@link GenericContainerResponse} */
    public GenericContainerResponse getContainerResponse() {
        return response;
    }

    /** @return See {@link DependencySupplier} */
    public DependencySupplier getDependencySupplier() {
        return dependencySupplier;
    }

    /** @return See {@link HttpHeaders} */
    public HttpHeaders getHttpHeaders() {
        return request;
    }

    @Override
    public List<Object> getMatchedResources() {
        return matchedResources;
    }

    @Override
    public URI resolve(URI uri) {
        return UriComponent.resolve(getBaseUri(), uri);
    }

    @Override
    public URI relativize(URI uri) {
        if (!uri.isAbsolute()) {
            uri = resolve(uri);
        }
        return getRequestUri().relativize(uri);
    }

    @Override
    public List<String> getMatchedURIs() {
        return getMatchedURIs(true);
    }

    @Override
    public List<String> getMatchedURIs(boolean decode) {
        return decode ? matchedURIs : encodedMatchedURIs;
    }

    /**
     * Returns appropriate implementation of {@code MethodInvoker}
     *
     * @param methodDescriptor method descriptor
     * @return invoker that must be used for processing methods
     */
    public MethodInvoker getMethodInvoker(GenericResourceMethod methodDescriptor) {
        String method = request.getMethod();
        if ("OPTIONS".equals(method) && methodDescriptor.getMethod() == null) {
            // GenericMethodResource.getMethod() always return null if method for
            // "OPTIONS" request was not described in source code of service. In
            // this case we provide mechanism for "fake" method invoking.
            return new OptionsRequestMethodInvoker(new WadlProcessor());
        }
        MethodInvoker invoker = null;
        // Never use AsynchronousMethodInvoker for process SubResourceLocatorDescriptor.
        // Locators can't be processed in asynchronous mode since it is not end point of request.
        if (isAsynchronous() && methodDescriptor instanceof ResourceMethodDescriptor) {
            ContextResolver<AsynchronousJobPool> asyncJobsResolver = getProviders().getContextResolver(AsynchronousJobPool.class, null);
            if (asyncJobsResolver == null) {
                throw new IllegalStateException("Asynchronous jobs feature is not configured properly. ");
            }
            invoker = new AsynchronousMethodInvoker(asyncJobsResolver.getContext(null), getParameterResolverFactory());
        }
        if (invoker == null) {
            invoker = new DefaultMethodInvoker(getParameterResolverFactory());
        }
        if (methodInvokerDecoratorFactory != null) {
            invoker = methodInvokerDecoratorFactory.makeDecorator(invoker);
        }
        return invoker;
    }

    /**
     * Should be used to pass template values in context by using returned list in matching to @see
     * {@link UriPattern#match(String, List)}. List will be cleared during matching.
     *
     * @return the list for template values
     */
    public List<String> getParameterValues() {
        return parameterValues;
    }

    @Override
    public String getPath() {
        return getPath(true);
    }

    @Override
    public String getPath(boolean decode) {
        if (encodedPath == null) {
            encodedPath = getAbsolutePath().getRawPath().substring(getBaseUri().getRawPath().length());
        }
        if (decode) {
            if (path == null) {
                path = UriComponent.decode(encodedPath, UriComponent.PATH);
            }
            return path;
        }
        return encodedPath;
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        return getPathParameters(true);
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        if (encodedPathParameters == null) {
            throw new IllegalStateException("Path template variables not initialized yet.");
        }
        if (decode) {
            if (pathParameters == null) {
                pathParameters = new MultivaluedMapImpl();
            }
            if (pathParameters.size() != encodedPathParameters.size()) {
                for (String key : encodedPathParameters.keySet()) {
                    if (!pathParameters.containsKey(key)) {
                        pathParameters.putSingle(UriComponent.decode(key, UriComponent.PATH_SEGMENT),
                                                 UriComponent.decode(encodedPathParameters.getFirst(key), UriComponent.PATH));
                    }
                }
            }
            return pathParameters;
        }
        return encodedPathParameters;
    }

    @Override
    public List<PathSegment> getPathSegments() {
        return getPathSegments(true);
    }

    @Override
    public List<PathSegment> getPathSegments(boolean decode) {
        if (decode) {
            return pathSegments == null ? (pathSegments = parsePathSegments(getPath(true), true)) : pathSegments;
        }
        return encodedPathSegments == null ? (encodedPathSegments = parsePathSegments(getPath(false), false)) : encodedPathSegments;
    }

    public ProviderBinder getProviders() {
        return providers;
    }

    public void setProviders(ProviderBinder providers) {
        this.providers = providers;
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        return getQueryParameters(true);
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        if (decode) {
            return queryParameters != null ? queryParameters : (queryParameters =
                    UriComponent.parseQueryString(getRequestUri().getRawQuery(), true));
        }
        return encodedQueryParameters != null ? encodedQueryParameters : (encodedQueryParameters =
                UriComponent.parseQueryString(getRequestUri().getRawQuery(), false));
    }

    public Request getRequest() {
        return request;
    }

    @Override
    public URI getRequestUri() {
        return request.getRequestUri();
    }

    @Override
    public UriBuilder getRequestUriBuilder() {
        return UriBuilder.fromUri(getRequestUri());
    }

    /** @return See {@link SecurityContext} */
    public SecurityContext getSecurityContext() {
        // We get security information from HttpServletRequest but we may be not able to do this is asynchronous mode.
        // In asynchronous mode resource method processed when HTTP request ended already and we cannot use it anymore.
        // Do some workaround to keep security info even after request ends.
        if (isAsynchronous() && (request instanceof ServletContainerRequest)) {
            if (asynchronousSecurityContext == null) {
                Principal requestPrincipal = request.getUserPrincipal();
                if (requestPrincipal == null) {
                    asynchronousSecurityContext = new SimpleSecurityContext(request.isSecure());
                } else {
                    // Info about roles declared for web application. We assume this is all roles that we can meet.
                    WebApplicationDeclaredRoles declaredRoles = getEnvironmentContext().get(WebApplicationDeclaredRoles.class);
                    if (declaredRoles == null) {
                        asynchronousSecurityContext = new SimpleSecurityContext(new SimplePrincipal(requestPrincipal.getName()),
                                                                                null,
                                                                                request.getAuthenticationScheme(),
                                                                                request.isSecure());
                    } else {
                        Set<String> userRoles = declaredRoles.getDeclaredRoles().stream()
                                .filter(declaredRole -> request.isUserInRole(declaredRole))
                                .collect(toSet());
                        asynchronousSecurityContext = new SimpleSecurityContext(new SimplePrincipal(requestPrincipal.getName()),
                                                                                userRoles,
                                                                                request.getAuthenticationScheme(),
                                                                                request.isSecure());
                    }
                }
            }
            return asynchronousSecurityContext;
        }
        return request;
    }

    /** @return See {@link UriInfo} */
    public UriInfo getUriInfo() {
        return this;
    }

    /**
     * Pass in context list of path template parameters @see {@link UriPattern}.
     *
     * @param parameterNames
     *         list of templates parameters
     */
    public void setParameterNames(List<String> parameterNames) {
        if (encodedPathParameters == null) {
            encodedPathParameters = new MultivaluedMapImpl();
        }
        for (int i = 0; i < parameterNames.size(); i++) {
            encodedPathParameters.add(parameterNames.get(i), parameterValues.get(i));
        }
    }

    /** @return {@code true} if request is asynchronous and {@code false} otherwise, */
    public boolean isAsynchronous() {
        return Boolean.parseBoolean(getQueryParameters().getFirst("async"))
               || Boolean.parseBoolean(request.getRequestHeaders().getFirst("x-everrest-async"));
    }

    public Application getApplication() {
        return application;
    }

    public ConfigurationProperties getConfigurationProperties() {
        return configuration;
    }

    public ParameterResolverFactory getParameterResolverFactory() {
        return parameterResolverFactory == null
                ? parameterResolverFactory = new DefaultParameterResolverFactory(new DefaultTypeProducerFactory())
                : parameterResolverFactory;
    }

    public EnvironmentContext getEnvironmentContext() {
        return environmentContext;
    }

    public ProcessingPhase getProcessingPhase() {
        return processingPhase;
    }

    public void setProcessingPhase(ProcessingPhase processingPhase) {
        this.processingPhase = processingPhase;
    }

    @Override
    public final void start() {
    }

    @Override
    public final void stop() {
        @SuppressWarnings("unchecked")
        List<LifecycleComponent> perRequestComponents = (List<LifecycleComponent>)getAttributes().get("org.everrest.lifecycle.PerRequest");
        if (perRequestComponents != null && !perRequestComponents.isEmpty()) {
            for (LifecycleComponent component : perRequestComponents) {
                try {
                    component.destroy();
                } catch (InternalException e) {
                    LOG.error("Unable to destroy component", e);
                }
            }
            perRequestComponents.clear();
        }
    }

    public static ApplicationContextBuilder anApplicationContext() {
        return new ApplicationContextBuilder();
    }

    public static class ApplicationContextBuilder {
        private GenericContainerRequest       request;
        private GenericContainerResponse      response;
        private ProviderBinder                providers;
        private Application                   application;
        private ConfigurationProperties       configuration;
        private EnvironmentContext            environmentContext;
        private DependencySupplier            dependencySupplier;
        private ParameterResolverFactory      parameterResolverFactory;
        private MethodInvokerDecoratorFactory methodInvokerDecoratorFactory;

        private ApplicationContextBuilder() {
        }

        public ApplicationContextBuilder withRequest(GenericContainerRequest request) {
            this.request = request;
            return this;
        }

        public ApplicationContextBuilder withResponse(GenericContainerResponse response) {
            this.response = response;
            return this;
        }

        public ApplicationContextBuilder withProviders(ProviderBinder providers) {
            this.providers = providers;
            return this;
        }

        public ApplicationContextBuilder withApplication(Application application) {
            this.application = application;
            return this;
        }

        public ApplicationContextBuilder withMethodInvokerDecoratorFactory(MethodInvokerDecoratorFactory methodInvokerDecoratorFactory) {
            this.methodInvokerDecoratorFactory = methodInvokerDecoratorFactory;
            return this;
        }

        public ApplicationContextBuilder withConfiguration(ConfigurationProperties configuration) {
            this.configuration = configuration;
            return this;
        }

        public ApplicationContextBuilder withDependencySupplier(DependencySupplier dependencySupplier) {
            this.dependencySupplier = dependencySupplier;
            return this;
        }

        public ApplicationContextBuilder withParameterResolverFactory(ParameterResolverFactory parameterResolverFactory) {
            this.parameterResolverFactory = parameterResolverFactory;
            return this;
        }

        public ApplicationContextBuilder withEnvironmentContext(EnvironmentContext environmentContext) {
            this.environmentContext = environmentContext;
            return this;
        }

        public ApplicationContext build() {
            return new ApplicationContext(this);
        }
    }
}
