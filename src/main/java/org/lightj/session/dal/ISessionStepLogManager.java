package org.lightj.session.dal;

import java.util.List;

import org.lightj.dal.BaseDatabaseType;
import org.lightj.dal.DataAccessException;
import org.lightj.dal.DataAccessRuntimeException;

/**
 * flow step log manager interface
 * @author biyu
 *
 * @param <T>
 */
public interface ISessionStepLogManager<T extends ISessionStepLog> {

	/**
	 * get new instance
	 * @return
	 * @throws DataAccessRuntimeException
	 */
	public T newInstance() throws DataAccessRuntimeException;
	
	/**
	 * save step
	 * @param data
	 * @throws DataAccessException
	 */
	public void save(T data) throws DataAccessException;
	
	/**
	 * asynchronous save
	 * @param data
	 */
	public void queuedSave(T data);
	
	/**
	 * delete step
	 * @param data
	 * @throws DataAccessException
	 */
	public void delete(T data) throws DataAccessException;
	
	/**
	 * find all logs by flow id
	 * @param flowId
	 * @return
	 * @throws DataAccessException
	 */
	public List<T> findByFlowId(long flowId) throws DataAccessException;
	
	/**
	 * associate with data store
	 * @param dbEnum
	 */
	public void setDbEnum(BaseDatabaseType dbEnum);
	
}
