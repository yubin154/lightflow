package org.lightj.locking;

import java.util.Date;

import org.lightj.dal.BaseDatabaseType;
import org.lightj.dal.ITransactional;
import org.lightj.dal.TransactionManager;
import org.lightj.locking.dal.ObjectLockDAO;
import org.lightj.locking.dal.ObjectLockDO;
import org.lightj.util.Log4jProxy;

/**
 * default global semaphore impl
 * @author biyu
 *
 */
public class LockManagerImpl implements ILockManager {
	
	static Log4jProxy logger = Log4jProxy.getInstance(LockManagerImpl.class);
	
	private final ObjectLockDAO dao;

	public LockManagerImpl(BaseDatabaseType db) {
		this.dao = new ObjectLockDAO(db);
	}
	
	public ObjectLockDAO getDao() {
		return dao;
	}

	/**
	 * This method will synchronize your code globally on an object.
	 * 
	 * The way synchronizeObject works is that it will lock a object across VMs in the database
	 * and will hold the lock until the transaction passed to it is not finished. 
	 * 
	 * @param semaphore
	 * @param context
	 * @throws Exception
	 */
	public final void synchronizeObject(final String semaphoreKey, final ITransactional context) 
			throws RuntimeException, LockException 
	{		
		//create the row for SemaphoreDO
		final ObjectLockDO semaphoreDO = new ObjectLockDO();
		semaphoreDO.setLockKey(semaphoreKey);
		semaphoreDO.setCreateDate(new Date());
		semaphoreDO.setLastModifiedDate(new Date());
		semaphoreDO.setLockKey(semaphoreKey);

		/*This code will only work on Site controller database*/
		try{
			TransactionManager.execute(dao.getDbEnum(), new ITransactional() {
				
				public void execute() throws Throwable {
					/* TRANSACTION */
					logger.debug("Trying to acquire semaphore on " + " Thread id = " + Thread.currentThread());
					dao.aquireSemaphore(semaphoreDO);
					logger.debug("Acquired semaphore on " + " Thread id = " + Thread.currentThread());

					/*Inner transaction from another database enum.*/
					//call another TransactionalManager. - always executed against the site database.
					logger.debug("Executing Critical Section " + " Thread id = " + Thread.currentThread());
					context.execute();
					logger.debug("Execution of Critical Section completed, removing lock on semaphore for Thread id = " + Thread.currentThread());
					/*Transaction will automatically be committed.*/
				}
				
			});
			
		} catch (Throwable t) {
			throw new LockException(t);
		}
	}
	@Override
	public void lock(final String targetKey, final boolean isExclusive) throws LockException {

		try{
			TransactionManager.execute(dao.getDbEnum(), new ITransactional() {
				
				public void execute() throws Throwable {
					/* TRANSACTION */
					logger.debug("Trying to acquire lock on " + targetKey + " Thread id = " + Thread.currentThread());
					int lockAcquired = dao.lock(targetKey, isExclusive);
					if (lockAcquired != 1) throw new LockException("Failed to acquire lock, " + lockAcquired + " row affected");
					logger.debug("Acquired lock on " + targetKey + " Thread id = " + Thread.currentThread());
				}
				
			});
			
		} catch (Throwable t) {
			throw new LockException(t);
		}

	}
	@Override
	public void unlock(final String targetKey) throws LockException {

		try{
			TransactionManager.execute(dao.getDbEnum(), new ITransactional() {
				
				public void execute() throws Throwable {
					/* TRANSACTION */
					logger.debug("Trying to acquire lock on " + targetKey + " Thread id = " + Thread.currentThread());
					int lockAcquired = dao.unlock(targetKey);
					if (lockAcquired != 1) throw new LockException("Failed to acquire lock, " + lockAcquired + " row affected");
					logger.debug("Acquired lock on " + targetKey + " Thread id = " + Thread.currentThread());
				}
				
			});
			
		} catch (Throwable t) {
			throw new LockException(t);
		}

		
	}

	@Override
	public int getLockCount(String targetKey) {
		return dao.getLockCount(targetKey);
	}
	
}