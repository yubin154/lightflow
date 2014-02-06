package org.lightj.session.dal.mongo;

import org.lightj.dal.BaseDatabaseType;
import org.lightj.dal.BaseSequenceEnum;
import org.lightj.dal.DataAccessException;
import org.lightj.dal.DataAccessRuntimeException;
import org.lightj.dal.mongo.BaseMongoDao;
import org.lightj.dal.mongo.MongoDatabaseType;
import org.lightj.session.FlowResult;
import org.lightj.session.FlowState;
import org.lightj.session.FlowType;
import org.lightj.session.dal.ISessionData;
import org.lightj.session.dal.ISessionDataManager;
import org.lightj.util.StringUtil;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * mongo based flow_session dao
 * @author biyu
 *
 */
public class MongoSessionDataManagerImpl extends BaseMongoDao<MongoSessionDataImpl> implements
		ISessionDataManager<MongoSessionDataImpl, Query> 
{
	
	public MongoSessionDataManagerImpl() {
		super(MongoSessionDataImpl.class);
	}

	@Override
	public MongoSessionDataImpl newInstance() throws DataAccessRuntimeException {
		return new MongoSessionDataImpl();
	}
	
	protected void beforeSave(MongoSessionDataImpl data) throws DataAccessException {
		if (data.getFlowId() <= 0) {
			data.setFlowId(database.getNextValue(BaseSequenceEnum.SEQ_FLOW_ID));
		}
	}
	
	protected void afterDelete(MongoSessionDataImpl data) {
		data.setFlowId(0);
	}

	@Override
	public MongoSessionDataImpl findById(long id) throws DataAccessException {
		return super.findByKey("flowId", id);
	}

	@Override
	public MongoSessionDataImpl findByKey(String key)
			throws DataAccessException {
		return super.findByKey("flowKey", key);
	}

	@Override
	public void setDbEnum(BaseDatabaseType dbEnum) {
		setDatabase((MongoDatabaseType) dbEnum);
	}

	@Override
	public Query queryActiveChildFlows(long parentId) {
		Query q = newQuery();
		q.addCriteria(Criteria.where("endDate").is(null).and("actionStatus")
				.in(FlowState.Running.name(), FlowState.Callback.name()).and("parentId").is(parentId));
		return q;
	}

	@Override
	public Query queryIncompleteChildFlows(long parentId) {
		Query q = newQuery();
		q.addCriteria(Criteria.where("parentId").is(parentId).and("endDate").is(null));
		return q;
	}

	@Override
	public Query queryIncompleteSessionsLike(ISessionData me) {
		Query query = newQuery();
		Criteria criteria = Criteria.where("type").is(me.getType()).and("endDate").is(null);
		if (me.getParentId() > 0) {
			criteria.and("flowId").ne(me.getParentId());
		}
		// don't count myself
		if (me.getFlowId() > 0) {
			criteria.and("flowId").ne(me.getFlowId());
		}
		// on the same component
		if (!StringUtil.isNullOrEmpty(me.getTargetKey())) {
			criteria.and("targetKey").is(me.getTargetKey());
		}
		query.addCriteria(criteria);
		return query;
	}

	@Override
	public Query queryFlows(FlowType wfType, FlowState wfState,
			FlowResult wfStatus, String targetKey) {
		Query query = newQuery();
		Criteria criteria = new Criteria();
		if (wfType != null) {
			criteria.and("type").is(wfType.value());
		}
		if (wfState != null) {
			criteria.and("actionStatus").is(wfState.name());
		}
		if (wfStatus != null) {
			criteria.and("resultStatus").is(wfStatus.name());
		}
		if (targetKey != null) {
			criteria.and("targetKey").is(targetKey);
		}
		query.addCriteria(criteria);
		return null;
	}

	@Override
	public Query queryActiveFlows(String runBy) {
		Query q = newQuery();
		q.addCriteria(Criteria.where("endDate").is(null)
				.and("runBy").is(runBy).and("actionStatus").in(
						FlowState.Running.name(),
						FlowState.Callback.name()).and("parentId")
				.is(null));
		return q;
	}

}
