package org.lightj.session.step;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * simple branching enclosure
 * @author binyu
 *
 */
public class DelayedEnclosure implements InvocationHandler {
	
	private Object realImpl;
	private long delayMs;

	public DelayedEnclosure(Object impl, long delayMs) {
		this.realImpl = impl;
		this.delayMs = delayMs;
	}

	@Override
	public Object invoke(Object proxy, final Method method, final Object[] args)
			throws Throwable {
		if (Object.class == method.getDeclaringClass()) {
			String name = method.getName();
			if ("equals".equals(name)) {
				return proxy == args[0];
			} else if ("hashCode".equals(name)) {
				return System.identityHashCode(proxy);
			} else if ("toString".equals(name)) {
				return proxy.getClass().getName() + "@"
						+ Integer.toHexString(System.identityHashCode(proxy))
						+ ", with InvocationHandler " + this;
			} else {
				throw new IllegalStateException(String.valueOf(method));
			}
		}
		else if ("execute".equals(method.getName())) {
			try { 
				Thread.sleep(delayMs);
			} catch (InterruptedException e) {
				// ignore
			}
			return method.invoke(realImpl, args);
		}
		else {
			return method.invoke(realImpl, args);
		}
		
	}	
	
	public static IFlowStep delay(long delayMs, IFlowStep step) {
		   return (IFlowStep) Proxy.newProxyInstance(IFlowStep.class.getClassLoader(),
                   new Class<?>[] {IFlowStep.class},
                   new DelayedEnclosure(step, delayMs));
	}

}
