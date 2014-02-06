package org.lightj.session;

import org.lightj.session.exception.FlowValidationException;
import org.lightj.session.exception.StateChangeException;


/**
 * interface for flow control
 * 
 * @author biyu
 *
 */
public interface IFlowControl {
	
	/**
	 * run a flow, asynchronously
	 * @throws StateChangeException
	 */
	public void runFlow() throws StateChangeException;
	
	/**
	 * pause a flow
	 * @param resultStatus
	 * @param message
	 * @throws StateChangeException
	 */
	public void pauseFlow(FlowResult resultStatus, String message) throws StateChangeException;

	/**
	 * stop a flow
	 * @param actionStatus
	 * @param resultStatus
	 * @param message
	 */
	public void stopFlow(FlowState actionStatus, FlowResult resultStatus, String message) throws StateChangeException;

	/**
	 * validate a session, make sure all its steps are validate
	 * @throws FlowValidationException
	 */
	public void validateSteps() throws FlowValidationException;

}
