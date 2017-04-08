package org.everrest.core.method;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public interface TypeProducerFactory {
    /**
     * @param aClass      parameter class
     * @param genericType generic parameter type
     * @param annotations annotations associated with the parameter
     * @return TypeProducer
     * @see TypeProducer
     * @see Method#getParameterTypes()
     * @see Method#getGenericParameterTypes()
     */
    <T> TypeProducer<T> createTypeProducer(Class<T> aClass, Type genericType, Annotation[] annotations);
}
