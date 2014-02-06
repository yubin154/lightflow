package org.lightj.session.eventlistener;

import org.lightj.session.FlowEvent;
import org.lightj.session.FlowSession;
import org.lightj.session.FlowSessionFactory;
import org.lightj.session.IFlowEventListener;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepTransition;
import org.lightj.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({"rawtypes"})
public class FlowLogger implements IFlowEventListener {
	
	static Logger logger = LoggerFactory.getLogger(FlowLogger.class);
	
	private boolean cleanAfterComplete = false;
	public FlowLogger(boolean cleanAfterComplete) {
		this.cleanAfterComplete = cleanAfterComplete;
	}
	
	/**
	 * error event
	 * @param t
	 * @param session
	 */
	public void handleError(Throwable t, FlowSession session) {
		// noop
	}

	/**
	 * flow event, start, complete
	 * @param event
	 * @param session
	 */
	public void handleFlowEvent(FlowEvent event, FlowSession session, String msg) {
		// persist log ALWAYS on flow event start/stop
		switch (event) {
		case stop:
			try {
				logger.info(JsonUtil.encode(session.getFlowInfo()));
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
			if (cleanAfterComplete) {
				FlowSessionFactory.getInstance().deleteSession(session);
			}
			break;
			
		}
	}

	/**
	 * step event, entry, exit
	 * persist session if property says so
	 * persist session meta if property says so
	 * persist user log if property says so, this default flow saver only persist step entry event
	 * persist step log if property says so, this default flow saver only persist step exit event
	 *  
	 * @param event
	 * @param session
	 * @param stepTransition
	 * @param execProperties
	 */
	public void handleStepEvent(FlowEvent event, FlowSession session, IFlowStep flowStep, StepTransition stepTransition) {
		// noop
	}
	
}
