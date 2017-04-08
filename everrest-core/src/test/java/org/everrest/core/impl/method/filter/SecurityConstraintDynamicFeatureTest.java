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
package org.everrest.core.impl.method.filter;

import org.junit.Before;
import org.junit.Test;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SecurityConstraintDynamicFeatureTest {
    private SecurityConstraintDynamicFeature feature;

    @Before
    public void setUp() throws Exception {
        feature = new SecurityConstraintDynamicFeature();
    }

    @Test
    public void processesSecurityAnnotationFromMethod() throws Exception {
        Class<?> aClass = ResourceWithMethodWithSecurityAnnotation.class;
        Method method = aClass.getMethod("m1");
        Annotation securityAnnotation = feature.getSecurityAnnotation(aClass, method);

        assertEquals(method.getAnnotation(RolesAllowed.class), securityAnnotation);
    }

    @Path("a")
    public static class ResourceWithMethodWithSecurityAnnotation {
        @RolesAllowed("user")
        @GET public void m1() {}
    }

    @Test
    public void processesSecurityAnnotationFromClass() throws Exception {
        Class<?> aClass = ResourceWithSecurityAnnotation.class;
        Annotation securityAnnotation = feature.getSecurityAnnotation(aClass, aClass.getMethod("m1"));

        assertEquals(aClass.getAnnotation(RolesAllowed.class), securityAnnotation);
    }

    @RolesAllowed("user")
    @Path("a")
    public static class ResourceWithSecurityAnnotation {
        @GET public void m1() {}
    }

    @Test
    public void ignoresSecurityAnnotationFromClassWhenMethodHasOwn() throws Exception {
        Class<?> aClass = ResourceWithSecurityAnnotationOnClassAndMethod.class;
        Method method = aClass.getMethod("m1");
        Annotation securityAnnotation = feature.getSecurityAnnotation(aClass, method);

        assertEquals(method.getAnnotation(PermitAll.class), securityAnnotation);
    }

    @RolesAllowed("user")
    @Path("a")
    public static class ResourceWithSecurityAnnotationOnClassAndMethod {
        @PermitAll
        @GET public void m1() {}
    }

    @Test
    public void inheritsSecurityAnnotationFromMethodOnParentInterface() throws Exception {
        Class<?> anInterface = InterfaceWithSecurityAnnotationOnMethod.class;
        Method interfaceMethod = anInterface.getMethod("m1");
        Class<?> aClass = ResourceWithSecurityAnnotationOnMethodInParentInterface.class;
        Method classMethod = aClass.getMethod("m1");
        Annotation securityAnnotation = feature.getSecurityAnnotation(aClass, classMethod);

        assertEquals(interfaceMethod.getAnnotation(RolesAllowed.class), securityAnnotation);
    }

    public interface InterfaceWithSecurityAnnotationOnMethod {
        @RolesAllowed("user") void m1();
    }

    @Path("a")
    public static class ResourceWithSecurityAnnotationOnMethodInParentInterface implements InterfaceWithSecurityAnnotationOnMethod {
        @GET public void m1() {}
    }

    @Test
    public void inheritsSecurityAnnotationFromParentInterface() throws Exception {
        Class<?> anInterface = InterfaceWithSecurityAnnotation.class;
        Class<?> aClass = ResourceWithSecurityAnnotationOnParentInterface.class;
        Annotation securityAnnotation = feature.getSecurityAnnotation(aClass, aClass.getMethod("m1"));

        assertEquals(anInterface.getAnnotation(RolesAllowed.class), securityAnnotation);
    }

    @RolesAllowed("user")
    public interface InterfaceWithSecurityAnnotation {
        void m1();
    }

    @Path("a")
    public static class ResourceWithSecurityAnnotationOnParentInterface implements InterfaceWithSecurityAnnotation {
        @GET public void m1() {}
    }

    @Test
    public void inheritsSecurityAnnotationFromMethodOnParentClass() throws Exception {
        Class<?> superClass = ClassWithSecurityAnnotationOnMethod.class;
        Method superClassMethod = superClass.getMethod("m1");
        Class<?> aClass = ResourceWithSecurityAnnotationOnMethodInParentClass.class;
        Method classMethod = aClass.getMethod("m1");
        Annotation securityAnnotation = feature.getSecurityAnnotation(aClass, classMethod);

        assertEquals(superClassMethod.getAnnotation(RolesAllowed.class), securityAnnotation);
    }

    public static abstract class ClassWithSecurityAnnotationOnMethod {
        @RolesAllowed("user") public abstract void m1();
    }

    @Path("a")
    public static class ResourceWithSecurityAnnotationOnMethodInParentClass extends ClassWithSecurityAnnotationOnMethod {
        @GET public void m1() {}
    }

    @Test
    public void inheritsSecurityAnnotationFromParentClass() throws Exception {
        Class<?> superClass = ClassWithSecurityAnnotation.class;
        Class<?> aClass = ResourceWithSecurityAnnotationOnParentClass.class;
        Annotation securityAnnotation = feature.getSecurityAnnotation(aClass, aClass.getMethod("m1"));

        assertEquals(superClass.getAnnotation(RolesAllowed.class), securityAnnotation);
    }

    @RolesAllowed("user")
    public static abstract class ClassWithSecurityAnnotation {
        public abstract void m1();
    }

    @Path("a")
    public static class ResourceWithSecurityAnnotationOnParentClass extends ClassWithSecurityAnnotation {
        @GET public void m1() {}
    }

    @Test
    public void registersSecurityConstraintFilterIfClassOrMethodHasSecurityAnnotations() throws Exception {
        Class aClass = ResourceWithMethodWithSecurityAnnotation.class;
        Method method = aClass.getMethod("m1");

        ResourceInfo resourceInfo = mock(ResourceInfo.class);
        when(resourceInfo.getResourceClass()).thenReturn(aClass);
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        FeatureContext context = mock(FeatureContext.class);

        feature.configure(resourceInfo, context);

        verify(context).register(isA(SecurityConstraint.class));
    }

    @Test
    public void doesNotRegisterSecurityConstraintFilterIfNeitherClassNorMethodHasSecurityAnnotations() throws Exception {
        Class aClass = Resource.class;
        Method method = aClass.getMethod("m1");

        ResourceInfo resourceInfo = mock(ResourceInfo.class);
        when(resourceInfo.getResourceClass()).thenReturn(aClass);
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        FeatureContext context = mock(FeatureContext.class);

        feature.configure(resourceInfo, context);

        verify(context, never()).register(isA(SecurityConstraint.class));
    }

    @Path("a")
    public static class Resource {
        @GET public void m1() {}
    }
}