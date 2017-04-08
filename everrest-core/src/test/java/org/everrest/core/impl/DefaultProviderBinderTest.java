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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.everrest.core.ConfigurationProperties;
import org.everrest.core.ProviderBinder;
import org.everrest.core.impl.provider.ByteEntityProvider;
import org.everrest.core.impl.provider.DOMSourceEntityProvider;
import org.everrest.core.impl.provider.DefaultExceptionMapper;
import org.everrest.core.impl.provider.StringEntityProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Produces;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import javax.xml.transform.dom.DOMSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class DefaultProviderBinderTest {
    @Provider
    @Produces("text/plain")
    public static class ContextResolverText implements ContextResolver<String> {
        public String getContext(Class<?> type) {
            return null;
        }
    }

    @Provider
    public static class ContextResolverWildcard implements ContextResolver<String> {
        public String getContext(Class<?> type) {
            return null;
        }
    }

    @Provider
    @Produces("text/xml")
    public static class ContextResolverXml implements ContextResolver<String> {
        public String getContext(Class<?> type) {
            return null;
        }
    }

    @Provider
    @Produces("text/*")
    public static class ContextResolverAnyText implements ContextResolver<String> {
        public String getContext(Class<?> type) {
            return null;
        }
    }


    @Provider
    public static class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {
        @Override
        public Response toResponse(RuntimeException exception) {
            return null;
        }
    }

    @Priority(99)
    public static class ReaderInterceptorImplOne implements ReaderInterceptor {
        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            return null;
        }
    }

    @Priority(101)
    public static class ReaderInterceptorImplTwo implements ReaderInterceptor {
        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            return null;
        }
    }

    @Priority(99)
    public static class WriterInterceptorImplOne implements WriterInterceptor {
        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        }
    }

    @Priority(101)
    public static class WriterInterceptorImplTwo implements WriterInterceptor {
        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        }
    }

    @Priority(69)
    public static class ClientRequestFilterImplOne implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
        }
    }

    @Priority(96)
    public static class ClientRequestFilterImplTwo implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
        }
    }

    @Priority(12)
    public static class ClientResponseFilterImplOne implements ClientResponseFilter {
        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        }
    }

    @Priority(21)
    public static class ClientResponseFilterImplTwo implements ClientResponseFilter {
        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        }
    }

    @Priority(13)
    public static class ContainerRequestFilterImplOne implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
        }
    }

    @Priority(31)
    public static class ContainerRequestFilterImplTwo implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
        }
    }

    @PreMatching
    public static class ContainerRequestFilterImplThree implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
        }
    }

    @_Named
    @PreMatching
    public static class ContainerRequestFilterImplFour implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
        }
    }

    @_Named
    public static class ContainerRequestFilterImplFive implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
        }
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @NameBinding
    public @interface _Named {
    }

    @Priority(14)
    public static class ContainerResponseFilterImplOne implements ContainerResponseFilter {
        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        }
    }

    @Priority(41)
    public static class ContainerResponseFilterImplTwo implements ContainerResponseFilter {
        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        }
    }

    public static class ParamConverterProviderImpl implements ParamConverterProvider {
        @Override
        public ParamConverter<Object> getConverter(Class rawType, Type genericType, Annotation[] annotations) {
            return new ParamConverterImpl();
        }
    }

    static class ParamConverterImpl implements ParamConverter<Object> {
        @Override
        public Object fromString(String value) {
            return null;
        }

        @Override
        public String toString(Object value) {
            return null;
        }
    }

    public static class FeatureImpl implements Feature {
        @Override
        public boolean configure(FeatureContext context) {
            context.register(ContainerRequestFilterImplOne.class);
            context.register(ContainerResponseFilterImplOne.class);
            return true;
        }
    }

    public static class DynamicFeatureImpl implements DynamicFeature {
        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        }
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ProviderBinder providers;

    private static ContextResolverText     contextResolverText     = new ContextResolverText();
    private static ContextResolverWildcard contextResolverWildcard = new ContextResolverWildcard();
    private static ContextResolverXml      contextResolverXml      = new ContextResolverXml();
    private static ContextResolverAnyText  contextResolverAnyText  = new ContextResolverAnyText();

    private static RuntimeExceptionMapper runtimeExceptionMapper = new RuntimeExceptionMapper();
    private static DefaultExceptionMapper defaultExceptionMapper = new DefaultExceptionMapper();

    private static MessageBodyReader<String> stringMessageBodyReader = new StringEntityProvider();
    private static MessageBodyReader<byte[]> byteMessageBodyReader   = new ByteEntityProvider();

    private static MessageBodyWriter<String> stringMessageBodyWriter = new StringEntityProvider();
    private static MessageBodyWriter<byte[]> byteMessageBodyWriter   = new ByteEntityProvider();

    private static final boolean SINGLETON = true;
    private static final boolean PER_REQUEST = false;

    private Appender<ILoggingEvent> mockLogbackAppender;

    private ApplicationContext context;
    private ConfigurationProperties configurationProperties;

    @Before
    public void setUp() throws Exception {
        context = mock(ApplicationContext.class);
        configurationProperties = mock(ConfigurationProperties.class);
        providers = new DefaultProviderBinder(RuntimeType.SERVER, configurationProperties);

        setUpLogbackAppender();
    }

    private void setUpLogbackAppender() {
        ch.qos.logback.classic.Logger providerBinderLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(DefaultProviderBinder.class);
        mockLogbackAppender = mockLogbackAppender();
        providerBinderLogger.addAppender(mockLogbackAppender);
    }

    private Appender mockLogbackAppender() {
        Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MockAppender");
        return mockAppender;
    }

    @After
    public void tearDown() {
        ch.qos.logback.classic.Logger providerBinderLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(DefaultProviderBinder.class);
        providerBinderLogger.detachAppender(mockLogbackAppender);

        ApplicationContext.setCurrent(null);
    }

    @DataProvider
    public static Object[][] contextResolverByClassAndMediaTypeData() {
        return new Object[][] {
                {PER_REQUEST, String.class, new MediaType("text", "plain"), ContextResolverText.class},
                {PER_REQUEST, String.class, new MediaType("text", "xml"),   ContextResolverXml.class},
                {PER_REQUEST, String.class, new MediaType("text", "xxx"),   ContextResolverAnyText.class},
                {PER_REQUEST, String.class, new MediaType("xxx", "xxx"),    ContextResolverWildcard.class},
                {PER_REQUEST, Object.class, new MediaType("xxx", "xxx"),    null},
                {SINGLETON,   String.class, new MediaType("text", "plain"), contextResolverText},
                {SINGLETON,   String.class, new MediaType("text", "xml"),   contextResolverXml},
                {SINGLETON,   String.class, new MediaType("text", "xxx"),   contextResolverAnyText},
                {SINGLETON,   String.class, new MediaType("xxx", "xxx"),    contextResolverWildcard},
                {SINGLETON,   Object.class, new MediaType("xxx", "xxx"),    null}
        };
    }

    @Test
    @UseDataProvider("contextResolverByClassAndMediaTypeData")
    public void retrievesContextResolverByClassAndMediaType(boolean singletonOrPerRequest,
                                                            Class<?> aClass,
                                                            MediaType mediaType,
                                                            Object expectedContextResolverClassOrInstance) throws Exception {
        if (singletonOrPerRequest == SINGLETON) {
            registerSingletonContextResolvers();
        } else {
            ApplicationContext.setCurrent(context);
            registerPerRequestContextResolvers();
        }

        ContextResolver<?> contextResolver = providers.getContextResolver(aClass, mediaType);
        if (singletonOrPerRequest == SINGLETON) {
            assertSame(expectedContextResolverClassOrInstance, contextResolver);
        } else {
            if (expectedContextResolverClassOrInstance == null) {
                assertNull(contextResolver);
            } else {
                assertNotNull(contextResolver);
                assertEquals(expectedContextResolverClassOrInstance, contextResolver.getClass());
            }
        }
    }

    private void registerPerRequestContextResolvers() {
        providers.register(ContextResolverText.class);
        providers.register(ContextResolverWildcard.class);
        providers.register(ContextResolverXml.class);
        providers.register(ContextResolverAnyText.class);
    }

    private void registerSingletonContextResolvers() {
        providers.register(contextResolverText);
        providers.register(contextResolverWildcard);
        providers.register(contextResolverXml);
        providers.register(contextResolverAnyText);
    }


    @DataProvider
    public static Object[][] exceptionMapperByExceptionType() {
        return new Object[][] {
                {PER_REQUEST, Exception.class, DefaultExceptionMapper.class},
                {PER_REQUEST, RuntimeException.class, RuntimeExceptionMapper.class},
                {PER_REQUEST, IOException.class, DefaultExceptionMapper.class},
                {SINGLETON, Exception.class, defaultExceptionMapper},
                {SINGLETON, RuntimeException.class, runtimeExceptionMapper},
                {SINGLETON, IOException.class, defaultExceptionMapper}
        };
    }

    @Test
    @UseDataProvider("exceptionMapperByExceptionType")
    public <T extends Throwable> void retrievesExceptionMapperByExceptionType(boolean singletonOrPerRequest,
                                                                              Class<T> errorClass,
                                                                              Object expectedExceptionMapperClassOrInstance) throws Exception {
        if (singletonOrPerRequest == SINGLETON) {
            registerSingletonExceptionMappers();
        } else {
            ApplicationContext.setCurrent(context);
            registerPerRequestExceptionMappers();
        }

        ExceptionMapper<T> exceptionMapper = providers.getExceptionMapper(errorClass);
        if (singletonOrPerRequest == SINGLETON) {
            assertSame(expectedExceptionMapperClassOrInstance, exceptionMapper);
        } else {
            assertNotNull(exceptionMapper);
            assertEquals(expectedExceptionMapperClassOrInstance, exceptionMapper.getClass());
        }
    }

    private void registerPerRequestExceptionMappers() {
        providers.register(DefaultExceptionMapper.class);
        providers.register(RuntimeExceptionMapper.class);
    }

    private void registerSingletonExceptionMappers() {
        providers.register(defaultExceptionMapper);
        providers.register(runtimeExceptionMapper);
    }


    @DataProvider
    public static Object[][] messageBodyReaderByTypeAndMediaType() {
        return new Object[][] {
                {SINGLETON,   String.class, null, TEXT_PLAIN_TYPE, stringMessageBodyReader},
                {SINGLETON,   byte[].class, null, TEXT_PLAIN_TYPE, byteMessageBodyReader},
                {SINGLETON,   Object.class, null, TEXT_PLAIN_TYPE, null},
                {PER_REQUEST, String.class, null, TEXT_PLAIN_TYPE, StringEntityProvider.class},
                {PER_REQUEST, byte[].class, null, TEXT_PLAIN_TYPE, ByteEntityProvider.class},
                {PER_REQUEST, Object.class, null, TEXT_PLAIN_TYPE, null}
        };
    }

    @Test
    @UseDataProvider("messageBodyReaderByTypeAndMediaType")
    public void retrievesMessageBodyReaderByTypeAndMediaType(boolean singletonOrPerRequest,
                                                             Class<?> readObjectType,
                                                             Type readObjectGenericType,
                                                             MediaType mediaType,
                                                             Object expectedMessageBodyReaderClassOrInstance) throws Exception {
        if (singletonOrPerRequest == SINGLETON) {
            registerSingletonMessageBodyReaders();
        } else {
            ApplicationContext.setCurrent(context);
            registerPerRequestMessageBodyReaders();
        }

        MessageBodyReader messageBodyReader = providers.getMessageBodyReader(readObjectType, readObjectGenericType, null, mediaType);
        if (singletonOrPerRequest == SINGLETON) {
            assertSame(expectedMessageBodyReaderClassOrInstance, messageBodyReader);
        } else {
            if (expectedMessageBodyReaderClassOrInstance == null) {
                assertNull(messageBodyReader);
            } else {
                assertNotNull(messageBodyReader);
                assertEquals(expectedMessageBodyReaderClassOrInstance, messageBodyReader.getClass());
            }
        }
    }

    private void registerPerRequestMessageBodyReaders() {
        providers.register(StringEntityProvider.class);
        providers.register(ByteEntityProvider.class);
    }

    private void registerSingletonMessageBodyReaders() {
        providers.register(stringMessageBodyReader);
        providers.register(byteMessageBodyReader);
    }


    @DataProvider
    public static Object[][] messageBodyWriterByTypeAndMediaType() {
        return new Object[][] {
                {SINGLETON,   String.class, null, TEXT_PLAIN_TYPE, stringMessageBodyWriter},
                {SINGLETON,   byte[].class, null, TEXT_PLAIN_TYPE, byteMessageBodyWriter},
                {SINGLETON,   Object.class, null, TEXT_PLAIN_TYPE, null},
                {PER_REQUEST, String.class, null, TEXT_PLAIN_TYPE, StringEntityProvider.class},
                {PER_REQUEST, byte[].class, null, TEXT_PLAIN_TYPE, ByteEntityProvider.class},
                {PER_REQUEST, Object.class, null, TEXT_PLAIN_TYPE, null}
        };
    }

    @Test
    @UseDataProvider("messageBodyWriterByTypeAndMediaType")
    public void retrievesMessageBodyWriterByTypeAndMediaType(boolean singletonOrPerRequest,
                                                             Class<?> writeObjectType,
                                                             Type writeObjectGenericType,
                                                             MediaType mediaType,
                                                             Object expectedMessageBodyWriterClassOrInstance) throws Exception {
        if (singletonOrPerRequest == SINGLETON) {
            registerSingletonMessageBodyWriters();
        } else {
            ApplicationContext.setCurrent(context);
            registerPerRequestMessageBodyWriters();
        }

        MessageBodyWriter messageBodyWriter = providers.getMessageBodyWriter(writeObjectType, writeObjectGenericType, null, mediaType);
        if (singletonOrPerRequest == SINGLETON) {
            assertSame(expectedMessageBodyWriterClassOrInstance, messageBodyWriter);
        } else {
            if (expectedMessageBodyWriterClassOrInstance == null) {
                assertNull(messageBodyWriter);
            } else {
                assertNotNull(messageBodyWriter);
                assertEquals(expectedMessageBodyWriterClassOrInstance, messageBodyWriter.getClass());
            }
        }
    }

    private void registerPerRequestMessageBodyWriters() {
        providers.register(StringEntityProvider.class);
        providers.register(ByteEntityProvider.class);
    }

    private void registerSingletonMessageBodyWriters() {
        providers.register(stringMessageBodyWriter);
        providers.register(byteMessageBodyWriter);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void retrievesAcceptableWriterMediaTypes() throws Exception {
        ApplicationContext.setCurrent(context);
        providers.register(DOMSourceEntityProvider.class);

        assertEquals(newArrayList(new MediaType("application", "xml"), new MediaType("text", "xml"), new MediaType("application", "*+xml"), new MediaType("text", "*+xml")),
                providers.getAcceptableWriterMediaTypes(DOMSource.class, null, null));
    }

    @Test
    public void registersExceptionMapperClass() {
        ApplicationContext.setCurrent(context);
        providers.register(RuntimeExceptionMapper.class);
        assertTrue(providers.isRegistered(RuntimeExceptionMapper.class));
        ExceptionMapper<RuntimeException> exceptionMapper = providers.getExceptionMapper(RuntimeException.class);
        assertTrue(exceptionMapper instanceof RuntimeExceptionMapper);
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateExceptionMapperClasses() {
        providers.register(RuntimeExceptionMapper.class);
        providers.register(RuntimeExceptionMapper.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ExceptionMapper {}. ExceptionMapper for exception {} already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{RuntimeExceptionMapper.class, RuntimeException.class}));
    }

    @Test
    public void registersExceptionMapper() {
        ExceptionMapper<RuntimeException> exceptionMapper = new RuntimeExceptionMapper();
        providers.register(exceptionMapper);
        assertTrue(providers.isRegistered(RuntimeExceptionMapper.class));
        assertTrue(providers.isRegistered(exceptionMapper));
        assertSame(exceptionMapper, providers.getExceptionMapper(RuntimeException.class));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateExceptionMappers() {
        providers.register(new RuntimeExceptionMapper());
        ExceptionMapper<RuntimeException> duplicateExceptionMapper = new RuntimeExceptionMapper();
        providers.register(duplicateExceptionMapper);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ExceptionMapper {}. ExceptionMapper for exception {} already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateExceptionMapper, RuntimeException.class}));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateExceptionMapperClassAndInstance() {
        providers.register(new RuntimeExceptionMapper());
        providers.register(RuntimeExceptionMapper.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ExceptionMapper {}. ExceptionMapper for exception {} already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{RuntimeExceptionMapper.class, RuntimeException.class}));
    }

    @Test
    public void registersMessageBodyReaderClass() {
        ApplicationContext.setCurrent(context);
        providers.register(StringEntityProvider.class);
        assertTrue(providers.isRegistered(StringEntityProvider.class));
        MessageBodyReader<String> reader = providers.getMessageBodyReader(String.class, String.class, null, new MediaType("*", "*"));
        assertTrue(reader instanceof StringEntityProvider);
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateMessageBodyReaderClasses() {
        providers.register(StringEntityProvider.class);
        providers.register(StringEntityProvider.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore MessageBodyReader {} for media type {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{StringEntityProvider.class, new MediaType("*", "*")}));
    }

    @Test
    public void registersMessageBodyReader() {
        MessageBodyReader reader = new StringEntityProvider();
        providers.register(reader);
        assertTrue(providers.isRegistered(StringEntityProvider.class));
        assertTrue(providers.isRegistered(reader));
        assertSame(reader, providers.getMessageBodyReader(String.class, String.class, null, new MediaType("*", "*")));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateMessageBodyReaders() {
        providers.register(new StringEntityProvider());
        StringEntityProvider duplicateReader = new StringEntityProvider();
        providers.register(duplicateReader);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore MessageBodyReader {} for media type {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateReader, new MediaType("*", "*")}));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateMessageBodyReaderClassAndInstance() {
        providers.register(new StringEntityProvider());
        providers.register(StringEntityProvider.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore MessageBodyReader {} for media type {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{StringEntityProvider.class, new MediaType("*", "*")}));
    }

    @Test
    public void registersMessageBodyWriterClass() {
        ApplicationContext.setCurrent(context);
        providers.register(StringEntityProvider.class, MessageBodyWriter.class);
        assertTrue(providers.isRegistered(StringEntityProvider.class));
        MessageBodyWriter<String> writer = providers.getMessageBodyWriter(String.class, String.class, null, new MediaType("*", "*"));
        assertTrue(writer instanceof StringEntityProvider);
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateMessageBodyWriterClasses() {
        providers.register(StringEntityProvider.class, MessageBodyWriter.class);
        providers.register(StringEntityProvider.class, MessageBodyWriter.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore MessageBodyWriter {} for media type {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{StringEntityProvider.class, new MediaType("*", "*")}));
    }

    @Test
    public void registersMessageBodyWriter() {
        MessageBodyReader writer = new StringEntityProvider();
        providers.register(writer, MessageBodyWriter.class);
        assertTrue(providers.isRegistered(StringEntityProvider.class));
        assertTrue(providers.isRegistered(writer));
        assertSame(writer, providers.getMessageBodyWriter(String.class, String.class, null, new MediaType("*", "*")));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateMessageBodyWriters() {
        providers.register(new StringEntityProvider(), MessageBodyWriter.class);
        StringEntityProvider duplicateReader = new StringEntityProvider();
        providers.register(duplicateReader, MessageBodyWriter.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore MessageBodyWriter {} for media type {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateReader, new MediaType("*", "*")}));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateMessageBodyWriterClassAndInstance() {
        providers.register(new StringEntityProvider(), MessageBodyWriter.class);
        providers.register(StringEntityProvider.class, MessageBodyWriter.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore MessageBodyWriter {} for media type {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{StringEntityProvider.class, new MediaType("*", "*")}));
    }

    @Test
    public void registersContextResolverClass() {
        ApplicationContext.setCurrent(context);
        providers.register(ContextResolverXml.class);
        assertTrue(providers.isRegistered(ContextResolverXml.class));
        ContextResolver<String> resolver = providers.getContextResolver(String.class, new MediaType("text", "xml"));
        assertTrue(resolver instanceof ContextResolverXml);
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateContextResolverClasses() {
        providers.register(ContextResolverXml.class);
        providers.register(ContextResolverXml.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ContextResolver {}. ContextResolver for {} and media type {} already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{ContextResolverXml.class, String.class, new MediaType("text", "xml")}));
    }

    @Test
    public void registersContextResolver() {
        ContextResolver resolver = new ContextResolverXml();
        providers.register(resolver);
        assertTrue(providers.isRegistered(ContextResolverXml.class));
        assertTrue(providers.isRegistered(resolver));
        assertSame(resolver, providers.getContextResolver(String.class, new MediaType("text", "xml")));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateContextResolvers() {
        providers.register(new ContextResolverXml());
        ContextResolver duplicateResolver = new ContextResolverXml();
        providers.register(duplicateResolver);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ContextResolver {}. ContextResolver for {} and media type {} already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateResolver, String.class, new MediaType("text", "xml")}));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateContextResolverClassAndInstance() {
        providers.register(new ContextResolverXml());
        providers.register(ContextResolverXml.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ContextResolver {}. ContextResolver for {} and media type {} already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{ContextResolverXml.class, String.class, new MediaType("text", "xml")}));
    }

    @Test
    public void registersReaderInterceptorClass() {
        ApplicationContext.setCurrent(context);
        providers.register(ReaderInterceptorImplOne.class);
        assertTrue(providers.isRegistered(ReaderInterceptorImplOne.class));
        List<ReaderInterceptor> interceptors = providers.getReaderInterceptors();
        assertEquals(1, interceptors.size());
        assertTrue(interceptors.get(0) instanceof ReaderInterceptorImplOne);
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateReaderInterceptorClasses() {
        providers.register(ReaderInterceptorImplOne.class);
        providers.register(ReaderInterceptorImplOne.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ReaderInterceptor {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Class[]{ReaderInterceptorImplOne.class}));
    }

    @Test
    public void readerInterceptorClassesOrderedByPriority() {
        ApplicationContext.setCurrent(context);
        providers.register(ReaderInterceptorImplTwo.class);
        providers.register(ReaderInterceptorImplOne.class);

        List<ReaderInterceptor> interceptors = providers.getReaderInterceptors();

        assertEquals(2, interceptors.size());
        assertTrue(interceptors.get(0) instanceof ReaderInterceptorImplOne);
        assertTrue(interceptors.get(1) instanceof ReaderInterceptorImplTwo);
    }

    @Test
    public void registersReaderInterceptor() {
        ReaderInterceptor interceptor = new ReaderInterceptorImplOne();
        providers.register(interceptor);
        assertTrue(providers.isRegistered(ReaderInterceptorImplOne.class));
        assertTrue(providers.isRegistered(interceptor));
        List<ReaderInterceptor> interceptors = providers.getReaderInterceptors();
        assertEquals(1, interceptors.size());
        assertSame(interceptor, interceptors.get(0));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateReaderInterceptors() {
        providers.register(new ReaderInterceptorImplOne());
        ReaderInterceptorImplOne duplicateInterceptor = new ReaderInterceptorImplOne();
        providers.register(duplicateInterceptor);
        assertEquals(1, providers.getReaderInterceptors().size());
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ReaderInterceptor {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateInterceptor}));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateReaderInterceptorClassAndInstance() {
        ApplicationContext.setCurrent(context);
        providers.register(ReaderInterceptorImplOne.class);
        ReaderInterceptorImplOne duplicateInterceptor = new ReaderInterceptorImplOne();
        providers.register(duplicateInterceptor);
        assertEquals(1, providers.getReaderInterceptors().size());
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ReaderInterceptor {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateInterceptor}));
    }

    @Test
    public void readerInterceptorsOrderedByPriority() {
        ReaderInterceptor interceptorOne = new ReaderInterceptorImplOne();
        ReaderInterceptor interceptorTwo = new ReaderInterceptorImplTwo();
        providers.register(interceptorOne);
        providers.register(interceptorTwo);

        List<ReaderInterceptor> interceptors = providers.getReaderInterceptors();

        assertEquals(2, interceptors.size());
        assertEquals(interceptorOne, interceptors.get(0));
        assertEquals(interceptorTwo, interceptors.get(1));
    }

    @Test
    public void registersWriterInterceptorClass() {
        ApplicationContext.setCurrent(context);
        providers.register(WriterInterceptorImplOne.class);
        assertTrue(providers.isRegistered(WriterInterceptorImplOne.class));
        List<WriterInterceptor> interceptors = providers.getWriterInterceptors();
        assertEquals(1, interceptors.size());
        assertTrue(interceptors.get(0) instanceof WriterInterceptorImplOne);
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateWriterInterceptorClasses() {
        providers.register(WriterInterceptorImplOne.class);
        providers.register(WriterInterceptorImplOne.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore WriterInterceptor {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Class[]{WriterInterceptorImplOne.class}));
    }

    @Test
    public void writerInterceptorClassesOrderedByPriority() {
        ApplicationContext.setCurrent(context);
        providers.register(WriterInterceptorImplTwo.class);
        providers.register(WriterInterceptorImplOne.class);

        List<WriterInterceptor> interceptors = providers.getWriterInterceptors();

        assertEquals(2, interceptors.size());
        assertTrue(interceptors.get(0) instanceof WriterInterceptorImplOne);
        assertTrue(interceptors.get(1) instanceof WriterInterceptorImplTwo);
    }

    @Test
    public void registersWriterInterceptor() {
        WriterInterceptor interceptor = new WriterInterceptorImplOne();
        providers.register(interceptor);
        assertTrue(providers.isRegistered(WriterInterceptorImplOne.class));
        assertTrue(providers.isRegistered(interceptor));
        List<WriterInterceptor> interceptors = providers.getWriterInterceptors();
        assertEquals(1, interceptors.size());
        assertSame(interceptor, interceptors.get(0));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateWriterInterceptors() {
        providers.register(new WriterInterceptorImplOne());
        WriterInterceptorImplOne duplicateInterceptor = new WriterInterceptorImplOne();
        providers.register(duplicateInterceptor);
        assertEquals(1, providers.getWriterInterceptors().size());
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore WriterInterceptor {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateInterceptor}));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateWriterInterceptorClassAndInstance() {
        ApplicationContext.setCurrent(context);
        providers.register(WriterInterceptorImplOne.class);
        WriterInterceptorImplOne duplicateInterceptor = new WriterInterceptorImplOne();
        providers.register(duplicateInterceptor);
        assertEquals(1, providers.getWriterInterceptors().size());
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore WriterInterceptor {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateInterceptor}));
    }

    @Test
    public void writerInterceptorsOrderedByPriority() {
        WriterInterceptor interceptorOne = new WriterInterceptorImplOne();
        WriterInterceptor interceptorTwo = new WriterInterceptorImplTwo();
        providers.register(interceptorOne);
        providers.register(interceptorTwo);

        List<WriterInterceptor> interceptors = providers.getWriterInterceptors();

        assertEquals(2, interceptors.size());
        assertEquals(interceptorOne, interceptors.get(0));
        assertEquals(interceptorTwo, interceptors.get(1));
    }

    @Test
    public void registersClientRequestFilterClass() {
        ApplicationContext.setCurrent(context);
        providers = new DefaultProviderBinder(RuntimeType.CLIENT, configurationProperties);
        providers.register(ClientRequestFilterImplOne.class);
        assertTrue(providers.isRegistered(ClientRequestFilterImplOne.class));
        List<ClientRequestFilter> filters = providers.getClientRequestFilters();
        assertEquals(1, filters.size());
        assertTrue(filters.get(0) instanceof ClientRequestFilterImplOne);
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateClientRequestFilterClasses() {
        providers = new DefaultProviderBinder(RuntimeType.CLIENT, configurationProperties);
        providers.register(ClientRequestFilterImplOne.class);
        providers.register(ClientRequestFilterImplOne.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ClientRequestFilter {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Class[]{ClientRequestFilterImplOne.class}));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateClientRequestFilterClassAndInstance() {
        ApplicationContext.setCurrent(context);
        providers = new DefaultProviderBinder(RuntimeType.CLIENT, configurationProperties);
        providers.register(ClientRequestFilterImplOne.class);
        ClientRequestFilterImplOne duplicateFilter = new ClientRequestFilterImplOne();
        providers.register(duplicateFilter);
        assertEquals(1, providers.getClientRequestFilters().size());
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ClientRequestFilter {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateFilter}));
    }

    @Test
    public void clientRequestFilterClassesOrderedByPriority() {
        ApplicationContext.setCurrent(context);
        providers = new DefaultProviderBinder(RuntimeType.CLIENT, configurationProperties);
        providers.register(ClientRequestFilterImplTwo.class);
        providers.register(ClientRequestFilterImplOne.class);

        List<ClientRequestFilter> filters = providers.getClientRequestFilters();

        assertEquals(2, filters.size());
        assertTrue(filters.get(0) instanceof ClientRequestFilterImplOne);
        assertTrue(filters.get(1) instanceof ClientRequestFilterImplTwo);
    }

    @Test
    public void registersClientRequestFilter() {
        providers = new DefaultProviderBinder(RuntimeType.CLIENT, configurationProperties);
        ClientRequestFilter filter = new ClientRequestFilterImplOne();
        providers.register(filter);
        assertTrue(providers.isRegistered(ClientRequestFilterImplOne.class));
        assertTrue(providers.isRegistered(filter));
        List<ClientRequestFilter> filters = providers.getClientRequestFilters();
        assertEquals(1, filters.size());
        assertSame(filter, filters.get(0));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateClientRequestFilters() {
        providers = new DefaultProviderBinder(RuntimeType.CLIENT, configurationProperties);
        providers.register(new ClientRequestFilterImplOne());
        ClientRequestFilter duplicateFilter = new ClientRequestFilterImplOne();
        providers.register(duplicateFilter);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ClientRequestFilter {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateFilter}));
    }

    @Test
    public void clientRequestFiltersOrderedByPriority() {
        providers = new DefaultProviderBinder(RuntimeType.CLIENT, configurationProperties);
        ClientRequestFilter filterOne = new ClientRequestFilterImplOne();
        ClientRequestFilter filterTwo = new ClientRequestFilterImplTwo();
        providers.register(filterTwo);
        providers.register(filterOne);

        List<ClientRequestFilter> filters = providers.getClientRequestFilters();

        assertEquals(2, filters.size());
        assertSame(filterOne, filters.get(0));
        assertSame(filterTwo, filters.get(1));
    }

    @Test
    public void registersClientResponseFilterClass() {
        ApplicationContext.setCurrent(context);
        providers = new DefaultProviderBinder(RuntimeType.CLIENT, configurationProperties);
        providers.register(ClientResponseFilterImplOne.class);
        assertTrue(providers.isRegistered(ClientResponseFilterImplOne.class));
        List<ClientResponseFilter> filters = providers.getClientResponseFilters();
        assertEquals(1, filters.size());
        assertTrue(filters.get(0) instanceof ClientResponseFilterImplOne);
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateClientResponseFilterClasses() {
        providers = new DefaultProviderBinder(RuntimeType.CLIENT, configurationProperties);
        providers.register(ClientResponseFilterImplOne.class);
        providers.register(ClientResponseFilterImplOne.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ClientResponseFilter {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Class[]{ClientResponseFilterImplOne.class}));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateClientResponseFilterClassAndInstance() {
        ApplicationContext.setCurrent(context);
        providers = new DefaultProviderBinder(RuntimeType.CLIENT, configurationProperties);
        providers.register(ClientResponseFilterImplOne.class);
        ClientResponseFilterImplOne duplicateFilter = new ClientResponseFilterImplOne();
        providers.register(duplicateFilter);
        assertEquals(1, providers.getClientResponseFilters().size());
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ClientResponseFilter {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateFilter}));
    }

    @Test
    public void clientResponseFilterClassesOrderedByPriority() {
        ApplicationContext.setCurrent(context);
        providers = new DefaultProviderBinder(RuntimeType.CLIENT, configurationProperties);
        providers.register(ClientResponseFilterImplTwo.class);
        providers.register(ClientResponseFilterImplOne.class);

        List<ClientResponseFilter> filters = providers.getClientResponseFilters();

        assertEquals(2, filters.size());
        assertTrue(filters.get(0) instanceof ClientResponseFilterImplTwo);
        assertTrue(filters.get(1) instanceof ClientResponseFilterImplOne);
    }

    @Test
    public void registersClientResponseFilter() {
        providers = new DefaultProviderBinder(RuntimeType.CLIENT, configurationProperties);
        ClientResponseFilter filter = new ClientResponseFilterImplOne();
        providers.register(filter);
        assertTrue(providers.isRegistered(ClientResponseFilterImplOne.class));
        assertTrue(providers.isRegistered(filter));
        List<ClientResponseFilter> filters = providers.getClientResponseFilters();
        assertEquals(1, filters.size());
        assertSame(filter, filters.get(0));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateClientResponseFilters() {
        providers = new DefaultProviderBinder(RuntimeType.CLIENT, configurationProperties);
        providers.register(new ClientResponseFilterImplOne());
        ClientResponseFilter duplicateFilter = new ClientResponseFilterImplOne();
        providers.register(duplicateFilter);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ClientResponseFilter {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateFilter}));
    }

    @Test
    public void clientResponseFiltersOrderedByPriority() {
        ApplicationContext.setCurrent(context);
        providers = new DefaultProviderBinder(RuntimeType.CLIENT, configurationProperties);
        ClientResponseFilter filterOne = new ClientResponseFilterImplOne();
        ClientResponseFilter filterTwo = new ClientResponseFilterImplTwo();
        providers.register(filterTwo);
        providers.register(filterOne);

        List<ClientResponseFilter> filters = providers.getClientResponseFilters();

        assertEquals(2, filters.size());
        assertSame(filterTwo, filters.get(0));
        assertSame(filterOne, filters.get(1));
    }

    @Test
    public void registersContainerRequestFilterClass() {
        ApplicationContext.setCurrent(context);
        providers.register(ContainerRequestFilterImplOne.class);
        assertTrue(providers.isRegistered(ContainerRequestFilterImplOne.class));
        List<ContainerRequestFilter> filters = providers.getContainerRequestFilters(new Annotation[0], false);
        assertEquals(1, filters.size());
        assertTrue(filters.get(0) instanceof ContainerRequestFilterImplOne);
    }

    @Test
    public void containerRequestFilterClassesOrderedByPriority() {
        ApplicationContext.setCurrent(context);
        providers.register(ContainerRequestFilterImplOne.class);
        providers.register(ContainerRequestFilterImplTwo.class);

        List<ContainerRequestFilter> filters = providers.getContainerRequestFilters(new Annotation[0], false);

        assertEquals(2, filters.size());
        assertTrue(filters.get(0) instanceof ContainerRequestFilterImplOne);
        assertTrue(filters.get(1) instanceof ContainerRequestFilterImplTwo);
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateContainerRequestFilterClasses() {
        providers.register(ContainerRequestFilterImplOne.class);
        providers.register(ContainerRequestFilterImplOne.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ContainerRequestFilter {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Class[]{ContainerRequestFilterImplOne.class}));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateContainerRequestFilterClassAndInstance() {
        ApplicationContext.setCurrent(context);
        providers.register(ContainerRequestFilterImplOne.class);
        ContainerRequestFilterImplOne duplicateFilter = new ContainerRequestFilterImplOne();
        providers.register(duplicateFilter);
        assertEquals(1, providers.getContainerRequestFilters(new Annotation[0], false).size());
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ContainerRequestFilter {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateFilter}));
    }

    @Test
    public void registersContainerRequestFilter() {
        ContainerRequestFilterImplOne filter = new ContainerRequestFilterImplOne();
        providers.register(filter);
        assertTrue(providers.isRegistered(ContainerRequestFilterImplOne.class));
        assertTrue(providers.isRegistered(filter));
        List<ContainerRequestFilter> filters = providers.getContainerRequestFilters(new Annotation[0], false);
        assertEquals(1, filters.size());
        assertSame(filter, filters.get(0));
    }

    @Test
    public void containerRequestFiltersOrderedByPriority() {
        ContainerRequestFilter filterOne = new ContainerRequestFilterImplOne();
        ContainerRequestFilter filterTwo = new ContainerRequestFilterImplTwo();
        providers.register(filterTwo);
        providers.register(filterOne);

        List<ContainerRequestFilter> filters = providers.getContainerRequestFilters(new Annotation[0], false);

        assertEquals(2, filters.size());
        assertSame(filterOne, filters.get(0));
        assertSame(filterTwo, filters.get(1));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateContainerRequestFilters() {
        providers.register(new ContainerRequestFilterImplOne());
        ContainerRequestFilter duplicateFilter = new ContainerRequestFilterImplOne();
        providers.register(duplicateFilter);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ContainerRequestFilter {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateFilter}));
    }

    @Test
    public void registersContainerResponseFilterClass() {
        ApplicationContext.setCurrent(context);
        providers.register(ContainerResponseFilterImplOne.class);
        assertTrue(providers.isRegistered(ContainerResponseFilterImplOne.class));
        List<ContainerResponseFilter> filters = providers.getContainerResponseFilters(new Annotation[0]);
        assertEquals(1, filters.size());
        assertTrue(filters.get(0) instanceof ContainerResponseFilterImplOne);
    }

    @Test
    public void containerResponseFilterClassesOrderedByPriority() {
        ApplicationContext.setCurrent(context);
        providers.register(ContainerResponseFilterImplOne.class);
        providers.register(ContainerResponseFilterImplTwo.class);

        List<ContainerResponseFilter> filters = providers.getContainerResponseFilters(new Annotation[0]);

        assertEquals(2, filters.size());
        assertTrue(filters.get(0) instanceof ContainerResponseFilterImplTwo);
        assertTrue(filters.get(1) instanceof ContainerResponseFilterImplOne);
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateContainerResponseFilterClasses() {
        ApplicationContext.setCurrent(context);
        providers.register(ContainerResponseFilterImplOne.class);
        providers.register(ContainerResponseFilterImplOne.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ContainerResponseFilter {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Class[]{ContainerResponseFilterImplOne.class}));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateContainerResponseFilterClassAndInstance() {
        ApplicationContext.setCurrent(context);
        providers.register(ContainerResponseFilterImplOne.class);
        ContainerResponseFilterImplOne duplicateFilter = new ContainerResponseFilterImplOne();
        providers.register(duplicateFilter);
        assertEquals(1, providers.getContainerResponseFilters(new Annotation[0]).size());
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ContainerResponseFilter {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateFilter}));
    }

    @Test
    public void retrievesPreMatchingContainerRequestFiltersRegardlessNamingAnnotation() {
        ApplicationContext.setCurrent(context);
        ContainerRequestFilterImplThree three = new ContainerRequestFilterImplThree();
        ContainerRequestFilterImplFour four = new ContainerRequestFilterImplFour();
        providers.register(three);
        providers.register(four);

        assertTrue(providers.isRegistered(ContainerRequestFilterImplThree.class));
        assertTrue(providers.isRegistered(ContainerRequestFilterImplFour.class));

        _Named named = ContainerRequestFilterImplFour.class.getAnnotation(_Named.class);
        List<ContainerRequestFilter> filters = providers.getContainerRequestFilters(new Annotation[]{named}, true);
        assertEquals(newHashSet(three, four), newHashSet(filters));
    }

    @Test
    public void retrievesOnlyNamingAnnotatedContainerRequestFiltersWhenNamingAnnotationsSpecified() {
        ApplicationContext.setCurrent(context);
        providers.register(ContainerRequestFilterImplTwo.class);
        providers.register(ContainerRequestFilterImplFive.class);

        assertTrue(providers.isRegistered(ContainerRequestFilterImplTwo.class));
        assertTrue(providers.isRegistered(ContainerRequestFilterImplFive.class));

        _Named named = ContainerRequestFilterImplFive.class.getAnnotation(_Named.class);
        List<ContainerRequestFilter> filters = providers.getContainerRequestFilters(new Annotation[]{named}, false);
        assertEquals(1, filters.size());
        assertTrue(filters.get(0) instanceof ContainerRequestFilterImplFive);
    }

    @Test
    public void retrievesOnlyNoneNamingAnnotatedContainerRequestFiltersWhenNamingAnnotationsNotSpecified() {
        ApplicationContext.setCurrent(context);
        providers.register(ContainerRequestFilterImplTwo.class);
        providers.register(ContainerRequestFilterImplFive.class);

        assertTrue(providers.isRegistered(ContainerRequestFilterImplTwo.class));
        assertTrue(providers.isRegistered(ContainerRequestFilterImplFive.class));

        List<ContainerRequestFilter> filters = providers.getContainerRequestFilters(new Annotation[0], false);
        assertEquals(1, filters.size());
        assertTrue(filters.get(0) instanceof ContainerRequestFilterImplTwo);
    }

    @Test
    public void registersContainerResponseFilter() {
        ContainerResponseFilterImplOne filter = new ContainerResponseFilterImplOne();
        providers.register(filter);
        assertTrue(providers.isRegistered(ContainerResponseFilterImplOne.class));
        assertTrue(providers.isRegistered(filter));
        List<ContainerResponseFilter> filters = providers.getContainerResponseFilters(new Annotation[0]);
        assertEquals(1, filters.size());
        assertSame(filter, filters.get(0));
    }

    @Test
    public void containerResponseFiltersOrderedByPriority() {
        ContainerResponseFilter filterOne = new ContainerResponseFilterImplOne();
        ContainerResponseFilter filterTwo = new ContainerResponseFilterImplTwo();
        providers.register(filterTwo);
        providers.register(filterOne);

        List<ContainerResponseFilter> filters = providers.getContainerResponseFilters(new Annotation[0]);

        assertEquals(2, filters.size());
        assertSame(filterTwo, filters.get(0));
        assertSame(filterOne, filters.get(1));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateContainerResponseFilters() {
        ApplicationContext.setCurrent(context);
        providers.register(new ContainerResponseFilterImplOne());
        ContainerResponseFilter duplicateFilter = new ContainerResponseFilterImplOne();
        providers.register(duplicateFilter);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ContainerResponseFilter {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateFilter}));
    }

    @Test
    public void registersParamConverterProviderClass() {
        ApplicationContext.setCurrent(context);
        providers.register(ParamConverterProviderImpl.class);
        assertTrue(providers.isRegistered(ParamConverterProviderImpl.class));
        ParamConverter converter = providers.getConverter(String.class, String.class, null);
        assertTrue(converter instanceof ParamConverterImpl);
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateParamConverterProviderClasses() {
        providers.register(ParamConverterProviderImpl.class);
        providers.register(ParamConverterProviderImpl.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ParamConverterProvider {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Class[]{ParamConverterProviderImpl.class}));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateParamConverterProviderClassAndInstance() {
        providers.register(ParamConverterProviderImpl.class);
        ParamConverterProviderImpl duplicateProvider = new ParamConverterProviderImpl();
        providers.register(duplicateProvider);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore ParamConverterProvider {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateProvider}));
    }

    @Test
    public void registersParamConverterProvider() {
        providers.register(new ParamConverterProviderImpl());
        assertTrue(providers.isRegistered(ParamConverterProviderImpl.class));
        ParamConverter converter = providers.getConverter(String.class, String.class, null);
        assertTrue(converter instanceof ParamConverterImpl);
    }

    @Test
    public void registersFeatureClass() {
        providers.register(FeatureImpl.class);
        assertTrue(providers.isRegistered(FeatureImpl.class));
        assertTrue(providers.isEnabled(FeatureImpl.class));
        assertTrue(providers.isRegistered(ContainerRequestFilterImplOne.class));
        assertTrue(providers.isRegistered(ContainerResponseFilterImplOne.class));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateFeatureClasses() {
        providers.register(FeatureImpl.class);
        providers.register(FeatureImpl.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore Feature {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Class[]{FeatureImpl.class}));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateFeatureClassAndInstance() {
        providers.register(FeatureImpl.class);
        Feature duplicateFeature = new FeatureImpl();
        providers.register(duplicateFeature);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore Feature {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateFeature}));
    }

    @Test
    public void registersFeature() {
        Feature feature = new FeatureImpl();
        providers.register(feature);
        assertTrue(providers.isRegistered(feature));
        assertTrue(providers.isRegistered(FeatureImpl.class));
        assertTrue(providers.isEnabled(feature));
        assertTrue(providers.isEnabled(FeatureImpl.class));
        assertTrue(providers.isRegistered(ContainerRequestFilterImplOne.class));
        assertTrue(providers.isRegistered(ContainerResponseFilterImplOne.class));
    }

    @Test
    public void registersDynamicFeatureClass() {
        ApplicationContext.setCurrent(context);
        providers.register(DynamicFeatureImpl.class);
        assertTrue(providers.isRegistered(DynamicFeatureImpl.class));
        List<DynamicFeature> dynamicFeatures = providers.getDynamicFeatures();
        assertEquals(1, dynamicFeatures.size());
        assertTrue(dynamicFeatures.get(0) instanceof DynamicFeatureImpl);
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateDynamicFeatureClasses() {
        providers.register(DynamicFeatureImpl.class);
        providers.register(DynamicFeatureImpl.class);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore DynamicFeature {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{DynamicFeatureImpl.class}));
    }

    @Test
    public void logsWarnWhenTryRegisterDuplicateDynamicFeatureClassAndInstance() {
        providers.register(DynamicFeatureImpl.class);
        DynamicFeature duplicateFeature = new DynamicFeatureImpl();
        providers.register(duplicateFeature);
        assertThatLoggingEventAppended(event -> event.getLevel() == Level.WARN
                && "Ignore DynamicFeature {}. The same component class already registered".equals(event.getMessage())
                && Arrays.equals(event.getArgumentArray(), new Object[]{duplicateFeature}));
    }

    @Test
    public void registersDynamicFeature() {
        DynamicFeature dynamicFeature = new DynamicFeatureImpl();
        providers.register(dynamicFeature);
        assertTrue(providers.isRegistered(dynamicFeature));
        assertTrue(providers.isRegistered(DynamicFeatureImpl.class));
        List<DynamicFeature> dynamicFeatures = providers.getDynamicFeatures();
        assertEquals(1, dynamicFeatures.size());
        assertSame(dynamicFeature, dynamicFeatures.get(0));
    }

    @Test
    public void findsMessageBodyReaderThatSupportsTypeNearestToDeserializedTypeInClassesHierarchy() {
        EntityReader entityReader = new EntityReader();
        ExtendedEntityReader extendedEntityReader = new ExtendedEntityReader();
        ExtendedExtendedEntityReader extendedExtendedEntityReader = new ExtendedExtendedEntityReader();

        providers.register(entityReader);
        providers.register(extendedEntityReader);

        assertSame(entityReader, providers.getMessageBodyReader(Entity.class, null, null, TEXT_PLAIN_TYPE));
        assertSame(extendedEntityReader, providers.getMessageBodyReader(ExtendedEntity.class, null, null, TEXT_PLAIN_TYPE));
        assertSame(extendedEntityReader, providers.getMessageBodyReader(ExtendedExtendedEntity.class, null, null, TEXT_PLAIN_TYPE));

        providers.register(extendedExtendedEntityReader);

        assertSame(entityReader, providers.getMessageBodyReader(Entity.class, null, null, TEXT_PLAIN_TYPE));
        assertSame(extendedEntityReader, providers.getMessageBodyReader(ExtendedEntity.class, null, null, TEXT_PLAIN_TYPE));
        assertSame(extendedExtendedEntityReader, providers.getMessageBodyReader(ExtendedExtendedEntity.class, null, null, TEXT_PLAIN_TYPE));
    }

    @Test
    public void findsMessageBodyWriterThatSupportsTypeNearestToSerializedTypeInClassesHierarchy() {
        EntityWriter entityWriter = new EntityWriter();
        ExtendedEntityWriter extendedEntityWriter = new ExtendedEntityWriter();
        ExtendedExtendedEntityWriter extendedExtendedEntityWriter = new ExtendedExtendedEntityWriter();

        providers.register(entityWriter);
        providers.register(extendedEntityWriter);

        assertSame(entityWriter, providers.getMessageBodyWriter(Entity.class, null, null, TEXT_PLAIN_TYPE));
        assertSame(extendedEntityWriter, providers.getMessageBodyWriter(ExtendedEntity.class, null, null, TEXT_PLAIN_TYPE));
        assertSame(extendedEntityWriter, providers.getMessageBodyWriter(ExtendedExtendedEntity.class, null, null, TEXT_PLAIN_TYPE));

        providers.register(extendedExtendedEntityWriter);

        assertSame(entityWriter, providers.getMessageBodyWriter(Entity.class, null, null, TEXT_PLAIN_TYPE));
        assertSame(extendedEntityWriter, providers.getMessageBodyWriter(ExtendedEntity.class, null, null, TEXT_PLAIN_TYPE));
        assertSame(extendedExtendedEntityWriter, providers.getMessageBodyWriter(ExtendedExtendedEntity.class, null, null, TEXT_PLAIN_TYPE));
    }
    
    static class Entity {
    }

    static class ExtendedEntity extends Entity {
    }

    static class ExtendedExtendedEntity extends ExtendedEntity {
    }

    @Provider
    static class EntityReader implements MessageBodyReader<Entity> {
        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return Entity.class.isAssignableFrom(type);
        }

        @Override
        public Entity readFrom(Class<Entity> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
            return null;
        }
    }

    @Provider
    static class ExtendedEntityReader implements MessageBodyReader<ExtendedEntity> {
        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return Entity.class.isAssignableFrom(type);
        }

        @Override
        public ExtendedEntity readFrom(Class<ExtendedEntity> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                                       MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
            return null;
        }
    }

    @Provider
    static class ExtendedExtendedEntityReader implements MessageBodyReader<ExtendedExtendedEntity> {
        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return Entity.class.isAssignableFrom(type);
        }

        @Override
        public ExtendedExtendedEntity readFrom(Class<ExtendedExtendedEntity> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                                               MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
            return null;
        }
    }

    @Provider
    static class EntityWriter implements MessageBodyWriter<Entity> {
        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return Entity.class.isAssignableFrom(type);
        }

        @Override
        public long getSize(Entity entity, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(Entity entity, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        }
    }

    @Provider
    static class ExtendedEntityWriter implements MessageBodyWriter<ExtendedEntity> {
        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return Entity.class.isAssignableFrom(type);
        }

        @Override
        public long getSize(ExtendedEntity extendedEntity, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(ExtendedEntity extendedEntity, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        }
    }

    @Provider static class ExtendedExtendedEntityWriter implements MessageBodyWriter<ExtendedExtendedEntity> {
         @Override
         public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                        return Entity.class.isAssignableFrom(type);
                    }
 
                 @Override
         public long getSize(ExtendedExtendedEntity extendedExtendedEntity, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                        return -1;
                    }
 
                 @Override
         public void writeTo(ExtendedExtendedEntity extendedExtendedEntity, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                             MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
                    }
     }
     
    private void assertThatLoggingEventAppended(Predicate<ILoggingEvent> loggingEventPredicate) {
        if (!retrieveLoggingEvents().stream().anyMatch(loggingEventPredicate)) {
            fail("Expected logging event was not appended");
        }
    }

    private List<ILoggingEvent> retrieveLoggingEvents() {
        ArgumentCaptor<ILoggingEvent> logEventCaptor = ArgumentCaptor.forClass(ILoggingEvent.class);
        verify(mockLogbackAppender, atLeastOnce()).doAppend(logEventCaptor.capture());
        return logEventCaptor.getAllValues();
    }
}