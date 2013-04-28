package org.lightj.session.dal.mongo;

import org.lightj.dal.BaseDatabaseType;
import org.lightj.dal.BaseSequenceEnum;
import org.lightj.dal.DataAccessException;
import org.lightj.dal.DataAccessRuntimeException;
import org.lightj.dal.mongo.BaseMongoDao;
import org.lightj.dal.mongo.MongoDatabaseType;
import org.lightj.session.dal.ISessionDataManager;
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

}
