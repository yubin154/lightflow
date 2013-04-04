package org.lightj.session;

import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepTransition;

@SuppressWarnings("rawtypes")
public interface IFlowEventListener {
	
	/**
	 * step event, entry, exit
	 * @param event
	 * @param session
	 * @param stepTransition
	 * @param execProperties
	 */
	public void handleStepEvent(
			FlowEvent event, 
			FlowSession session,
			IFlowStep flowStep,
			StepTransition stepTransition);

	/**
	 * flow event, start, complete
	 * @param event
	 * @param session
	 */
	public void handleFlowEvent(
			FlowEvent event,
			FlowSession session);
	
	/**
	 * error event
	 * @param t
	 * @param session
	 */
	public void handleError(
			Throwable t, 
			FlowSession session);
	
}
