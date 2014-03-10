package org.lightj.session.dal.rdbms;

import org.lightj.dal.AbstractDAO;
import org.lightj.dal.BaseSequenceEnum;
import org.lightj.dal.DataAccessException;
import org.lightj.dal.DataAccessRuntimeException;
import org.lightj.dal.Query;
import org.lightj.session.FlowResult;
import org.lightj.session.FlowState;
import org.lightj.session.FlowType;
import org.lightj.session.dal.ISessionData;
import org.lightj.session.dal.ISessionDataManager;
import org.lightj.util.DBUtil;
import org.lightj.util.StringUtil;

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

	@Override
	public Query queryActiveChildFlows(long parentId) {
		Query q = newQuery();
		q.and("end_date is null")
		 .and("flow_state in (" + 
				DBUtil.dbC(FlowState.Running.name()) + ',' +
				DBUtil.dbC(FlowState.Callback.name())+ ")")
		 .and("parent_id", "=", parentId);
		return q;
	}

	@Override
	public Query queryIncompleteChildFlows(long parentId) {
		Query q = newQuery();
		q.and("parent_id", "=", parentId).and("end_date is null");
		return q;
	}

	@Override
	public Query queryIncompleteSessionsLike(ISessionData me) {
		Query query = newQuery();
		// only care about the same type
		query.and("flow_type", "=", me.getType());
		// don't count my parent
		if (me.getParentId() > 0) {
			query.and("flow_id", "!=", me.getParentId());
		}
		// don't count myself
		if (me.getFlowId() > 0) {
			query.and("flow_id", "!=", me.getFlowId());
		}
		// on the same component
		if (!StringUtil.isNullOrEmpty(me.getTargetKey())) {
			query.and("target", "=", me.getTargetKey());
		}
		// open session
		query.and("END_DATE IS NULL");
		return query;
	}

	@Override
	public Query queryFlows(FlowType wfType, FlowState wfState,
			FlowResult wfStatus, String targetKey) {
		Query query = newQuery(); 
		query.orderBy(" id desc");
		if (wfType != null) {
			query.and("flow_type", "=", wfType.value());
		}
		if (wfState != null) {
			query.and("flow_state", "=", wfState.name());
		}
		if (wfStatus != null) {
			query.and("flow_result", "=", wfStatus.name());
		}
		if (targetKey != null) {
			query.and("target", "=", targetKey);
		}
		return null;
	}

	@Override
	public Query queryActiveFlows(String runBy) {
		Query q = newQuery();
		q.and("end_date is null").and("run_by", "=", runBy)
			.and("flow_state in ("+ DBUtil.dbC(FlowState.Running.name())
						+ ','+ DBUtil.dbC(FlowState.Callback.name())
						+ ")").and("parent_id is null");
		return q;
	}

}
