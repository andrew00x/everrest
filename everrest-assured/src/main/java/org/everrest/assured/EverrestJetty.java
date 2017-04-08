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
package org.everrest.assured;

import com.google.common.base.Throwables;
import com.jayway.restassured.RestAssured;
import org.everrest.core.impl.EverrestApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.Listeners;

import javax.servlet.Filter;
import java.lang.reflect.Field;

import static org.everrest.core.impl.RestComponentResolver.isRootResourceOrProvider;


public class EverrestJetty implements ITestListener, IInvokedMethodListener {
    private static final Logger LOG = LoggerFactory.getLogger(EverrestJetty.class);

    public final static String JETTY_PORT = "jetty-port";
    public final static String JETTY_SERVER = "jetty-server";

    private JettyHttpServer httpServer;
    private TestObjectFactoryProducer testedComponentFactoryProducer = new TestObjectFactoryProducer();

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (httpServer != null && hasEverrestJettyListener(method.getTestMethod().getInstance().getClass())) {
            httpServer.resetComponentBindings();
            httpServer.resetFilter();
        }
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        if (httpServer != null && hasEverrestJettyListener(method.getTestMethod().getInstance().getClass())) {
            httpServer.resetComponentBindings();
            httpServer.resetFilter();
            initRestComponents(method.getTestMethod());
        }
    }

    public void onFinish(ITestContext context) {
        JettyHttpServer httpServer = (JettyHttpServer) context.getAttribute(JETTY_SERVER);
        if (httpServer != null) {
            try {
                httpServer.stop();
            } catch (Exception e) {
                LOG.error(e.getLocalizedMessage(), e);
                throw Throwables.propagate(e);
            }
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
    }

    @Override
    public void onTestSuccess(ITestResult result) {
    }

    @Override
    public void onTestFailure(ITestResult result) {
    }

    @Override
    public void onTestSkipped(ITestResult result) {
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
    }

    public void onStart(ITestContext context) {
        ITestNGMethod[] allTestMethods = context.getAllTestMethods();
        if (allTestMethods == null) {
            return;
        }
        if (httpServer == null && hasEverrestJettyListenerTestHierarchy(allTestMethods)) {
            httpServer = new JettyHttpServer();

            context.setAttribute(JETTY_PORT, httpServer.getPort());
            context.setAttribute(JETTY_SERVER, httpServer);

            try {
                httpServer.start();
                httpServer.resetComponentBindings();
                httpServer.resetFilter();
                httpServer.getProviderBinder().setObjectFactoryProducer(testedComponentFactoryProducer);
                httpServer.getResourceBinder().setObjectFactoryProducer(testedComponentFactoryProducer);
                RestAssured.port = httpServer.getPort();
                RestAssured.basePath = JettyHttpServer.UNSECURE_REST;
            } catch (Exception e) {
                LOG.error(e.getLocalizedMessage(), e);
                throw Throwables.propagate(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void initRestComponents(ITestNGMethod... testMethods) {
        for (ITestNGMethod testMethod : testMethods) {
            Object test = testMethod.getInstance();

            if (hasEverrestJettyListenerTestHierarchy(test.getClass())) {
                EverrestApplication everrest = new EverrestApplication();
                Field[] fields = test.getClass().getDeclaredFields();
                for (Field field : fields) {
                    try {
                        field.setAccessible(true);
                        if (isRootResourceOrProvider(field.getType())) {
                            testedComponentFactoryProducer.registerRestComponent(test, field);
                            everrest.addClass(field.getType());
                        } else if (Filter.class.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);
                            Object fieldInstance = field.get(test);
                            if (fieldInstance != null) {
                                httpServer.addFilter(((Filter) fieldInstance), "/*");
                            } else {
                                httpServer.addFilter((Class<? extends Filter>) field.getType(), "/*");
                            }
                        }
                    } catch (IllegalAccessException e) {
                        LOG.error(e.getLocalizedMessage(), e);
                    }
                }
                httpServer.publish(everrest);
            }
        }
    }

    private boolean hasEverrestJettyListener(Class<?> clazz) {
        Listeners listeners = clazz.getAnnotation(Listeners.class);
        if (listeners == null) {
            return false;
        }

        for (Class<? extends ITestNGListener> listenerClass : listeners.value()) {
            if (EverrestJetty.class.isAssignableFrom(listenerClass)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEverrestJettyListenerTestHierarchy(Class<?> testClass) {
        for (Class<?> clazz = testClass; clazz != Object.class; clazz = clazz.getSuperclass()) {
            if (hasEverrestJettyListener(clazz)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEverrestJettyListenerTestHierarchy(ITestNGMethod... testMethods) {
        for (ITestNGMethod testMethod : testMethods) {
            Object instance = testMethod.getInstance();
            if (hasEverrestJettyListenerTestHierarchy(instance.getClass())) {
                return true;
            }
        }
        return false;
    }
}
