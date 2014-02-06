package org.lightj.session.eventlistener;

import org.lightj.session.FlowEvent;
import org.lightj.session.FlowResult;
import org.lightj.session.FlowSession;
import org.lightj.session.FlowState;
import org.lightj.session.IFlowEventListener;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepTransition;

@SuppressWarnings("rawtypes")
public class FlowRecoverEventListener implements IFlowEventListener {

	@Override
	public void handleStepEvent(FlowEvent event, FlowSession session,
			IFlowStep flowStep, StepTransition stepTransition) {
	}

	@Override
	public void handleFlowEvent(FlowEvent event, FlowSession session, String msg) {
		switch (event) {
		case recoverNot:
			session.killFlow(FlowState.Canceled, FlowResult.Failed, msg);
			break;
		case recoverFailure:
			session.killFlow(FlowState.Canceled, FlowResult.Failed, msg);
			break;
		}
	}

	@Override
	public void handleError(Throwable t, FlowSession session) {
	}

}
