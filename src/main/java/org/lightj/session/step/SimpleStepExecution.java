package org.lightj.session.step;

import org.lightj.session.FlowContext;
import org.lightj.session.exception.FlowExecutionException;

/**
 * execute with set transition
 * 
 * @author binyu
 *
 * @param <T>
 */
public class SimpleStepExecution<T extends FlowContext> extends StepExecution<T> {


	/**
	 * constructor
	 */
	public SimpleStepExecution() {
		super(null);
	}

	/**
	 * constructor
	 * @param transition
	 */
	public SimpleStepExecution(StepTransition transition) {
		super(transition);
	}

	/**
	 * constructor with next step
	 * @param transition
	 */
	public SimpleStepExecution(String nextStep) {
		super(StepTransition.runToStep(nextStep));
	}

	@Override
	public StepTransition execute() throws FlowExecutionException {
		return defResult;
	}
	
}
