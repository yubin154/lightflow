package org.lightj.session.dal;

import java.util.Date;

import org.lightj.dal.IData;
import org.lightj.dal.Locatable;
import org.lightj.session.FlowResult;
import org.lightj.session.FlowState;
import org.lightj.session.FlowType;

/**
 * flow data interface
 * @author biyu
 *
 */
public interface ISessionData extends IData {

	/**
	 * requester locatable string eg. user123::com.xyz.UserBOF 
	 * @see {@link Locatable}
	 * @return
	 */
	public String getRequesterKey();
	
	/**
	 * set requester
	 * @param requesterKey
	 */
	public void setRequesterKey(String requesterKey);
	
	/**
	 * key of requester eg. user123
	 * @return
	 */
	public String getKeyofRequester();
	
	/**
	 * runner
	 * @return
	 */
	public String getRunBy();
	
	/**
	 * set runner
	 * @param runBy
	 */
	public void setRunBy(String runBy);
	
	/**
	 * flow state
	 * @return
	 */
	public FlowState getFlowState();
	
	/**
	 * set flow state
	 * @param actionStatus
	 */
	public void setFlowState(FlowState actionStatus);
	
	/**
	 * current action
	 * @return
	 */
	public String getCurrentAction();
	
	/**
	 * set current action
	 * @param currentAction
	 */
	public void setCurrentAction(String currentAction);
	
	/**
	 * next action/step
	 * @return
	 */
	public String getNextAction();
	
	/**
	 * set next action/step
	 * @param nextAction
	 */
	public void setNextAction(String nextAction);
	
	/**
	 * flow result {@link FlowResult}
	 * @return
	 */
	public FlowResult getFlowResult();
	
	/**
	 * set flow result {@link FlowResult}
	 * @param resultStatus
	 */
	public void setFlowResult(FlowResult resultStatus);
	
	/**
	 * flow id
	 * @return
	 */
	public long getFlowId();
	
	/**
	 * set flow id
	 * @param i
	 */
	public void setFlowId(long i);
	
	/**
	 * creation date
	 * @return
	 */
	public Date getCreationDate();
	
	/**
	 * set creation date
	 * @param date
	 */
	public void setCreationDate(Date date);
	
	/**
	 * end date
	 * @return
	 */
	public Date getEndDate();
	
	/**
	 * set end date
	 * @param date
	 */
	public void setEndDate(Date date);
	
	/**
	 * target locatable string eg. user123::com.xyz.UserBOF 
	 * @see org.lightj.dal.Locatable
	 * @return
	 */
	public String getTargetKey();
	
	/**
	 * set target {@link Locatable} string
	 * @param targetKey
	 */
	public void setTargetKey(String targetKey);
	
	/**
	 * key of target eg. user123
	 * @return
	 */
	public String getKeyOfTarget();
	
	/**
	 * flow type {@link FlowType}
	 * @return
	 */
	public String getType();
	
	/**
	 * set {@link FlowType}
	 * @param type
	 */
	public void setType(String type);
	
	/**
	 * parent flow id
	 * @return
	 */
	public long getParentId();
	
	/**
	 * set parent flow id
	 * @param i
	 */
	public void setParentId(long i);
	
	/**
	 * last modified date
	 * @return
	 */
	public Date getLastModified();
	
	/**
	 * set last modified date
	 * @param lastModified
	 */
	public void setLastModified(Date lastModified);
	
	/**
	 * user friendly status 
	 * @return
	 */
	public String getStatus();
	
	/**
	 * set user friendly flow status
	 * @param status
	 */
	public void setStatus(String status);
	
	/**
	 * flow key
	 * @param key
	 */
	public void setFlowKey(String key);
	
	/**
	 * flow key
	 * @return
	 */
	public String getFlowKey();

}
