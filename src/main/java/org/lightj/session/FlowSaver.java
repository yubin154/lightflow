package org.lightj.session;

import java.util.Date;

import org.lightj.session.dal.ISessionStepLog;
import org.lightj.session.dal.SessionDataFactory;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepTransition;
import org.lightj.util.Log4jProxy;
import org.lightj.util.StringUtil;


@SuppressWarnings({"rawtypes", "unchecked"})
public class FlowSaver implements IFlowEventListener {
	
	static Log4jProxy logger = Log4jProxy.getInstance(FlowSaver.class);
	
	/**
	 * save step history for the current step
	 * @param session
	 * @param flowStep
	 * @param msg
	 * @param detail
	 */
	public static void persistStepHistory(FlowSession session, IFlowStep flowStep, String msg, String detail) {
		if (flowStep.getStepOptions().loggingEnabled()) {
			ISessionStepLog log = SessionDataFactory.getInstance().getStepLogManager().newInstance();
			log.setFlowId(session.getId());
			log.setCreationDate(new Date());
			log.setStepName(flowStep.getStepName());
			log.setResult(msg);
			log.setDetails(detail);
			SessionDataFactory.getInstance().getStepLogManager().queuedSave(log);
		}
	}
	
	/**
	 * save step history without step information
	 * @param session
	 * @param msg
	 * @param rst
	 */
	private void persistStepHistory(FlowSession session, String msg, String rst) {
		ISessionStepLog log = SessionDataFactory.getInstance().getStepLogManager().newInstance();
		log.setFlowId(session.getId());
		log.setStepName(session.getCurrentAction());
		log.setResult(rst);
		log.setDetails(msg);
		SessionDataFactory.getInstance().getStepLogManager().queuedSave(log);
	}

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
		persistStepHistory(session, "Exception " + t.getClass().getName() + ": " + t.getMessage(), "Failed");
	}

	/**
	 * flow event, start, complete
	 * @param event
	 * @param session
	 */
	public void handleFlowEvent(FlowEvent event, FlowSession session) {
		// persist log ALWAYS on flow event start/stop
		switch (event) {
		case start:
		case resume:
		case log:
			persistStepHistory(session, event.getLabel(), event.getLabel());
			break;
		case recover:
		case stop:
		case pause:
			persistStepHistory(session, (session.getStatus() != null ? session.getStatus() : event.getLabel()), event.getLabel());
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
    	String detail = stepTransition.getDetail();
    	boolean hasDetail = !StringUtil.isNullOrEmpty(detail);
    	FlowResult rst = stepTransition.getResultStatus();

    	// log to flow step log table
		switch (event) {
		case stepEntry:
			session.getSessionContext().addStep(flowStep);
			break;
		case log:
			if (hasMsg || hasDetail) persistStepHistory(session, flowStep, msg, detail);
			break;
		case stepBuild:
//			persistStepHistory(session, flowStep, msg, detail);
			break;
		case stepExit:
			session.getSessionContext().setStepComplete(flowStep.getStepId());
			session.getSessionContext().prepareSave();
        	persistStepHistory(session, flowStep, hasMsg ? msg : event.getLabel(), hasDetail ? detail : (rst != null ? rst.name() : null));
        	break;
        default:
			if (hasMsg && hasDetail) {
	        	persistStepHistory(session, flowStep, msg, hasDetail ? detail : (rst!=null ? rst.name() : null));
			}
		}
        // compare log level of result status and log it as session status if needed
        if (rst!=null && hasMsg) {
        	FlowResult currentWfRst = session.getResult();
        	if (currentWfRst == null || rst.logLevel().isGreaterOrEqual(currentWfRst.logLevel())) {
				persistMsgInSession(session, msg);
        	}
        }
	}
	
}
