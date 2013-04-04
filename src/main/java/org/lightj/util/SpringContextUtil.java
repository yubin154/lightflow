package org.lightj.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringContextUtil {
	
	private static Map<String, ApplicationContext> ctxes = new HashMap<String, ApplicationContext>();
	
	public static ApplicationContext loadContext(String key, String xmlPath) {
		ApplicationContext ctx = new ClassPathXmlApplicationContext(xmlPath);
		ctxes.put(key, ctx);
		return ctx;
	}
	
	public static boolean hasContext(String key) {
		return ctxes.containsKey(key);
	}
	
	public static ApplicationContext getContext(String key) {
		return ctxes.get(key);
	}
	
}
