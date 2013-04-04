package org.lightj.locking.dal;

import java.util.Date;

import org.lightj.dal.IData;


public class ObjectLockDO implements IData {
	public static final String TABLENAME = "OBJECT_LOCK";

	private long lockId;
	
	private String lockKey; //unique key in the database

	private Date createDate = new Date();
	
	private Date lastModifiedDate = new Date();
	
	private int lockCount;
	
	public long getPrimaryKey() {
		return lockId;
	}

	public static String getTABLENAME() {
		return TABLENAME;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date creationDate) {
		this.createDate = creationDate;
	}

	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(Date lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

	public long getLockId() {
		return lockId;
	}

	public void setLockId(long lockId) {
		this.lockId = lockId;
	}

	public String getLockKey() {
		return lockKey;
	}

	public void setLockKey(String semaphoreKey) {
		this.lockKey = semaphoreKey;
	}

	public int getLockCount() {
		return lockCount;
	}

	public void setLockCount(int lockCount) {
		this.lockCount = lockCount;
	}

	public String toString(){
		StringBuffer buf = new StringBuffer();
		buf.append(" semaphoreKey = " + lockKey );
		
		return buf.toString();
	}
}
