package org.lightj.session;

import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepTransition;

/**
 * track flow stats
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
public class FlowStatsTracker implements IFlowEventListener {

	public void handleError(Throwable t, FlowSession session) {
	}

	public void handleFlowEvent(FlowEvent event, FlowSession session) {
		switch (event) {
		case start:
			session.getStats().setPercentComplete(0);
			break;
		case stop:
			session.getStats().setPercentComplete(100);
			break;
		default:
		}
	}

	public void handleStepEvent(FlowEvent event, FlowSession session,
			IFlowStep flowStep, StepTransition stepTransition) {
		if (event == FlowEvent.stepExit) {
			session.getStats().updatePercentComplete(flowStep.getStepName());
		}
	}

}
