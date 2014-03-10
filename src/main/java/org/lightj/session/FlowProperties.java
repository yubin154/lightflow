package org.lightj.session;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.lightj.session.eventlistener.FlowTimer;
import org.lightj.session.exception.FlowExistException;

/**
 * flow properties
 * 
 * @author binyu
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FlowProperties {
	
	/** flow type */
	public String typeId();
	
	/** flow desc */
	public String desc()	default "";

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
	
	}
