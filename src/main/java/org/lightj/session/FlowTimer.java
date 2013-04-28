package org.lightj.session;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepTransition;
import org.lightj.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * add timer functionality to a flow or a flow step
 * 
 * @author biyu
 *
 */
@SuppressWarnings({"rawtypes"})
public class FlowTimer implements IFlowEventListener {
	
	static Logger logger = LoggerFactory.getLogger(FlowTimer.class);
	
	private Timer flowTimeout;
	
	/**
	 * error event
	 * @param t
	 * @param session
	 */
	public void handleError(Throwable t, FlowSession session) {
		// do nothing
	}

	/**
	 * flow event, start, complete
	 * @param event
	 * @param session
	 */
	public void handleFlowEvent(final FlowEvent event, final FlowSession session) {
		if (event == FlowEvent.start && session.getFlowProperties().timeoutInSec() > 0) {
			long timeoutMs = session.getFlowProperties().timeoutInSec() * 1000;
			Date timeoutAt = new Date(System.currentTimeMillis() + timeoutMs);	
			String msg = "Session will timeout at " + DateUtil.format(timeoutAt, "yyyy-MM-dd HH:mm:ss");
			FlowSaver.persistStepHistory(session, null, msg, FlowResult.InProgress.name());

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
	 * support for flow step timeout withdrawned
	 * @param event
	 * @param session
	 * @param stepTransition
	 * @param execProperties
	 */
	public void handleStepEvent(FlowEvent event, final FlowSession session, final IFlowStep flowStep, StepTransition stepTransition) 
	{
	}
	
}
