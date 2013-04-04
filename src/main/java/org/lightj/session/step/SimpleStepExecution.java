package org.lightj.session.step;

import org.lightj.session.FlowContext;
import org.lightj.session.FlowExecutionException;

@SuppressWarnings("rawtypes")
public class SimpleStepExecution<T extends FlowContext> extends StepExecution<T> {

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

	/**
	 * constructor with next step
	 * @param nextStep
	 */
	public SimpleStepExecution(Enum nextStep) {
		super(StepTransition.runToStep(nextStep));
	}
	
	@Override
	public StepTransition execute() throws FlowExecutionException {
		return defResult;
	}
	
}
