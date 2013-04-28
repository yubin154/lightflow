package org.lightj.session.dal.mongo;

import java.io.Serializable;

import org.lightj.dal.mongo.BaseEntity;
import org.lightj.session.dal.ISessionMetaData;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "FlowSessionMeta")
public class MongoSessionMetaDataImpl extends BaseEntity implements ISessionMetaData {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3785901164767044458L;

	// not for persistence, just a flag to mark for update
	private boolean isDirty = false;
	
	private long sessionMetaId;
	@Indexed
	private long sessionId;
	private String name;
	private String strValue;
	private Serializable blobValue;

	public long getPrimaryKey() {
		return sessionMetaId;
	}
	public Serializable getBlobValue() {
		return blobValue;
	}
	public void setBlobValue(Serializable blobValue) {
		this.blobValue = blobValue;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getFlowId() {
		return sessionId;
	}
	public void setFlowId(long sessionId) {
		this.sessionId = sessionId;
	}
	public String getStrValue() {
		return strValue;
	}
	public void setStrValue(String strValue) {
		this.strValue = strValue;
	}
	public long getFlowMetaId() {
		return sessionMetaId;
	}
	public void setFlowMetaId(long sessionMetaId) {
		this.sessionMetaId = sessionMetaId;
	}
	public boolean isDirty() {
		return isDirty;
	}
	public void setDirty(boolean isDirty) {
		this.isDirty = isDirty;
	}

}
