package org.lightj.session.step;

import org.lightj.session.FlowContext;
import org.lightj.session.FlowExecutionException;


/**
 * An execution aspect of a flow step, whether it is a execution, or callback handling,
 * or error handling
 * 
 * @author biyu
 *
 */
public abstract class StepExecution<T extends FlowContext> {
	
	/**
	 * default flow transition
	 */
	protected StepTransition defResult;
	
	/**
	 * parent step that hosts this execution
	 */
	protected IFlowStep flowStep;
	
	/**
	 * a locatable key that can be used to reinstantiate the execution later
	 */
	protected String key;
	
	/**
	 * session context
	 */
	protected T sessionContext;
	
	/**
	 * constructor
	 *
	 */
	StepExecution() {}
	
	/**
	 * construct a new flow step execution with a default transition 
	 * @param transition
	 */
	public StepExecution(StepTransition transition) {
		this.defResult = transition;
	}
	
	/**
	 * do the work
	 * @return
	 * @throws FlowExecutionException
	 */
	public abstract StepTransition execute() throws FlowExecutionException;

	/**
	 * get default transition
	 * @return
	 */
	public StepTransition getDefResult() {
		return defResult;
	}

	/**
	 * set default transition
	 * @param defResult
	 */
	public void setDefResult(StepTransition defResult) {
		this.defResult = defResult;
	}
	
	/**
	 * set parent flowstep that hosts this execution, called from {@link StepImpl}
	 * @param flowStep
	 */
	public void setFlowStep(IFlowStep flowStep) {
		this.flowStep = flowStep;
	}

	/**
	 * set session context
	 * @param sessionContext
	 */
	public void setSessionContext(T sessionContext) {
		this.sessionContext = sessionContext;
	}
	
	/**
	 * set default transition if null
	 * @param transition
	 */
	public void setDefIfNull(StepTransition transition) {
		if (defResult == null) {
			defResult = transition;
		}
	}
}
