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

import org.everrest.core.ConfigurationProperties;
import org.everrest.core.Parameter;
import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.ApplicationContext;
import org.everrest.core.impl.InternalException;
import org.everrest.core.impl.provider.DefaultReaderInterceptorContext;
import org.everrest.core.impl.provider.MessageBodyReaderNotFoundException;
import org.everrest.core.method.MethodInvoker;
import org.everrest.core.method.ParameterResolver;
import org.everrest.core.method.ParameterResolverFactory;
import org.everrest.core.resource.GenericResourceMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static org.everrest.core.impl.header.HeaderHelper.getContentLengthLong;
import static org.everrest.core.impl.provider.DefaultReaderInterceptorContext.aReaderInterceptorContext;

/**
 * Invoker for Resource Method, Sub-Resource Method and SubResource Locator.
 *
 * @author andrew00x
 */
public class DefaultMethodInvoker implements MethodInvoker {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMethodInvoker.class);

    private final ParameterResolverFactory parameterResolverFactory;

    public DefaultMethodInvoker(ParameterResolverFactory parameterResolverFactory) {
        this.parameterResolverFactory = parameterResolverFactory;
    }

    @Override
    public final Object invokeMethod(Object resource, GenericResourceMethod methodResource, ApplicationContext context) {
        Object[] params = makeMethodParameters(methodResource, context);
        beforeInvokeMethod(resource, methodResource, params, context);
        return invokeMethod(resource, methodResource, params, context);
    }

    @SuppressWarnings({"unchecked"})
    private Object[] makeMethodParameters(GenericResourceMethod resourceMethod, ApplicationContext context) {
        Object[] params = new Object[resourceMethod.getMethodParameters().size()];
        int i = 0;
        for (Parameter methodParameter : resourceMethod.getMethodParameters()) {
            Annotation methodParameterAnnotation = methodParameter.getAnnotation();
            if (methodParameterAnnotation != null) {
                params[i++] = createAnnotatedParameter(context, methodParameter, methodParameterAnnotation);
            } else {
                params[i++] = createEntityParameter(context, methodParameter);
            }
        }
        return params;
    }

    private Object createAnnotatedParameter(ApplicationContext context, Parameter methodParameter, Annotation methodParameterAnnotation) {
        ParameterResolver<?> parameterResolver = parameterResolverFactory.createParameterResolver(methodParameterAnnotation);
        try {
            return parameterResolver.resolve(methodParameter, context);
        } catch (Exception e) {
            String errorMsg = String.format("Not able resolve method parameter %s", methodParameter);
            Class<?> annotationType = methodParameterAnnotation.annotationType();
            if (annotationType == MatrixParam.class || annotationType == QueryParam.class || annotationType == PathParam.class) {
                throw new WebApplicationException(e, Response.status(NOT_FOUND).entity(errorMsg).type(TEXT_PLAIN).build());
            }
            throw new WebApplicationException(e, Response.status(BAD_REQUEST).entity(errorMsg).type(TEXT_PLAIN).build());
        }
    }

    private Object createEntityParameter(ApplicationContext context, Parameter methodParameter) {
        Object result = null;
        InputStream entityStream = context.getContainerRequest().getEntityStream();
        MediaType mediaType = context.getContainerRequest().getMediaType();
        if (entityStream != null) {
            ProviderBinder providers = context.getProviders();
            ConfigurationProperties properties = context.getConfigurationProperties();
            DefaultReaderInterceptorContext readerInterceptorContext = aReaderInterceptorContext(providers, properties)
                    .withType(methodParameter.getParameterClass())
                    .withGenericType(methodParameter.getGenericType())
                    .withAnnotations(methodParameter.getAnnotations())
                    .withMediaType(mediaType)
                    .withHeaders(context.getContainerRequest().getHeaders())
                    .withEntityStream(entityStream)
                    .build();
            try {
                result = readerInterceptorContext.proceed();
            } catch (MessageBodyReaderNotFoundException e) {
                long contentLength = getContentLengthOrZeroIfHeaderInvalid(context);
                if (mediaType == null && contentLength == 0) {
                    result = null;
                } else {
                    LOG.debug(e.getMessage());
                    throw new WebApplicationException(Response.status(UNSUPPORTED_MEDIA_TYPE).entity(e.getMessage()).type(TEXT_PLAIN).build());
                }
            } catch (WebApplicationException | InternalException e) {
                throw e;
            } catch (Exception e) {
                throw new InternalException(e);
            }
        }
        return result;
    }

    private long getContentLengthOrZeroIfHeaderInvalid(ApplicationContext context) {
        long contentLength = 0;
        try {
            contentLength = getContentLengthLong(context.getContainerRequest().getRequestHeaders());
        } catch (NumberFormatException ignored) {
        }
        return contentLength;
    }

    protected void beforeInvokeMethod(Object resource, GenericResourceMethod methodResource, Object[] params, ApplicationContext context) {
    }

    protected Object invokeMethod(Object resource, GenericResourceMethod methodResource, Object[] params, ApplicationContext context) {
        try {
            return methodResource.getMethod().invoke(resource, params);
        } catch (IllegalArgumentException | IllegalAccessException unexpectedException) {
            throw new InternalException(unexpectedException);
        } catch (InvocationTargetException invocationException) {
            LOG.debug(invocationException.getMessage(), invocationException);

            Throwable cause = invocationException.getCause();

            if (cause instanceof WebApplicationException) {
                throw (WebApplicationException)cause;
            }

            if (cause instanceof InternalException) {
                throw (InternalException)cause;
            }

            throw new InternalException(cause);
        }
    }
}
