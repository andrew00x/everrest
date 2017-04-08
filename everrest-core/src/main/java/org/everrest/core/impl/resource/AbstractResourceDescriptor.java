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
package org.everrest.core.impl.resource;

import com.google.common.base.MoreObjects;
import org.everrest.core.BaseObjectModel;
import org.everrest.core.Parameter;
import org.everrest.core.impl.method.MethodParameter;
import org.everrest.core.resource.ResourceDescriptor;
import org.everrest.core.resource.ResourceMethodDescriptor;
import org.everrest.core.resource.SubResourceLocatorDescriptor;
import org.everrest.core.resource.SubResourceMethodDescriptor;
import org.everrest.core.uri.UriPattern;
import org.everrest.core.util.ResourceMethodComparator;
import org.everrest.core.util.UriPatternComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkState;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static org.everrest.core.impl.header.MediaTypeHelper.WADL_TYPE;
import static org.everrest.core.impl.header.MediaTypeHelper.createConsumesList;
import static org.everrest.core.impl.header.MediaTypeHelper.createProducesList;
import static org.everrest.core.impl.method.ParameterHelper.RESOURCE_METHOD_PARAMETER_ANNOTATIONS;
import static org.everrest.core.util.ReflectionUtils.findAnnotationsAnnotatedWith;
import static org.everrest.core.util.ReflectionUtils.findFirstAnnotationAnnotatedWith;

/**
 * @author andrew00x
 */
public class AbstractResourceDescriptor extends BaseObjectModel implements ResourceDescriptor {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractResourceDescriptor.class);

    private static Comparator<UriPattern> uriPatternComparator = new UriPatternComparator();

    /** PathValue. */
    private final PathValue path;
    /** UriPattern. */
    private final UriPattern uriPattern;
    /** Sub-resource methods. Sub-resource method has path annotation. */
    private final TreeMap<UriPattern, Map<String, List<SubResourceMethodDescriptor>>> subResourceMethods;
    /** Sub-resource locators. Sub-resource locator has path annotation. */
    private final TreeMap<UriPattern, SubResourceLocatorDescriptor> subResourceLocators;
    /** Resource methods. Resource method has not own path annotation. */
    private final MultivaluedMap<String, ResourceMethodDescriptor> resourceMethods;
    private final ResourceMethodComparator resourceMethodComparator = new ResourceMethodComparator();

    /**
     * Constructs new instance of AbstractResourceDescriptor.
     *
     * @param resourceClass resource class
     */
    public AbstractResourceDescriptor(Class<?> resourceClass) {
        this(PathValue.getPath(resourceClass.getAnnotation(Path.class)), resourceClass);
    }

    /**
     * Constructs new instance of AbstractResourceDescriptor.
     *
     * @param path          resource path
     * @param resourceClass resource class
     */
    public AbstractResourceDescriptor(String path, Class<?> resourceClass) {
        this(path, resourceClass, null);
    }

    /**
     * Constructs new instance of AbstractResourceDescriptor.
     *
     * @param path                              resource path
     * @param resourceClass                     resource class
     * @param applicationNameBindingAnnotations name binding annotations (see {@link javax.ws.rs.NameBinding}) that are applied to {@link javax.ws.rs.core.Application} subclass.
     *                                          If this resource deployed with different mechanism this parameter might be {@code null} or empty array.
     */
    public AbstractResourceDescriptor(String path, Class<?> resourceClass, Annotation[] applicationNameBindingAnnotations) {
        super(resourceClass, applicationNameBindingAnnotations);
        if (path == null) {
            this.path = null;
            this.uriPattern = null;
        } else {
            this.path = new PathValue(path);
            this.uriPattern = new UriPattern(path);
        }
        this.resourceMethods = new MultivaluedHashMap<>();
        this.subResourceMethods = new TreeMap<>(uriPatternComparator);
        this.subResourceLocators = new TreeMap<>(uriPatternComparator);
        processMethods();
    }

    /**
     * Constructs new instance of AbstractResourceDescriptor.
     *
     * @param resource resource
     */
    public AbstractResourceDescriptor(Object resource) {
        this(resource.getClass());
    }

    /**
     * Constructs new instance of AbstractResourceDescriptor.
     *
     * @param path     resource path
     * @param resource resource
     */
    public AbstractResourceDescriptor(String path, Object resource) {
        this(path, resource.getClass());
    }

    /**
     * Constructs new instance of AbstractResourceDescriptor.
     *
     * @param path                              resource path
     * @param resource                          resource
     * @param applicationNameBindingAnnotations name binding annotations (see {@link javax.ws.rs.NameBinding}) that are applied to {@link javax.ws.rs.core.Application} subclass.
     *                                          If this resource deployed with different mechanism this parameter might be {@code null} or empty array.
     */
    public AbstractResourceDescriptor(String path, Object resource, Annotation[] applicationNameBindingAnnotations) {
        this(path, resource.getClass(), applicationNameBindingAnnotations);
    }

    @Override
    public PathValue getPathValue() {
        return path;
    }

    @Override
    public Map<String, List<ResourceMethodDescriptor>> getResourceMethods() {
        return resourceMethods;
    }

    @Override
    public Map<UriPattern, SubResourceLocatorDescriptor> getSubResourceLocators() {
        return subResourceLocators;
    }

    @Override
    public Map<UriPattern, Map<String, List<SubResourceMethodDescriptor>>> getSubResourceMethods() {
        return subResourceMethods;
    }

    @Override
    public UriPattern getUriPattern() {
        return uriPattern;
    }

    @Override
    public boolean isRootResource() {
        return path != null;
    }

    /**
     * Process method of resource and separate them to three types Resource Methods, Sub-Resource Methods and
     * Sub-Resource Locators.
     */
    private void processMethods() {
        for (Method method : getAllMethods(getObjectClass())) {
            Path subPath = getMethodAnnotation(method, Path.class, false);
            HttpMethod httpMethod = getMethodAnnotation(method, HttpMethod.class, true);

            if (subPath != null || httpMethod != null) {
                if (Modifier.isPublic(method.getModifiers())) {
                    List<Parameter> methodParameters = createMethodParameters(method);
                    if (httpMethod != null) {
                        List<MediaType> produces = getProducesMediaTypes(method);
                        List<MediaType> consumes = getConsumesMediaTypes(method);
                        Annotation[] nameBindingAnnotations = getNameBindingAnnotations(method);
                        if (subPath == null) {
                            addResourceMethod(method, httpMethod, methodParameters, nameBindingAnnotations, produces, consumes);
                        } else {
                            addSubResourceMethod(method, subPath, httpMethod, methodParameters, nameBindingAnnotations, produces, consumes);
                        }
                    } else {
                        addSubResourceLocator(method, subPath, methodParameters);
                    }
                } else {
                    LOG.warn("Non-public method {} in {} annotated with @Path of HTTP method annotation, it's ignored", method.getName(), clazz.getName());
                }
            }
        }
        if (resourceMethods.isEmpty() && subResourceMethods.isEmpty() && subResourceLocators.isEmpty()) {
            LOG.warn("Not found any resource methods, sub-resource methods or sub-resource locators in {}", getObjectClass().getName());
        }

        resolveHeadRequest();
        resolveOptionsRequest();

        sortResourceMethods();
        sortSubResourceMethods();
    }

    private Annotation[] getNameBindingAnnotations(Method method) {
        Annotation[] methodBindingAnnotations = findAnnotationsAnnotatedWith(method, NameBinding.class);
        Annotation[] classBindingAnnotations = getNameBindingAnnotations();
        Set<Annotation> mergedBindingAnnotations = new HashSet<>();
        Collections.addAll(mergedBindingAnnotations, methodBindingAnnotations);
        Collections.addAll(mergedBindingAnnotations, classBindingAnnotations);
        return mergedBindingAnnotations.toArray(new Annotation[mergedBindingAnnotations.size()]);
    }

    private List<MediaType> getConsumesMediaTypes(Method method) {
        Consumes consumesAnnotation = getMethodAnnotation(method, Consumes.class, false);
        if (consumesAnnotation == null) {
            consumesAnnotation = getClassAnnotation(getObjectClass(), Consumes.class);
        }
        return createConsumesList(consumesAnnotation);
    }

    private List<MediaType> getProducesMediaTypes(Method method) {
        Produces producesAnnotation = getMethodAnnotation(method, Produces.class, false);
        if (producesAnnotation == null) {
            producesAnnotation = getClassAnnotation(getObjectClass(), Produces.class);
        }
        return createProducesList(producesAnnotation);
    }

    private List<Method> getAllMethods(Class<?> resourceClass) {
        List<Method> methods = new ArrayList<>();
        Collections.addAll(methods, resourceClass.getDeclaredMethods());

        List<Method> inheritedMethods = new ArrayList<>();
        Class<?> superclass = resourceClass.getSuperclass();
        while (superclass != null && superclass != Object.class) {
            Collections.addAll(inheritedMethods, superclass.getDeclaredMethods());
            superclass = superclass.getSuperclass();
        }

        for (Method method : methods) {
            for (Iterator<Method> iterator = inheritedMethods.iterator(); iterator.hasNext(); ) {
                Method inheritedMethod = iterator.next();
                if (Objects.equals(method.getName(), inheritedMethod.getName())
                    && method.getReturnType() == inheritedMethod.getReturnType() &&
                    Arrays.equals(method.getParameterTypes(), inheritedMethod.getParameterTypes())) {
                    iterator.remove();
                }
            }
        }

        methods.addAll(inheritedMethods);

        return methods;
    }

    private void addResourceMethod(Method method, HttpMethod httpMethod, List<Parameter> params, Annotation[] nameBindingAnnotations, List<MediaType> produces, List<MediaType> consumes) {
        ResourceMethodDescriptor resourceMethod = new ResourceMethodDescriptorImpl(method, httpMethod.value(), params, this, consumes, produces, nameBindingAnnotations);
        validateResourceMethod(resourceMethod);
        ResourceMethodDescriptor existedResourceMethod = findResourceMethodWithMediaTypes(getResourceMethods(httpMethod.value()), resourceMethod.consumes(), resourceMethod.produces());
        checkState(existedResourceMethod == null, "Two resource method %s and %s with the same HTTP method, consumes and produces found", resourceMethod, existedResourceMethod);
        resourceMethods.add(httpMethod.value(), resourceMethod);
    }

    private void addSubResourceMethod(Method method, Path subPath, HttpMethod httpMethod, List<Parameter> params, Annotation[] nameBindingAnnotations, List<MediaType> produces, List<MediaType> consumes) {
        SubResourceMethodDescriptor subResourceMethod = new SubResourceMethodDescriptorImpl(new PathValue(subPath.value()), method, httpMethod.value(), params, this, consumes, produces, nameBindingAnnotations);
        validateResourceMethod(subResourceMethod);

        Map<String, List<SubResourceMethodDescriptor>> subResourceMethods = getSubResourceMethods(subResourceMethod.getUriPattern());

        SubResourceMethodDescriptor existedSubResourceMethod = (SubResourceMethodDescriptor) findResourceMethodWithMediaTypes(subResourceMethods.get(httpMethod.value()), subResourceMethod.consumes(), subResourceMethod.produces());
        checkState(existedSubResourceMethod == null, "Two sub-resource method %s and %s with the same HTTP method, path, consumes and produces found", subResourceMethod, existedSubResourceMethod);
        List<SubResourceMethodDescriptor> methodList = subResourceMethods.get(httpMethod.value());
        if (methodList == null) {
            methodList = new ArrayList<>();
            subResourceMethods.put(httpMethod.value(), methodList);
        }
        methodList.add(subResourceMethod);
    }

    private void validateResourceMethod(ResourceMethodDescriptor resourceMethod) {
        List<Parameter> methodParameters = resourceMethod.getMethodParameters();
        int numberOfEntityParameters = (int)methodParameters.stream().filter(parameter -> parameter.getAnnotation() == null).count();
        if (numberOfEntityParameters > 1) {
            throw new RuntimeException(String.format("Method %s has %d parameters that are not annotated with JAX-RS parameter annotations, but must not have more than one",
                                                     resourceMethod.getMethod().getName(), numberOfEntityParameters));
        }
        boolean isAnyParameterAnnotatedWithFormParam = methodParameters.stream().anyMatch(parameter -> parameter.getAnnotation() != null &&
                                                                                          parameter.getAnnotation().annotationType() == FormParam.class);
        if (isAnyParameterAnnotatedWithFormParam && numberOfEntityParameters == 1) {
            boolean entityParameterIsMultivaluedMap = false;
            Parameter entityParameter = methodParameters.stream().filter(parameter -> parameter.getAnnotation() == null).findFirst().get();
            if (entityParameter.getParameterClass() == MultivaluedMap.class && entityParameter.getGenericType() instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType)entityParameter.getGenericType()).getActualTypeArguments();
                if (actualTypeArguments.length == 2 && String.class == actualTypeArguments[0] && String.class == actualTypeArguments[1]) {
                    entityParameterIsMultivaluedMap = true;
                }
            }
            if (!entityParameterIsMultivaluedMap) {
                throw new RuntimeException("At least one method's parameter is annotated with FormParam, entity parameter might not be other than MultivaluedMap<String, String>");
            }
        }
    }

    private void addSubResourceLocator(Method method, Path subPath, List<Parameter> params) {
        SubResourceLocatorDescriptor resourceLocator = new SubResourceLocatorDescriptorImpl(new PathValue(subPath.value()), method, params, this);
        validateSubResourceLocator(resourceLocator);
        checkState(!subResourceLocators.containsKey(resourceLocator.getUriPattern()), "Two sub-resource locators %s and %s with the same path found", resourceLocator, subResourceLocators.get(resourceLocator.getUriPattern()));
        subResourceLocators.put(resourceLocator.getUriPattern(), resourceLocator);
    }

    private void validateSubResourceLocator(SubResourceLocatorDescriptor resourceLocator) {
        List<Parameter> methodParameters = resourceLocator.getMethodParameters();
        boolean hasEntityParameter = methodParameters.stream().anyMatch(parameter -> parameter.getAnnotation() == null);
        if (hasEntityParameter) {
            throw new RuntimeException(String.format("Method %s is resource locator, it must not have not JAX-RS annotated (entity) parameters",
                                                     resourceLocator.getMethod().getName()));
        }
    }

    private void sortResourceMethods() {
        for (List<ResourceMethodDescriptor> resourceMethods : this.resourceMethods.values()) {
            Collections.sort(resourceMethods, resourceMethodComparator);
        }
    }

    private List<ResourceMethodDescriptor> getResourceMethods(String httpMethod) {
        List<ResourceMethodDescriptor> methodDescriptors = resourceMethods.get(httpMethod);
        if (methodDescriptors == null) {
            methodDescriptors = new ArrayList<>();
            resourceMethods.put(httpMethod, methodDescriptors);
        }
        return methodDescriptors;
    }

    private Map<String, List<SubResourceMethodDescriptor>> getSubResourceMethods(UriPattern subResourceUriPattern) {
        Map<String, List<SubResourceMethodDescriptor>> map = subResourceMethods.get(subResourceUriPattern);
        if (map == null) {
            map = new MultivaluedHashMap<>();
            subResourceMethods.put(subResourceUriPattern, map);
        }
        return map;
    }

    private void sortSubResourceMethods() {
        for (Map<String, List<SubResourceMethodDescriptor>> subResourceMethods : this.subResourceMethods.values()) {
            for (List<SubResourceMethodDescriptor> resourceMethods : subResourceMethods.values()) {
                Collections.sort(resourceMethods, resourceMethodComparator);
            }
        }
    }

    private List<Parameter> createMethodParameters(Method method) {
        Class<?>[] parameterClasses = method.getParameterTypes();
        if (parameterClasses.length > 0) {
            Type[] parameterGenTypes = method.getGenericParameterTypes();
            Annotation[][] annotations = method.getParameterAnnotations();

            List<Parameter> methodParameters = new ArrayList<>(parameterClasses.length);
            boolean classEncoded = getClassAnnotation(getObjectClass(), Encoded.class) != null;
            boolean methodEncoded = getMethodAnnotation(method, Encoded.class, false) != null;
            for (int i = 0; i < parameterClasses.length; i++) {
                String defaultValue = null;
                Annotation parameterAnnotation = null;
                boolean encoded = false;

                for (int j = 0; j < annotations[i].length; j++) {
                    Annotation annotation = annotations[i][j];
                    Class<?> annotationType = annotation.annotationType();
                    if (RESOURCE_METHOD_PARAMETER_ANNOTATIONS.contains(annotationType.getName())) {
                        if (parameterAnnotation != null) {
                            String msg = String.format("JAX-RS annotations on one of method parameters of resource %s, method %s are equivocality. Annotations: %s and %s can't be applied to one parameter",
                                                       this.toString(), method.getName(), parameterAnnotation, annotation);
                            throw new RuntimeException(msg);
                        }
                        parameterAnnotation = annotation;
                    } else if (annotationType == Encoded.class) {
                        encoded = true;
                    } else if (annotationType == DefaultValue.class) {
                        defaultValue = ((DefaultValue)annotation).value();
                    }
                }

                Parameter methodParameter = new MethodParameter(
                        parameterAnnotation,
                        annotations[i],
                        parameterClasses[i],
                        parameterGenTypes[i],
                        defaultValue,
                        encoded || methodEncoded || classEncoded);
                methodParameters.add(methodParameter);
            }

            return methodParameters;
        }

        return Collections.emptyList();
    }

    /**
     * According to JSR-311:
     * <p>
     * On receipt of a HEAD request an implementation MUST either: 1. Call method annotated with request method
     * designation for HEAD or, if none present, 2. Call method annotated with a request method designation GET and
     * discard any returned entity.
     * </p>
     */
    private void resolveHeadRequest() {
        List<ResourceMethodDescriptor> getResources = resourceMethods.get(HttpMethod.GET);
        if (getResources != null && getResources.size() > 0) {
            List<ResourceMethodDescriptor> headResources = getResourceMethods(HttpMethod.HEAD);
            for (ResourceMethodDescriptor resourceMethod : getResources) {
                if (findResourceMethodWithMediaTypes(headResources, resourceMethod.consumes(), resourceMethod.produces()) == null) {
                    headResources.add(
                            new ResourceMethodDescriptorImpl(resourceMethod.getMethod(),
                                                             HttpMethod.HEAD,
                                                             resourceMethod.getMethodParameters(),
                                                             this,
                                                             resourceMethod.consumes(),
                                                             resourceMethod.produces(),
                                                             resourceMethod.getNameBindingAnnotations()));
                }
            }
        }

        for (Map<String, List<SubResourceMethodDescriptor>> allSubResourceMethods : subResourceMethods.values()) {
            List<SubResourceMethodDescriptor> getSubResources = allSubResourceMethods.get(HttpMethod.GET);
            if (getSubResources != null && getSubResources.size() > 0) {
                List<SubResourceMethodDescriptor> headSubResources = allSubResourceMethods.get(HttpMethod.HEAD);
                if (headSubResources == null) {
                    headSubResources = new ArrayList<>();
                    allSubResourceMethods.put(HttpMethod.HEAD, headSubResources);
                }
                for (SubResourceMethodDescriptor subResourceMethod : getSubResources) {
                    if (findResourceMethodWithMediaTypes(headSubResources, subResourceMethod.consumes(), subResourceMethod.produces()) == null) {
                        headSubResources.add(
                                new SubResourceMethodDescriptorImpl(subResourceMethod.getPathValue(),
                                                                    subResourceMethod.getMethod(),
                                                                    HttpMethod.HEAD,
                                                                    subResourceMethod.getMethodParameters(),
                                                                    this,
                                                                    subResourceMethod.consumes(),
                                                                    subResourceMethod.produces(),
                                                                    subResourceMethod.getNameBindingAnnotations()));
                    }
                }
            }
        }
    }

    /**
     * According to JSR-311:
     * <p>
     * On receipt of a OPTIONS request an implementation MUST either: 1. Call method annotated with request method
     * designation for OPTIONS or, if none present, 2. Generate an automatic response using the metadata provided by the
     * JAX-RS annotations on the matching class and its methods.
     * </p>
     */
    private void resolveOptionsRequest() {
        List<ResourceMethodDescriptor> optionResources = getResourceMethods(HttpMethod.OPTIONS);
        if (optionResources.isEmpty()) {
            List<Parameter> methodParameters = Collections.emptyList();
            List<MediaType> consumes = Collections.singletonList(WILDCARD_TYPE);
            List<MediaType> produces = Collections.singletonList(WADL_TYPE);
            optionResources.add(new OptionsRequestResourceMethodDescriptorImpl("OPTIONS", methodParameters, this, consumes, produces, new Annotation[0]));
        }
    }

    /**
     * Tries to get JAX-RS annotation on method from the resource class's superclasses or implemented interfaces.
     *
     * @param <A>             annotation type
     * @param method          method for discovering
     * @param annotationClass annotation type what we are looking for
     * @param metaAnnotation  {@code false} if annotation should be on method and {@code true} in method should contain annotations that
     *                        has supplied annotation
     * @return annotation from class or its ancestor or {@code null} if nothing found
     */
    @SuppressWarnings("unchecked")
    private <A extends Annotation> A getMethodAnnotation(final Method method, Class<A> annotationClass, boolean metaAnnotation) {
        Annotation annotation = metaAnnotation ? findFirstAnnotationAnnotatedWith(method, annotationClass) : method.getAnnotation(annotationClass);

        if (annotation == null) {
            Method aMethod;
            Class<?> aClass = getObjectClass();
            while (annotation == null && aClass != Object.class) {
                for (Class<?> anInterface : aClass.getInterfaces()) {
                    aMethod = getMethodIfPossible(anInterface, method.getName(), method.getParameterTypes());
                    if (aMethod != null) {
                        Annotation newAnnotation = metaAnnotation ? findFirstAnnotationAnnotatedWith(aMethod, annotationClass) : aMethod.getAnnotation(annotationClass);
                        if (annotation == null) {
                            annotation = newAnnotation;
                        } else {
                            throw new RuntimeException(String.format("Conflicts of JAX-RS annotations on method %s of resource %s. Method is declared in more than one interface and different interfaces contains JAX-RS annotations.",
                                    aMethod.getName(), getObjectClass().getName()));
                        }
                    }
                }
                if (annotation == null) {
                    aClass = aClass.getSuperclass();
                    if (aClass != Object.class) {
                        aMethod = getMethodIfPossible(aClass, method.getName(), method.getParameterTypes());
                        if (aMethod != null) {
                            annotation = metaAnnotation ? findFirstAnnotationAnnotatedWith(aMethod, annotationClass) : aMethod.getAnnotation(annotationClass);
                        }
                    }
                }
            }
        }

        if (annotation == null || !metaAnnotation) {
            return (A) annotation;
        }
        return annotation.annotationType().getAnnotation(annotationClass);
    }

    private Method getMethodIfPossible(Class<?> methodOwner, String methodName, Class<?>[] methodParameters) {
        Method aMethod = null;
        try {
            aMethod = methodOwner.getDeclaredMethod(methodName, methodParameters);
        } catch (NoSuchMethodException ignored) {
        }
        return aMethod;
    }

    private <A extends Annotation> A getClassAnnotation(Class<?> resourceClass, Class<A> annotationClass) {
        A annotation = resourceClass.getAnnotation(annotationClass);
        if (annotation == null) {
            Class<?> aClass = resourceClass;
            while (annotation == null && aClass != Object.class) {
                for (Class<?> anInterface : aClass.getInterfaces()) {
                    A newAnnotation = anInterface.getAnnotation(annotationClass);
                    if (annotation == null) {
                        annotation = newAnnotation;
                    } else {
                        throw new RuntimeException(String.format("Conflict of JAX-RS annotation on class %s. Class implements more that one interface and few interfaces have JAX-RS annotations.", resourceClass.getName()));
                    }
                }
                if (annotation == null) {
                    aClass = aClass.getSuperclass();
                    if (aClass != Object.class) {
                        annotation = aClass.getAnnotation(annotationClass);
                    }
                }
            }
        }
        return annotation;
    }

    private <RM extends ResourceMethodDescriptor> ResourceMethodDescriptor findResourceMethodWithMediaTypes(List<RM> resourceMethods,
                                                                                                            List<MediaType> consumes,
                                                                                                            List<MediaType> produces) {
        ResourceMethodDescriptor matched = null;
        if (resourceMethods != null && !resourceMethods.isEmpty()) {
            for (Iterator<RM> iterator = resourceMethods.iterator(); matched == null && iterator.hasNext(); ) {
                RM method = iterator.next();
                if (method.consumes().size() == consumes.size()
                        && method.produces().size() == produces.size()
                        && method.consumes().containsAll(consumes)
                        && method.produces().containsAll(produces)) {
                    matched = method;
                }
            }
        }
        return matched;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                          .add("path", getPathValue())
                          .add("isRootResource", isRootResource())
                          .add("class", getObjectClass())
                          .omitNullValues()
                          .toString();
    }
}
