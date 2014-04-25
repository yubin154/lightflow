package org.lightj.session.step;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * retry enclosure, to create a proxy around an actual step to have retry behavior
 * 
 * @author binyu
 *
 */
public class RetryEnclosure<T extends IFlowStep> implements InvocationHandler {
	
	static Set<String> methods = new HashSet<String>(Arrays.asList(new String[] {"onExecutionError","execute","onResult","onResultError"}));
	
	private T realImpl;
	private StepTransition[] transitions;
	private int maxRetry = 0;
	private boolean retryOnMatch;
	private AtomicInteger retry = new AtomicInteger(0);

	private RetryEnclosure(T impl, int maxRetry, boolean retryOnMatch, StepTransition... transitions) {
		this.realImpl = impl;
		this.transitions = transitions;
		this.maxRetry = maxRetry;
		this.retryOnMatch = retryOnMatch;
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
		else if (methods.contains(method.getName())) {
			Object rst = method.invoke(realImpl, args);
			if (rst instanceof StepTransition) {
				StepTransition cur = (StepTransition) rst;
				if (maxRetry <= 0 || retry.incrementAndGet() <= maxRetry) {
					String stepName = ((IFlowStep) realImpl).getStepName();
					for (StepTransition transition : transitions) {
						if (cur.compareTo(transition) == 0) {
							return retryOnMatch ? StepTransition.runToStep(stepName) : cur;
						}
					}
					if (!retryOnMatch) {
						return StepTransition.runToStep(stepName);
					}
				}
				else {
					((StepTransition) rst).setDetail("retry limit reached: " + retry.get());
				}
			}
			return rst;
		}
		else {
			return method.invoke(realImpl, args);
		}
		
	}	
	
	/**
	 * make a step retry for maxRetry time on matching condition
	 * @param condition
	 * @param maxRetry 0 means infinite
	 * @param delay
	 * @param step
	 * @return
	 */
	public static IFlowStep retryIf(IFlowStep step, int maxRetry, StepTransition... conditions) {
		   return (IFlowStep) Proxy.newProxyInstance(IFlowStep.class.getClassLoader(),
                   new Class<?>[] {IFlowStep.class},
                   new RetryEnclosure<IFlowStep>(step, maxRetry, true, conditions));
	}


	/**
	 * make a step retry for maxRetry time till matching one of the conditions
	 * @param step
	 * @param maxRetry 0 means infinite
	 * @param conditions
	 * @return
	 */
	public static IFlowStep retryTill(IFlowStep step, int maxRetry, StepTransition... conditions) {
		   return (IFlowStep) Proxy.newProxyInstance(IFlowStep.class.getClassLoader(),
                new Class<?>[] {IFlowStep.class},
                new RetryEnclosure<IFlowStep>(step, maxRetry, false, conditions));
	}
}
