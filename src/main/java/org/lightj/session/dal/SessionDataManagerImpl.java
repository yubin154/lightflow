package org.lightj.session.dal;

import org.lightj.dal.AbstractDAO;
import org.lightj.dal.BaseSequenceEnum;
import org.lightj.dal.DataAccessException;
import org.lightj.dal.DataAccessRuntimeException;
import org.lightj.dal.Query;

/**
 * session data manager implementation
 * @author biyu
 *
 */
public class SessionDataManagerImpl extends AbstractDAO<SessionDataImpl> implements ISessionDataManager<SessionDataImpl, Query> {

	private static final SessionDataManagerImpl me = new SessionDataManagerImpl();

	public static final SessionDataManagerImpl getInstance() {
		return me;
	}

	private SessionDataManagerImpl() {
		super();
		register(SessionDataImpl.class, SessionDataImpl.TABLENAME, null, BaseSequenceEnum.SEQ_FLOW_ID,
		new String[] {"flow_id", "flow_key", "creation_date","end_date", "flow_status","target","flow_type", "parent_id",
			"current_action", "next_action", "flow_state", "flow_result", "last_modified", "run_by","requester"},
		new String[] {"flowId", "flowKey", "creationDate", "endDate", "status", "targetKey", "type", "parentId",
			"currentAction", "nextAction", "actionStatus", "resultStatus", "lastModified", "runBy","requesterKey"} 
		);
	}

	/**
	 * create new instance of session data
	 */
	public SessionDataImpl newInstance() throws DataAccessRuntimeException {
		try {
			return doKlass.newInstance();
		} catch (IllegalAccessException e) {
			throw new DataAccessRuntimeException(e);
		} catch (InstantiationException e) {
			throw new DataAccessRuntimeException(e);
		}
	}

	/**
	 * find by key
	 */
	public SessionDataImpl findByKey(String key)
			throws DataAccessException {
		SessionDataImpl data = null;
		try {
			data = doKlass.newInstance();
		}
		catch (Exception e) {
			logger.error("Exception finding by id " + doKlass.getName() + " because " + e.getMessage());
			throw new DataAccessException(e);
		}
		initUnique(data, "flow_key", key);
		return data;
	}

}
