package org.lightj.session;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepTransition;
import org.lightj.util.DateUtil;
import org.lightj.util.Log4jProxy;
import org.lightj.util.StringUtil;


/**
 * add timer functionality to a flow or a flow step
 * 
 * @author biyu
 *
 */
@SuppressWarnings({"rawtypes"})
public class FlowTimer implements IFlowEventListener {
	
	static Log4jProxy cat = Log4jProxy.getInstance(FlowTimer.class);
	
	private Timer flowTimeout;
	
	private Timer flowStepTimeout;
	
	private String curStepWithTimeout;
	
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
		if (event == FlowEvent.stepEntry && flowStep.getStepOptions().getTimeoutMs() > 0) {
			long timeoutMs = flowStep.getStepOptions().getTimeoutMs();
			Date timeoutAt = new Date(System.currentTimeMillis() + timeoutMs);	
			final String stepName = flowStep.getStepName();
			String msg = "Step will timeout at " + DateUtil.format(timeoutAt, "yyyy-MM-dd HH:mm:ss");
			FlowSaver.persistStepHistory(session, flowStep, msg, FlowResult.InProgress.name());

			if (flowStepTimeout != null && !StringUtil.equalIgnoreCase(stepName, curStepWithTimeout)) {
				flowStepTimeout.cancel();
			}
			flowStepTimeout = new Timer();
			flowStepTimeout.schedule( new TimerTask() {
					
				@Override
				public void run() {
					if (StringUtil.equalIgnoreCase(session.getCurrentAction(), stepName) 
							&& session.getEndDate()==null) {
						StepTransition timeoutTran = flowStep.onExecutionError(new FlowExecutionException("Step timed out"));
						flowStep.getFlowDriver().driveWithTransition(timeoutTran);
					}
		 		}
					
			}, timeoutMs);
			curStepWithTimeout = stepName;

		}
		else if(event == FlowEvent.stop && flowStepTimeout != null) {
			flowStepTimeout.cancel();
		}
	}
	
	static class StepTimerTask extends TimerTask {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
}
