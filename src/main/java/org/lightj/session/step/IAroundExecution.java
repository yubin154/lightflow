package org.lightj.session.step;

import org.lightj.session.FlowContext;
import org.lightj.session.exception.FlowExecutionException;

/**
 * execute around main execution body
 * @author binyu
 *
 * @param <T>
 */
public interface IAroundExecution<T extends FlowContext> {

	/**
	 * execute before main execution
	 * @param ctx
	 * @throws FlowExecutionException
	 */
	public void preExecute(T ctx) throws FlowExecutionException;
	
	/**
	 * execute after main execution
	 * @param ctx
	 * @throws FlowExecutionException
	 */
	public void postExecute(T ctx) throws FlowExecutionException;
}
