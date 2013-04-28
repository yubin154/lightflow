package org.lightj.locking;

import java.util.Date;

import org.lightj.dal.BaseDatabaseType;
import org.lightj.dal.DataAccessException;
import org.lightj.dal.ITransactional;
import org.lightj.dal.RdbmsDatabaseType;
import org.lightj.dal.TransactionManager;
import org.lightj.dal.mongo.MongoDatabaseType;
import org.lightj.locking.dal.IObjectLockDAO;
import org.lightj.locking.dal.MongoObjectLockDAO;
import org.lightj.locking.dal.ObjectLockDAO;
import org.lightj.locking.dal.ObjectLockDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * default global semaphore impl
 * @author biyu
 *
 */
public class LockManagerImpl implements ILockManager {
	
	static Logger logger = LoggerFactory.getLogger(LockManagerImpl.class);
	
	private static IObjectLockDAO objectLockDao;

	public LockManagerImpl(BaseDatabaseType db) {
		init(db);
	}
	
	private synchronized void init(BaseDatabaseType db) {
		if (objectLockDao == null) {
			if (db instanceof RdbmsDatabaseType) {
				objectLockDao = new ObjectLockDAO(db);
			} else if (db instanceof MongoDatabaseType) {
				objectLockDao = new MongoObjectLockDAO((MongoDatabaseType) db);
			} else {
				throw new IllegalArgumentException("not a valid database type " + db);
			}
		}
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
		if (objectLockDao.getDbEnum() instanceof RdbmsDatabaseType) {
			//create the row for SemaphoreDO
			final ObjectLockDO semaphoreDO = new ObjectLockDO();
			semaphoreDO.setLockKey(semaphoreKey);
			semaphoreDO.setCreateDate(new Date());
			semaphoreDO.setLastModifiedDate(new Date());
			semaphoreDO.setLockKey(semaphoreKey);

			/*This code will only work on Site controller database*/
			try{
				TransactionManager.execute(objectLockDao.getDbEnum(), new ITransactional() {
					
					public void execute() throws Throwable {
						/* TRANSACTION */
						logger.debug("Trying to acquire semaphore on " + " Thread id = " + Thread.currentThread());
						objectLockDao.aquireSemaphore(semaphoreKey);
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
		else if (objectLockDao.getDbEnum() instanceof MongoDatabaseType) {
			try {
				objectLockDao.aquireSemaphore(semaphoreKey);
			} catch (DataAccessException e1) {
				throw new LockException(e1);
			}
			try {
				context.execute();
			} catch (Throwable e) {
				throw new LockException(e);
			} finally {
				objectLockDao.unlock(semaphoreKey);
			}
		}
		
	}
	@Override
	public void lock(final String targetKey) throws LockException {

		if (objectLockDao.getDbEnum() instanceof RdbmsDatabaseType) {
			try{
				TransactionManager.execute(objectLockDao.getDbEnum(), new ITransactional() {
					
					public void execute() throws Throwable {
						/* TRANSACTION */
						logger.debug("Trying to acquire lock on " + targetKey + " Thread id = " + Thread.currentThread());
						int lockAcquired = objectLockDao.lock(targetKey);
						if (lockAcquired != 1) throw new LockException("Failed to acquire lock, " + lockAcquired + " row affected");
						logger.debug("Acquired lock on " + targetKey + " Thread id = " + Thread.currentThread());
					}
					
				});
				
			} catch (Throwable t) {
				throw new LockException(t);
			}
		}
		else if (objectLockDao.getDbEnum() instanceof MongoDatabaseType) {
			
			try {
				objectLockDao.lock(targetKey);
			} catch (DataAccessException e) {
				throw new LockException(e);
			}
		}

	}
	@Override
	public void unlock(final String targetKey) throws LockException {

		if (objectLockDao.getDbEnum() instanceof RdbmsDatabaseType) {
			try{
				TransactionManager.execute(objectLockDao.getDbEnum(), new ITransactional() {
					
					public void execute() throws Throwable {
						/* TRANSACTION */
						logger.debug("Trying to acquire lock on " + targetKey + " Thread id = " + Thread.currentThread());
						int lockAcquired = objectLockDao.unlock(targetKey);
						if (lockAcquired != 1) throw new LockException("Failed to acquire lock, " + lockAcquired + " row affected");
						logger.debug("Acquired lock on " + targetKey + " Thread id = " + Thread.currentThread());
					}
					
				});
				
			} catch (Throwable t) {
				throw new LockException(t);
			}
		}
		else if (objectLockDao.getDbEnum() instanceof MongoDatabaseType) {
			objectLockDao.unlock(targetKey);
		}
		
	}

	@Override
	public int getLockCount(String targetKey) {
		return objectLockDao.getLockCount(targetKey);
	}
	
}