package org.lightj.session.step;

import java.util.UUID;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.lightj.session.FlowContext;
import org.lightj.session.FlowDriver;
import org.lightj.session.FlowExecutionException;
import org.lightj.session.FlowModule;
import org.lightj.session.FlowStepOptions;

/**
 * a flow step, wraps all executions to do work, callback handling, and error handling
 * 
 * @author biyu
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class StepImpl implements IFlowStep {
	
	static Category logger = Logger.getLogger(StepImpl.class);
	
	/** unique step id */
	private final String stepId = "Step|" + UUID.randomUUID().toString();
	
	/**
	 * step it represents in the flow, set by framework
	 */
	protected String stepName;
	
	/**
	 * step execution, set by user
	 */
	protected StepExecution execution;
	
	/**
	 * result handler, set by user
	 */
	protected StepCallbackHandler resultHandler;
	
	/**
	 * execution error handler, set by user
	 */
	protected StepErrorHandler executionErrorHandler;
	
	/**
	 * result error handler, set by user
	 */
	protected StepErrorHandler resultErrorHandler;
	
	/**
	 * flow driver that drives this step, set by framework
	 */
	protected FlowDriver driver;
	
	/**
	 * execution options
	 */
	protected FlowStepOptions flowStepOptions = new FlowStepOptions();
	
	/**
	 * flow step statistics
	 */
	protected StepStatistics statistics;
	
	/**
	 * transition caused this step to be executed
	 */
	protected StepTransition transitionFrom;
	
	/**
	 * locatable key
	 */
	protected String key;
	
	/**
	 * construct a flow step with a name and its associated session
	 * @param session
	 * @param stepName
	 */
	public StepImpl() {
	}

	/**
	 * do the work
	 */
	public StepTransition execute() throws FlowExecutionException {
		return execution.execute();
	}

	/**
	 * handling execution error
	 */
	public StepTransition onExecutionError(Throwable t) {
		executionErrorHandler.setError(t);
		return executionErrorHandler.execute();
	}

	/**
	 * result handling
	 */
	public StepTransition onResult() throws FlowExecutionException {
		return resultHandler.execute();
	}

	/**
	 * result error handling
	 */
	public StepTransition onResultError(Throwable t) {
		resultErrorHandler.setError(t);
		return resultErrorHandler.execute();
	}

	/**
	 * register the flow driver to the step to be used in callback notification
	 * @param driver
	 */
	public void setFlowDriver(FlowDriver driver) {
		this.driver = driver;
	}
	
	/**
	 * get driver
	 * @return
	 */
	public FlowDriver getFlowDriver() {
		return driver;
	}
	
	/**
	 * pass down session context to all executions
	 * @param context
	 */
	public void setSessionContext(FlowContext context) {
		if (execution != null) execution.setSessionContext(context);
		if (executionErrorHandler != null) executionErrorHandler.setSessionContext(context);
		if (resultHandler != null) resultHandler.setSessionContext(context);
		if (resultErrorHandler != null) resultErrorHandler.setSessionContext(context);
	}

	/**
	 * resume a parked step on asynch callback
	 */
	public void resume() {
		// this is to make sure this step is still the "current" step recognized by the flow driver
		// and yes, we check reference equal
		synchronized (driver) {
			if (this.driver.getCurrentFlowStep() == this) {
				final FlowDriver dvr = driver;
				FlowModule.getExecutorService().submit(new Runnable() {
					
					@Override
					public void run() {
						dvr.callback();
					}
				});
			}
		}
	}

	/**
	 * set error handler for error happend in execution phase
	 * @param executionErrorHandler
	 */
	public void setExecutionErrorHandler(StepErrorHandler executionErrorHandler) {
		executionErrorHandler.setFlowStep(this);
		this.executionErrorHandler = executionErrorHandler;
	}

	/**
	 * set execution 
	 * @param execution
	 */
	public void setExecution(StepExecution execution) {
		execution.setFlowStep(this);
		this.execution = execution;
	}

	/**
	 * set error handler for error happend in result handling phase
	 * @param resultErrorHandler
	 */
	public void setResultErrorHandler(StepErrorHandler resultErrorHandler) {
		resultErrorHandler.setFlowStep(this);
		this.resultErrorHandler = resultErrorHandler;
	}

	/**
	 * set result handler
	 * @param resultHandler
	 */
	public void setResultHandler(StepCallbackHandler resultHandler) {
		execution.setDelegateTaskListener(resultHandler);
		resultHandler.setFlowStep(this);
		this.resultHandler = resultHandler;
	}
	
	/**
	 * flow step name
	 */
	public String getStepName() {
		return stepName;
	}
	
	/**
	 * set step name
	 * @param name
	 */
	public void setStepName(String name) {
		this.stepName = name;
	}

	/**
	 * flow step statistics
	 */
	public StepStatistics getStatistics() {
		return statistics;
	}

	/**
	 * flow step statistics
	 */
	public void setStatistics(StepStatistics statistics) {
		this.statistics = statistics;
	}

	/**
	 * transition from
	 */
	public void setTransitionFrom(StepTransition transitionFrom) {
		this.transitionFrom = transitionFrom;
	}

	/**
	 * transition from
	 */
	public StepTransition getTransitionFrom() {
		return transitionFrom;
	}

	public FlowStepOptions getStepOptions() {
		return flowStepOptions;
	}

	@Override
	public IFlowStep setTimeoutMilliSec(int timeoutMilliSec) {
		this.flowStepOptions.setTimeoutMs(timeoutMilliSec);
		return this;
	}

	@Override
	public final void setIfNull(StepExecution exec, StepErrorHandler ehandler,
			StepCallbackHandler chandler, StepErrorHandler cehandler) {
		if (execution == null) execution = exec;
		if (executionErrorHandler == null) executionErrorHandler = ehandler;
		if (resultErrorHandler == null) resultErrorHandler = cehandler;
		if (resultHandler == null) resultHandler = chandler;
	}

	@Override
	public String getStepId() {
		return stepId;
	}

}
