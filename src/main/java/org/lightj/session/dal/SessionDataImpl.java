package org.lightj.session.dal;

import java.util.Date;

import org.lightj.dal.LocatorUtil;
import org.lightj.session.FlowResult;
import org.lightj.session.FlowState;
import org.lightj.util.StringUtil;


/**
 * session data
 * @author biyu
 *
 */
public class SessionDataImpl implements ISessionData {
	
	static String TABLENAME	=	"FLOW_SESSION";
	
	private long sessionId;
	private String flowKey;
	private Date creationDate;
	private Date endDate;
	private String targetKey;
	private String type;
	private long parentId;
	private String status;

	private String currentAction;
	private String nextAction;
	private String actionStatus;
	private String resultStatus;
	private Date lastModified;
	private String runBy;

	private String requesterKey;

	public String getRequesterKey(){
		return requesterKey;
	}
	public void setRequesterKey(String requesterKey){
		this.requesterKey = requesterKey;
	}
	public String getKeyofRequester() {
		if (!StringUtil.isNullOrEmpty(requesterKey)) {
			String[] tokens = requesterKey.split(":");
			if (tokens.length >= 2) return tokens[1];
			else return tokens[0];
		}
		return null;
	}
	public String getRunBy() {
		return runBy;
	}
	public void setRunBy(String runBy) {
		this.runBy = runBy;
	}
	public String getActionStatus() {
		return actionStatus;
	}
	public void setActionStatus(String actionStatus) {
		this.actionStatus = actionStatus;
	}
	public String getCurrentAction() {
		return currentAction;
	}
	public void setCurrentAction(String currentAction) {
		this.currentAction = currentAction;
	}
	public String getNextAction() {
		return nextAction;
	}
	public void setNextAction(String nextAction) {
		this.nextAction = nextAction;
	}
	public String getResultStatus() {
		return resultStatus;
	}
	public void setResultStatus(String resultStatus) {
		this.resultStatus = resultStatus;
	}
	public long getFlowId() {
		return sessionId;
	}
	public void setFlowId(long i) {
		sessionId = i;
	}

	public SessionDataImpl() {}

	////////////// IData //////////////
	public long getPrimaryKey() {
		return sessionId;
	}
	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date date) {
		creationDate = date;
	}
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date date) {
		endDate = date;
	}
	public String getTargetKey() {
		return targetKey;
	}
	public void setTargetKey(String componentKey) {
		this.targetKey = componentKey;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public long getParentId() {
		return parentId;
	}
	public void setParentId(long i) {
		parentId = i;
	}
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = StringUtil.trimToLength(status, 255);
	}
	public String getStatusDesc() {
		if (FlowState.Completed.name().equalsIgnoreCase(getActionStatus())) {
			return getResultStatus();
		}
		else {
			return getActionStatus();
		}
	}
	public String getKeyOfTarget() {
		return LocatorUtil.getLocatableKey(targetKey);
	}
	public FlowState getFlowState() {
		return FlowState.valueOf(actionStatus);
	}
	public FlowResult getFlowResult() {
		try {
			if (resultStatus==null) return FlowResult.Unknown;
			return FlowResult.valueOf(resultStatus);
		}
		catch (IllegalArgumentException e) {
			return FlowResult.Unknown;
		}
	}
	public void setFlowState(FlowState actionStatus) {
		setActionStatus(actionStatus.name());
	}
	public void setFlowResult(FlowResult resultStatus) {
		setResultStatus(resultStatus.name());
	}
	public String getFlowKey() {
		return flowKey;
	}
	public void setFlowKey(String key) {
		this.flowKey = key;
	}

}
