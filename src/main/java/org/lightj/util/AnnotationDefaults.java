package org.lightj.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * this creates an instance of an annotation with default value
 * @author biyu
 *
 */
@SuppressWarnings("unchecked")
public class AnnotationDefaults implements InvocationHandler {
	
	public static <A extends Annotation> A of(Class<A> annotation) {
        return (A) Proxy.newProxyInstance(annotation.getClassLoader(), new Class[] {annotation}, new AnnotationDefaults());
    }
	
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.getDefaultValue();
    }
}
