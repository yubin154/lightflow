package org.lightj.session;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FlowProperties {
	
	/**
	 * if the flow should be synchronized on target, 
	 * et. when active flow exist on the target, no new flow of the same time is allowed
	 * @see FlowExistException 
	 * @return
	 */
	boolean lockTarget()	default false;

	/**
	 * if interruptible, flow driver will try to load the latest session data from db before executing a step
	 * if the session is not interruptible, et. its execution state cannot be changed externally, the flag can be false 
	 * @return
	 */
	boolean interruptible() 		default false;
	
	/**
	 * if clustered, peers in the same cluster will try to recover the session 
	 * @return
	 */
	boolean clustered()			default true;
	
	/**
	 * setting this property will force flow to timeout in x seconds
	 * flow will be stopped with timeout status, see {@link FlowTimer}
	 * @return
	 */
	int timeoutInSec()		default -1;
	
	/**
	 * priority of the session compared to other sessions in the same task group
	 * @return
	 */
	int priority()				default 5;
	
	/**
	 * if recover fails, whether to terminate session or pause it as is
	 * @return
	 */
	boolean killNonRecoverable()	default true;
	
	/**
	 * default error step
	 * @return
	 */
	String errorStep()		default "";
	
}
