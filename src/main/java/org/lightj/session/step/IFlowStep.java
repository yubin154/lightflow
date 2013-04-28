package org.lightj.session.step;

import org.lightj.session.FlowContext;
import org.lightj.session.FlowDriver;
import org.lightj.session.FlowExecutionException;
import org.lightj.session.FlowStepProperties;

/**
 * A step in a workflow session
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
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
	public StepTransition onError(Throwable t);
	
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
	 * resume a parked step with a step transition in asynchronous callback
	 *
	 */
	public void resume(StepTransition transition);

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
	 * flow step options available at runtime
	 * @return
	 */
	public FlowStepProperties getFlowStepProperties();

	/**
	 * set flow step properties 
	 * @param flowStepProperties
	 */
	public void setFlowStepProperties(FlowStepProperties flowStepProperties);

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
	void setIfNull(StepExecution exec, StepErrorHandler ehandler, StepCallbackHandler chandler);
	
	/**
	 * execution
	 * @return
	 */
	public StepExecution getExecution();
	
	/**
	 * result handler
	 * @return
	 */
	public StepCallbackHandler getResultHandler();
	
	/**
	 * error handler
	 * @return
	 */
	public StepErrorHandler getErrorHandler();
	
}
