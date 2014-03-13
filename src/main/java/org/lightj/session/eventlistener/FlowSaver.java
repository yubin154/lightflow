package org.lightj.session.eventlistener;

import org.lightj.session.FlowEvent;
import org.lightj.session.FlowResult;
import org.lightj.session.FlowSession;
import org.lightj.session.FlowSessionFactory;
import org.lightj.session.IFlowEventListener;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepTransition;
import org.lightj.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({"rawtypes"})
public class FlowSaver implements IFlowEventListener {
	
	static Logger logger = LoggerFactory.getLogger(FlowSaver.class);
	
	/**
	 * persist a msg in session.status column for easy access
	 * @param session
	 * @param msg
	 */
	private void persistMsgInSession(FlowSession session, String msg) {
		session.getSessionData().setStatus(msg);
	}

	/**
	 * error event
	 * @param t
	 * @param session
	 */
	public void handleError(Throwable t, FlowSession session) {
		session.getSessionContext().saveFlowError(StringUtil.getStackTrace(t, 2000));
		persistMsgInSession(session, t.getMessage());
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
		case pause:
			FlowSessionFactory.getInstance().removeSessionFromCache(session.getKey());
			break;
			
		}
		// persist meta if property says so
		if (session.getFlowProperties().clustered()) {
			FlowSessionFactory.getInstance().saveMeta(session);
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
    	String msg = stepTransition.getMsg(); 
    	boolean hasMsg = !StringUtil.isNullOrEmpty(msg);
    	FlowResult rst = stepTransition.getResultStatus();

    	// log to flow step log table
		switch (event) {
		case stepEntry:
			flowStep.getAndIncrementStepEntry();
			session.getSessionContext().addStep(flowStep);
			break;
		case stepBuild:
			break;
		case stepExit:
			session.getSessionContext().setStepComplete(flowStep.getStepId());
			session.getSessionContext().prepareSave();
        	break;
        default:
		}
        // compare log level of result status and log it as session status if needed
        if (rst!=null && hasMsg) {
        	FlowResult currentWfRst = session.getResult();
        	if (currentWfRst == null || rst.logLevel().intValue() >= currentWfRst.logLevel().intValue()) {
				persistMsgInSession(session, msg);
        	}
        }
	}
	
}
