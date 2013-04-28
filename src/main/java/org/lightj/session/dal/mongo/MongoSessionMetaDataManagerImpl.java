package org.lightj.session.dal.mongo;

import java.util.List;

import org.lightj.dal.BaseDatabaseType;
import org.lightj.dal.BaseSequenceEnum;
import org.lightj.dal.DataAccessException;
import org.lightj.dal.DataAccessRuntimeException;
import org.lightj.dal.mongo.BaseMongoDao;
import org.lightj.dal.mongo.MongoDatabaseType;
import org.lightj.session.dal.ISessionMetaDataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;

public class MongoSessionMetaDataManagerImpl extends BaseMongoDao<MongoSessionMetaDataImpl> implements
		ISessionMetaDataManager<MongoSessionMetaDataImpl, Query> {
	
	public MongoSessionMetaDataManagerImpl() {
		super(MongoSessionMetaDataImpl.class);
	}
	
	@Autowired
	private MongoSessionDataManagerImpl sessionDataManager;

	@Override
	public MongoSessionMetaDataImpl newInstance() throws DataAccessRuntimeException {
		return new MongoSessionMetaDataImpl();
	}

	@Override
	public void save(MongoSessionMetaDataImpl data) throws DataAccessException {
		if (data.getFlowMetaId() <= 0) {
			data.setFlowMetaId(database.getNextValue(BaseSequenceEnum.SEQ_FLOW_META_ID));
		}
		MongoSessionDataImpl sessionData = sessionDataManager.findById(data.getFlowId());
		sessionData.addMeta(data);
		sessionDataManager.save(sessionData);
	}
	
	@Override
	public void delete(MongoSessionMetaDataImpl data) throws DataAccessException {
		MongoSessionDataImpl sessionData = sessionDataManager.findById(data.getFlowId());
		sessionData.removeMeta(data);
		sessionDataManager.save(sessionData);
		data.setFlowMetaId(0);
	}
	
	@Override
	public List<MongoSessionMetaDataImpl> findByFlowId(long sessId)
			throws DataAccessException {
		MongoSessionDataImpl sessionData = sessionDataManager.findById(sessId);
		return sessionData.getMetasAsList();
	}

	@Override
	public void setDbEnum(BaseDatabaseType dbEnum) {
		setDatabase((MongoDatabaseType) dbEnum);
	}

	public MongoSessionDataManagerImpl getSessionDataManager() {
		return sessionDataManager;
	}

	public void setSessionDataManager(MongoSessionDataManagerImpl sessionDataManager) {
		this.sessionDataManager = sessionDataManager;
	}

}
