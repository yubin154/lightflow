package org.lightj.session.dal;

import java.util.Date;

import org.lightj.dal.IData;
import org.lightj.session.FlowResult;

/**
 * flow step log interface
 * @author biyu
 *
 */
public interface ISessionStepLog extends IData {

	/**
	 * step id
	 * @return
	 */
	public long getStepId();
	
	/**
	 * set step id
	 * @param stepId
	 */
	public void setStepId(long stepId);
	
	/**
	 * step details
	 * @return
	 */
	public String getDetails();
	
	/**
	 * set step details
	 * @param details
	 */
	public void setDetails(String details);
	
	/**
	 * step result {@link FlowResult}
	 * @return
	 */
	public String getResult();
	
	/**
	 * set result {@link FlowResult}
	 * @param result
	 */
	public void setResult(String result);
	
	/**
	 * flow id
	 * @return
	 */
	public long getFlowId();
	
	/**
	 * set flow id
	 * @param sessionId
	 */
	public void setFlowId(long sessionId);
	
	/**
	 * creation date
	 * @return
	 */
	public Date getCreationDate();
	
	/**
	 * set creation date
	 * @param startTime
	 */
	public void setCreationDate(Date startTime);
	
	/**
	 * step name
	 * @return
	 */
	public String getStepName();
	
	/**
	 * set step name
	 * @param stepName
	 */
	public void setStepName(String stepName);

}
