package org.lightj.locking.dal;

import java.util.Date;

import org.lightj.dal.mongo.BaseEntity;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ObjectLock")
public class MongoObjectLockDO extends BaseEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3995087495542188156L;

	private long lockId;
	private String lockKey; //unique key in the database
	private Date createDate = new Date();
	private Date lastModifiedDate = new Date();
	private int lockCount;
	public long getLockId() {
		return lockId;
	}
	public void setLockId(long lockId) {
		this.lockId = lockId;
	}
	public String getLockKey() {
		return lockKey;
	}
	public void setLockKey(String lockKey) {
		this.lockKey = lockKey;
	}
	public Date getCreateDate() {
		return createDate;
	}
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}
	public void setLastModifiedDate(Date lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}
	public int getLockCount() {
		return lockCount;
	}
	public void setLockCount(int lockCount) {
		this.lockCount = lockCount;
	}

}
