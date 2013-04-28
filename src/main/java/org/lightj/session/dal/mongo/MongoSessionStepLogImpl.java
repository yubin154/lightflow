package org.lightj.session.dal.mongo;

import java.util.Date;

import org.lightj.dal.mongo.BaseEntity;
import org.lightj.session.dal.ISessionStepLog;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "FlowStepLog")
public class MongoSessionStepLogImpl extends BaseEntity implements ISessionStepLog {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4706079351271102216L;
	
	private long stepId;
	@Indexed
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
