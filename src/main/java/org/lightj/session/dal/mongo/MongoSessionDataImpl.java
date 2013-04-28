package org.lightj.session.dal.mongo;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.lightj.dal.LocatorUtil;
import org.lightj.dal.mongo.BaseEntity;
import org.lightj.session.FlowResult;
import org.lightj.session.FlowState;
import org.lightj.session.dal.ISessionData;
import org.lightj.util.StringUtil;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "FlowSession")
public class MongoSessionDataImpl extends BaseEntity implements ISessionData {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 6461286753651262315L;

	@Indexed
	private String flowKey;
	
	private long flowId;
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
	
	/** flow context */
	private LinkedHashMap<String, MongoSessionMetaDataImpl> metas = new LinkedHashMap<String, MongoSessionMetaDataImpl>();

	public long getFlowId() {
		return flowId;
	}

	public void setFlowId(long sessionId) {
		this.flowId = sessionId;
	}

	public String getFlowKey() {
		return flowKey;
	}

	public void setFlowKey(String flowKey) {
		this.flowKey = flowKey;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getTargetKey() {
		return targetKey;
	}

	public void setTargetKey(String targetKey) {
		this.targetKey = targetKey;
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

	public void setParentId(long parentId) {
		this.parentId = parentId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	public String getActionStatus() {
		return actionStatus;
	}

	public void setActionStatus(String actionStatus) {
		this.actionStatus = actionStatus;
	}

	public String getResultStatus() {
		return resultStatus;
	}

	public void setResultStatus(String resultStatus) {
		this.resultStatus = resultStatus;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public String getRunBy() {
		return runBy;
	}

	public void setRunBy(String runBy) {
		this.runBy = runBy;
	}

	public String getRequesterKey() {
		return requesterKey;
	}

	public void setRequesterKey(String requesterKey) {
		this.requesterKey = requesterKey;
	}

	@Override
	public long getPrimaryKey() {
		return flowId;
	}

	@Override
	public String getKeyofRequester() {
		if (!StringUtil.isNullOrEmpty(requesterKey)) {
			String[] tokens = requesterKey.split(":");
			if (tokens.length >= 2) return tokens[1];
			else return tokens[0];
		}
		return null;
	}

	@Override
	public FlowState getFlowState() {
		return FlowState.valueOf(actionStatus);
	}

	@Override
	public void setFlowState(FlowState actionStatus) {
		setActionStatus(actionStatus.name());
	}

	@Override
	public FlowResult getFlowResult() {
		try {
			if (resultStatus==null) return FlowResult.Unknown;
			return FlowResult.valueOf(resultStatus);
		}
		catch (IllegalArgumentException e) {
			return FlowResult.Unknown;
		}
	}

	@Override
	public void setFlowResult(FlowResult resultStatus) {
		setResultStatus(resultStatus.name());
	}

	@Override
	public String getKeyOfTarget() {
		return LocatorUtil.getLocatableKey(targetKey);
	}

	public LinkedHashMap<String, MongoSessionMetaDataImpl> getMetas() {
		return metas;
	}

	public void setMetas(LinkedHashMap<String, MongoSessionMetaDataImpl> metas) {
		this.metas = metas;
	}
	
	public void addMeta(MongoSessionMetaDataImpl meta) {
		this.metas.put(meta.getName(), meta);
	}
	public void removeMeta(MongoSessionMetaDataImpl meta) {
		this.metas.remove(meta.getName());
	}
	public List<MongoSessionMetaDataImpl> getMetasAsList() {
		return new ArrayList<MongoSessionMetaDataImpl>(this.metas.values());
	}

}
