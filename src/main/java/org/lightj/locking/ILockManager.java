package org.lightj.locking;

import org.lightj.dal.ITransactional;

public interface ILockManager {
	
	/** lock a target */
	public void lock(String targetKey) throws LockException;
	
	/** unlock a target */
	public void unlock(String targetKey) throws LockException;

	/** synchronize on a global semaphore */
	public void synchronizeObject(String semaphoreKey, ITransactional context) throws LockException;
	
	/** current count of lock */
	public int getLockCount(String targetKey);
}
