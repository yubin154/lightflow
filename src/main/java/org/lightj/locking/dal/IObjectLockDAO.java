package org.lightj.locking.dal;

import org.lightj.dal.BaseDatabaseType;
import org.lightj.dal.DataAccessException;

public interface IObjectLockDAO {

	public int lock(final String lockKey) throws DataAccessException;

	public int unlock(final String lockKey);

	public void aquireSemaphore(String semaphore) throws DataAccessException;
	
	public int getLockCount(String targetKey);
	
	public BaseDatabaseType getDbEnum();
	
}
