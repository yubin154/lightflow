package org.lightj.session.eventlistener;

import org.lightj.session.FlowEvent;
import org.lightj.session.FlowSession;
import org.lightj.session.IFlowEventListener;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepTransition;

/**
 * track flow stats
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
public class FlowStatsTracker implements IFlowEventListener {

	private FlowStatistics stat;
	
	public void handleError(Throwable t, FlowSession session) {
	}

	@SuppressWarnings("unchecked")
	public void handleFlowEvent(FlowEvent event, FlowSession session, String msg) {
		switch (event) {
		case start:
			stat = new FlowStatistics(session.getOrderedStepProperties(), session.getCurrentAction());
			session.getSessionContext().setPctComplete(0);
			break;
		case stop:
			session.getSessionContext().setPctComplete(100);
			break;
		default:
		}
	}

	public void handleStepEvent(FlowEvent event, FlowSession session,
			IFlowStep flowStep, StepTransition stepTransition) {
		if (event == FlowEvent.stepExit) {
			stat.updatePercentComplete(flowStep.getStepName());
			session.getSessionContext().setPctComplete(stat.getPercentComplete());
		}
	}

}
