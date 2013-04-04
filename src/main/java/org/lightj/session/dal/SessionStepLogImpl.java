package org.lightj.session.dal;

import java.util.Date;

/**
 * session step log
 * @author biyu
 *
 */
public class SessionStepLogImpl implements ISessionStepLog {

	public static final String TABLENAME = "FLOW_STEP_LOG";
	
	private long stepId;

	private long sessionId;

	private String stepName;

	private String result;

	private Date creationDate;

	private String details;
	
	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public long getPrimaryKey() {
		return stepId;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public long getFlowId() {
		return sessionId;
	}

	public void setFlowId(long sessionId) {
		this.sessionId = sessionId;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date startTime) {
		this.creationDate = startTime;
	}

	public long getStepId() {
		return stepId;
	}

	public void setStepId(long stepId) {
		this.stepId = stepId;
	}

	public String getStepName() {
		return stepName;
	}

	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

}
