package org.lightj.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 
 *
 */
public class SpringContextUtil {
	
	/** contexts */
	private static Map<String, ApplicationContext> ctxes = new HashMap<String, ApplicationContext>();
	
	/**
	 * load context
	 * @param key
	 * @param xmlPath
	 * @return
	 */
	public synchronized static ApplicationContext loadContext(String key, String... xmlPaths) {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(xmlPaths);
		ctxes.put(key, ctx);
		return ctx;
	}
	
	/**
	 * has key in context map
	 * @param key
	 * @return
	 */
	public static boolean hasContext(String key) {
		return ctxes.containsKey(key);
	}
	
	/**
	 * get context by key
	 * @param key
	 * @return
	 */
	public static ApplicationContext getContext(String key) {
		return ctxes.get(key);
	}

	/**
	 * register context
	 * @param key
	 * @param context
	 */
	public synchronized static void registerContext(String key, ApplicationContext context) {
		ctxes.put(key, context);
	}
	
	/**
	 * get bean
	 * @param key
	 * @param beanName
	 * @return
	 */
	public static Object getBean(String key, String beanName) {
		if (!ctxes.containsKey(key)) throw new IllegalArgumentException("application context " + key + " not exist");
		return ctxes.get(key).getBean(beanName);
	}
	
	/**
	 * get bean of a class
	 * @param key
	 * @param beanName
	 * @param beanKlazz
	 * @return
	 */
	public static <T> T getBean(String key, String beanName, Class<T> beanKlazz) {
		if (!ctxes.containsKey(key)) throw new IllegalArgumentException("application context " + key + " not exist");
		return ctxes.get(key).getBean(beanName, beanKlazz);
	}

	/**
	 * get a bean of a class, it requires there can be only one bean registered in the context for the class
	 * @param beanKlazz
	 * @return
	 */
	public static <T> T getBean(String key, Class<T> beanKlazz) {
		if (!ctxes.containsKey(key)) throw new IllegalArgumentException("application context " + key + " not exist");
		return ctxes.get(key).getBean(beanKlazz);
	}
	
	/**
	 * get bean type of a super class from a context
	 * @param beanBaseKlazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<Class<? extends T>> getBeansClass(String key, Class<T> beanBaseKlazz) {
		if (!ctxes.containsKey(key)) throw new IllegalArgumentException("application context " + key + " not exist");
		ApplicationContext ctx = ctxes.get(key);
		List<Class<? extends T>> klazzes = new ArrayList<Class<? extends T>>(); 
		for (String name : ctx.getBeanNamesForType(beanBaseKlazz)) {
			if (beanBaseKlazz.isAssignableFrom(ctx.getType(name))) {
				klazzes.add((Class<? extends T>) ctx.getType(name));
			}
		}
		return klazzes;
	}
	
	/**
	 * get all registered bean type of a super class 
	 * @param key
	 * @param beanBaseKlazz
	 * @return
	 */
	public static <T> List<Class<? extends T>> getAllBeansClass(Class<T> beanBaseKlazz) {
		List<Class<? extends T>> klazzes = new ArrayList<Class<? extends T>>(); 
		for (String ctxName : ctxes.keySet()) {
			klazzes.addAll(getBeansClass(ctxName, beanBaseKlazz));
		}
		return klazzes;
	}
	
	/**
	 * get bean of a class from all registered context
	 * @param beanName
	 * @param beanKlass
	 * @return
	 */
	public static <T> T getBeanFromAllContext(Class<T> beanKlass) {
		for (ApplicationContext ctx : ctxes.values()) {
			try {
				return ctx.getBean(beanKlass);
			}
			catch (NoSuchBeanDefinitionException e) {
				// ignore
			}
		}
		throw new NoSuchBeanDefinitionException(String.format("bean of type %s does not exist", beanKlass.getName()));
	}

	/**
	 * get bean of a class of a name from all registered context
	 * @param beanName
	 * @param beanKlass
	 * @return
	 */
	public static <T> T getBeanOfNameFromAllContext(String beanName, Class<T> beanKlass) {
		for (ApplicationContext ctx : ctxes.values()) {
			try {
				return ctx.getBean(beanName, beanKlass);
			}
			catch (NoSuchBeanDefinitionException e) {
				// ignore
			}
		}
		throw new NoSuchBeanDefinitionException(String.format("%s of type %s does not exist", beanName, beanKlass.getName()));
	}
}
