package org.lightj.session.step;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.lightj.session.FlowContext;
import org.lightj.session.FlowDriver;
import org.lightj.session.FlowModule;
import org.lightj.session.FlowStepProperties;
import org.lightj.session.exception.FlowExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a flow step, wraps all executions to do work, callback handling, and error handling
 * 
 * @author biyu
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class StepImpl implements IFlowStep {
	
	static Logger logger = LoggerFactory.getLogger(StepImpl.class);
	
	/** part of a unique step id */
	private final String uid = UUID.randomUUID().toString();
	
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
	protected StepErrorHandler errorHandler;
	
	/**
	 * flow driver that drives this step, set by framework
	 */
	protected FlowDriver driver;
	
	/**
	 * execution options
	 */
	protected FlowStepProperties flowStepProperties;
	

	/** how many times flow entered this step */
	private AtomicInteger stepEntry = new AtomicInteger(0);
	
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
	public StepTransition onError(Throwable t) {
		errorHandler.setError(t);
		return errorHandler.execute();
	}

	/**
	 * result handling
	 */
	public StepTransition onResult() throws FlowExecutionException {
		return resultHandler.execute();
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
		if (errorHandler != null) errorHandler.setSessionContext(context);
		if (resultHandler != null) resultHandler.setSessionContext(context);
	}

	/**
	 * resume a parked step on asynch callback
	 */
	public void resume(final StepTransition trans) {
		// this is to make sure this step is still the "current" step recognized by the flow driver
		// and yes, we check reference equal
		synchronized (driver) {
			if (this.driver.getCurrentFlowStep() == this) {
				final FlowDriver dvr = driver;
				FlowModule.getExecutorService().submit(new Runnable() {
					
					@Override
					public void run() {
						dvr.driveWithTransition(trans);
					}
					
				});
			}
		}
	}

	/**
	 * resume a parked step on asynch callback
	 */
	public void resume(final Throwable t) {
		// this is to make sure this step is still the "current" step recognized by the flow driver
		// and yes, we check reference equal
		synchronized (driver) {
			if (this.driver.getCurrentFlowStep() == this) {
				final FlowDriver dvr = driver;
				FlowModule.getExecutorService().submit(new Runnable() {
					
					@Override
					public void run() {
						dvr.driveWithError(t);
					}
					
				});
			}
		}
	}

	/**
	 * set error handler for error happend in execution phase
	 * @param executionErrorHandler
	 */
	public void setErrorHandler(StepErrorHandler executionErrorHandler) {
		executionErrorHandler.setFlowStep(this);
		this.errorHandler = executionErrorHandler;
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
	 * set or merge result handler
	 * @param resultHandler
	 */
	public void setOrUpdateResultHandler(StepCallbackHandler resultHandler) {
		if (resultHandler != null) {
			if (this.resultHandler == null) {
				setResultHandler(resultHandler);
			} else {
				this.resultHandler.merge(resultHandler);
			}
		}
	}
	
	/**
	 * set result handler
	 * @param resultHandler
	 */
	public void setResultHandler(StepCallbackHandler resultHandler) {
		if (resultHandler != null) {
			resultHandler.setFlowStep(this);
			this.resultHandler = resultHandler;
		}
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

	public FlowStepProperties getFlowStepProperties() {
		return flowStepProperties;
	}

	public void setFlowStepProperties(FlowStepProperties flowStepProperties) {
		this.flowStepProperties = flowStepProperties;
	}

	@Override
	public final void setIfNull(StepExecution exec, StepErrorHandler ehandler, StepCallbackHandler chandler) {
		if (execution == null) {
			this.setExecution(exec);
		} else if (exec != null) {
			execution.setDefIfNull(exec.getDefResult());
		}
		if (errorHandler == null) this.setErrorHandler(ehandler);
		if (resultHandler == null) {
			this.setResultHandler(chandler);
		} else if (chandler != null){
			resultHandler.setDefIfNull(chandler.getDefResult());
			resultHandler.setResultMapIfNull(chandler);
		}
		
	}

	@Override
	public String getStepId() {
		return String.format("Step|%s|%s|%s", stepName, uid, stepEntry.get());
	}

	@Override
	public StepExecution getExecution() {
		return execution;
	}

	@Override
	public StepCallbackHandler getResultHandler() {
		return resultHandler;
	}

	@Override
	public StepErrorHandler getErrorHandler() {
		return errorHandler;
	}

	@Override
	public int getAndIncrementStepEntry() {
		return stepEntry.getAndIncrement();
	}

}
