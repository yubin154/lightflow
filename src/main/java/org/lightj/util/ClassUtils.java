package org.lightj.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class ClassUtils {

	public static Type[] getGenericType(Class<?> target) {
		if (target == null)
			return new Type[0];
		Type[] types = target.getGenericInterfaces();
		if (types.length > 0) {
			return types;
		}
		Type type = target.getGenericSuperclass();
		if (type != null) {
			if (type instanceof ParameterizedType) {
				return new Type[] { type };
			}
		}
		return new Type[0];
	}

}