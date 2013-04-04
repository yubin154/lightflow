package org.lightj.session.step;

import org.lightj.session.FlowContext;
import org.lightj.session.FlowDriver;
import org.lightj.session.FlowExecutionException;
import org.lightj.session.FlowStepOptions;

/**
 * A step in a workflow session
 * @author biyu
 *
 */
public interface IFlowStep {
	
	/**
	 * execute a flow step
	 * @throws FlowExecutionException
	 */
	public StepTransition execute() throws FlowExecutionException;
	
	/**
	 * handling error condition
	 * @return
	 * @throws FlowExecutionException
	 */
	public StepTransition onExecutionError(Throwable t);
	
	/**
	 * handling result, 
	 * a noop in synchronous step
	 * a callback in asynchronous and join step
	 * 
	 * @param result
	 * @return
	 * @throws FlowExecutionException
	 */
	public StepTransition onResult() throws FlowExecutionException;
	
	/**
	 * handling error condition in onResult()
	 * @param t
	 * @return
	 */
	public StepTransition onResultError(Throwable t);
	
	/**
	 * register the flow driver to the step to be used in callback notification
	 * @param driver
	 */
	public void setFlowDriver(FlowDriver driver);
	
	/**
	 * flow driver
	 * @return
	 */
	public FlowDriver getFlowDriver();

	/**
	 * pass down session context to all its executions
	 * @param context
	 */
	public void setSessionContext(FlowContext context);
	
	/**
	 * resume a parked step in asynchronous callback
	 *
	 */
	public void resume();
	
	/**
	 * step name
	 * @return
	 */
	public String getStepName();
	
	/**
	 * set step name
	 * @param name
	 */
	public void setStepName(String name);
	
	/**
	 * set flow step statistics
	 * @param statistics
	 */
	public void setStatistics(StepStatistics statistics);
	
	/**
	 * get flow step statistics
	 * @return
	 */
	public StepStatistics getStatistics();
	
	/**
	 * transition caused this step to be executed
	 * @param transitionFrom
	 */
	public void setTransitionFrom(StepTransition transitionFrom);
	
	/**
	 * the transition caused this step to be executed
	 * @return
	 */
	public StepTransition getTransitionFrom();

	/**
	 * flow step options available at runtime
	 * @return
	 */
	public FlowStepOptions getStepOptions();
	
	/**
	 * set timeout for the step
	 * @param timeoutMilliSec
	 * @return
	 */
	public IFlowStep setTimeoutMilliSec(int timeoutMilliSec);
	
	/**
	 * a unique step id
	 * @return
	 */
	public String getStepId();

	/**
	 * set default if null
	 * @param exec
	 * @param ehandler
	 * @param chandler
	 * @param cehandler
	 */
	@SuppressWarnings("rawtypes")
	void setIfNull(StepExecution exec, StepErrorHandler ehandler, StepCallbackHandler chandler, StepErrorHandler cehandler);
	
}
