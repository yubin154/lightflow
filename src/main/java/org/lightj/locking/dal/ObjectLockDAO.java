package org.lightj.locking.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.lightj.dal.AbstractDAO;
import org.lightj.dal.BaseDatabaseType;
import org.lightj.dal.BaseSequenceEnum;
import org.lightj.dal.ConnectionHelper;
import org.lightj.dal.DataAccessException;
import org.lightj.dal.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ObjectLockDAO extends AbstractDAO<ObjectLockDO> implements IObjectLockDAO {
	
	static Logger logger = LoggerFactory.getLogger(ObjectLockDAO.class);

	public ObjectLockDAO(BaseDatabaseType dbEnum) {
		super();
		register(ObjectLockDO.class, ObjectLockDO.TABLENAME, dbEnum, BaseSequenceEnum.SEQ_OBJECTLOCK,
				new String[] { "lock_id","lock_key", "lock_count", "create_date", "last_modified_date" },
				new String[] { "lockId", "lockKey", "lockCount", "createDate", "lastModifiedDate" });
	}
	
	static final String SELECT_FORUPDATE = String.format("SELECT * FROM %s l WHERE l.lock_key=?", ObjectLockDO.TABLENAME);
	static final String LOCK_EXCLUSIVE = String.format("UPDATE %s SET lock_count=lock_count+1 WHERE lock_key=? AND lock_count=0", ObjectLockDO.TABLENAME);
	static final String UNLOCK = String.format("UPDATE %s SET lock_count=lock_count-1 WHERE lock_key=? AND lock_count>=1", ObjectLockDO.TABLENAME);
	
	/**
	 * lock a key, has to execute in a transaction context
	 * @param lockDb
	 * @param lockKey
	 * @param isExclusive
	 * @return
	 * @throws DataAccessException
	 * @throws SQLException
	 */
	public int lock(final String lockKey) throws DataAccessException
	{
		Connection con = null;
		PreparedStatement stmt = null;
		PreparedStatement lockStmt = null;
		ResultSet rs = null;
		try {
			con = ConnectionHelper.getConnection(getDbEnum());
			stmt = con.prepareStatement(SELECT_FORUPDATE);
			stmt.setString(1, lockKey);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				// no row exist, create one
				ObjectLockDO lock = new ObjectLockDO();
				lock.setLockKey(lockKey);
				lock.setLockCount(0);
				insert(lock);
			}
			lockStmt = con.prepareStatement(LOCK_EXCLUSIVE);
			lockStmt.setString(1, lockKey);
			int rowAffected = lockStmt.executeUpdate();
			return rowAffected;
			
		} catch (SQLException e){
			throw new DataAccessException("Failed to acquire lock on " + lockKey);
		} finally{
			ConnectionHelper.cleanupDBResources(null, lockStmt, null);
			ConnectionHelper.cleanupDBResources(rs, stmt, con);
		}
	}

	/**
	 * unlock a key, has to execute in a transaction context
	 * @param lockDb
	 * @param lockKey
	 * @return
	 * @throws DataAccessException
	 */
	public int unlock(final String lockKey)
	{
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = ConnectionHelper.getConnection(getDbEnum());
			stmt = con.prepareStatement(UNLOCK);
			stmt.setString(1, lockKey);
			return stmt.executeUpdate();
			
		} catch (SQLException e){
			logger.error(e.getMessage(), e);
		} finally{
			ConnectionHelper.cleanupDBResources(rs, stmt, con);
		}
		return 0;
	}

	/**
	 * lock
	 * @param semaphoreDO
	 * @throws DataAccessException
	 * @throws SQLException
	 */
	public void aquireSemaphore(final String semaphore) throws DataAccessException {
		//Connection conn = ConnectionHelper.getConnection(this.getDbEnum());
		int numberOfRowsLocked = aquire (semaphore);
		logger.debug(" numberOfRowsLocked returned when trying to lock the semaphore : " + numberOfRowsLocked);
		
		if (numberOfRowsLocked == 0 ) {
			// SYNC Condition: all the threads will try to commit at the same time.
			// which means that if there are 4 threads , 1 commit will succeed and 3 will fail, 
			// in that case we will want to go aquire the lock again, so basically catch the exception 
			// and try to aquire the lock until we get it.
			Connection con = null;
			try{
				con = ConnectionHelper.getConnection(getDbEnum());
				ObjectLockDO semaphoreDO = new ObjectLockDO();
				semaphoreDO.setLockKey(semaphore);
				insert(semaphoreDO); 
				con.commit();
				//commitTr(this.getDbEnum());
				//
			}catch (Exception e){
				logger.debug("Insert commit failed.This might be expected, this is the first time we are locking a semaphore on this object key with many threads");
				if (con != null) {
					ConnectionHelper.rollbackTr(null, null, con);
				}
				throw new DataAccessException(e);
			} finally {
				ConnectionHelper.cleanupDBResources(null, null, con);
			}
			
			numberOfRowsLocked = aquire(semaphore);
			
			logger.debug("Trying to acquire the lock for the second time numberOfRowsLocked returned when trying to lock the semaphore : " + numberOfRowsLocked);
			if (numberOfRowsLocked == 0 ){
				throw new DataAccessException ("I was not able to aquire the semaphore");
			}
		}
	}
	
	/**
	 * lock count
	 * @param targetKey
	 * @return
	 * @throws DataAccessException
	 */
	public int getLockCount(String targetKey) {
		ObjectLockDO data = new ObjectLockDO();
		try {
			initUnique(data, "lock_key", targetKey);
		} catch (DataAccessException e) {
			return -1;
		}
		return data.getPrimaryKey() > 0 ? data.getLockCount() : -1;
	}
	
	/**
	 * Select for update happens right here.
	 * */
	@SuppressWarnings("rawtypes")
	private int aquire(String semaphore) throws DataAccessException{

		Query query = new Query();
		query.select("*");
		query.from(ObjectLockDO.TABLENAME, "l");
		query.or("l.lock_key","=",semaphore);

		StringBuffer sql = new StringBuffer();
		if (query.isFullQuery()) {
			sql.append(query.toString());
		} 
		else  {
			sql.append("SELECT * FROM ").append(this.tableName).append(query.daoString());
		}
		sql.append(" ").append("FOR UPDATE");

		logger.debug(this.getClass().getName() + ".search(...) is executing " + query.debugString());
		
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final List args = query.getArgs();
		int counter = 0;
		try {
			con = ConnectionHelper.getConnection(getDbEnum());
			stmt = con.prepareStatement(sql.toString());
			for (int i = 1, len = args.size(); i <= len; i++) {
				stmt.setObject(i, args.get(i-1));
			}
			rs = stmt.executeQuery();
			if (rs.next()) 		counter++;
			
		}catch (SQLException e){
			throw new DataAccessException("Failed to acquire lock on the semaphore ");
		}finally{
			ConnectionHelper.cleanupDBResources(rs, stmt, con);
		}

		return counter;
	}
	
	private static final Integer MY_NULL_INTEGER = Integer.valueOf(-1);  
	@SuppressWarnings("rawtypes")
	protected Object getNullValue(Class colType) {
		if (colType == Integer.class || colType == int.class) {
			return MY_NULL_INTEGER;
		}
		else {
			return super.getNullValue(colType);
		}
	}
}
