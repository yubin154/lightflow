package org.lightj.session.eventlistener;

import java.util.Timer;
import java.util.TimerTask;

import org.lightj.session.FlowEvent;
import org.lightj.session.FlowResult;
import org.lightj.session.FlowSession;
import org.lightj.session.FlowState;
import org.lightj.session.IFlowEventListener;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepTransition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * add timer functionality to a flow
 * 
 * @author biyu
 *
 */
@SuppressWarnings({"rawtypes"})
public class FlowTimer implements IFlowEventListener {
	
	static Logger logger = LoggerFactory.getLogger(FlowTimer.class);
	
	/** flow timer */
	private Timer flowTimeout;
	
	/**
	 * error event, cancel timer at flow error
	 * @param t
	 * @param session
	 */
	public void handleError(Throwable t, FlowSession session) {
		// do nothing
		if(flowTimeout != null){
			flowTimeout.cancel();
		}
	}

	/**
	 * flow event, start, complete
	 * @param event
	 * @param session
	 */
	public void handleFlowEvent(final FlowEvent event, final FlowSession session, String msg) {
		if (event == FlowEvent.start && session.getFlowProperties().timeoutInSec() > 0) {
			long timeoutMs = session.getFlowProperties().timeoutInSec() * 1000;

			flowTimeout = new Timer();
			flowTimeout.schedule( new TimerTask() {
					
				@Override
				public void run() {
					if(session.getEndDate()==null){
						session.killFlow(FlowState.Completed, FlowResult.Timeout, "Flow timeout");
					}
		 		}
					
			}, timeoutMs);

		}
		else if(event == FlowEvent.stop && flowTimeout != null){
			flowTimeout.cancel();
		}
	}

	/**
	 * step event, entry, exit
	 * support for flow step timeout withdrawn
	 * @param event
	 * @param session
	 * @param stepTransition
	 * @param execProperties
	 */
	public void handleStepEvent(FlowEvent event, final FlowSession session, final IFlowStep flowStep, StepTransition stepTransition) 
	{
		// noop
	}
	
}
