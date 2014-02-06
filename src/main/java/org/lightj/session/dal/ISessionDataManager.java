package org.lightj.session.dal;

import java.util.List;

import org.lightj.dal.BaseDatabaseType;
import org.lightj.dal.DataAccessException;
import org.lightj.dal.DataAccessRuntimeException;
import org.lightj.session.FlowResult;
import org.lightj.session.FlowState;
import org.lightj.session.FlowType;

/**
 * session data manager interface
 * @author biyu
 *
 * @param <T>
 */
public interface ISessionDataManager<T extends ISessionData, Q> {

	/**
	 * create new instance of {@link ISessionData} 
	 * @return
	 * @throws DataAccessRuntimeException
	 */
	public T newInstance() throws DataAccessRuntimeException;
	
	/**
	 * create a new query
	 * @return
	 */
	public Q newQuery();
	
	/**
	 * save {@link ISessionData}
	 * @param data
	 * @throws DataAccessException
	 */
	public void save(T data) throws DataAccessException;
	
	/**
	 * delete {@link ISessionData}
	 * @param data
	 * @throws DataAccessException
	 */
	public void delete(T data) throws DataAccessException;
	
	/**
	 * find {@link ISessionData} by its id
	 * @param id
	 * @param fromCache
	 * @return
	 * @throws DataAccessException
	 */
	public T findById(long id) throws DataAccessException;
	
	/**
	 * find {@link ISessionData} by its key
	 * @param key
	 * @param fromCache
	 * @return
	 * @throws DataAccessException
	 */
	public T findByKey(String key) throws DataAccessException;
	
	/**
	 * find {@link ISessionData}s by query
	 * @param query
	 * @return
	 * @throws DataAccessException
	 */
	public List<T> search(Q query) throws DataAccessException;
	
	/**
	 * associate with a datastore 
	 * @param dbEnum
	 */
	public void setDbEnum(BaseDatabaseType dbEnum);
	
	
	public Q queryActiveChildFlows(long parentId);
	public Q queryIncompleteChildFlows(long parentId);
	public Q queryIncompleteSessionsLike(ISessionData me);
	public Q queryFlows(FlowType wfType, FlowState wfState, FlowResult wfStatus, String targetKey);
	public Q queryActiveFlows(String runBy);
}
