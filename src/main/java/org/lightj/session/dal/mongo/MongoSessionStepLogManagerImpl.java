package org.lightj.session.dal.mongo;

import java.util.List;

import org.lightj.dal.BaseDatabaseType;
import org.lightj.dal.BaseSequenceEnum;
import org.lightj.dal.DataAccessException;
import org.lightj.dal.DataAccessRuntimeException;
import org.lightj.dal.mongo.BaseMongoDao;
import org.lightj.dal.mongo.MongoDatabaseType;
import org.lightj.session.dal.ISessionStepLogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * {@link ISessionStepLogManager} implementation
 * @author biyu
 *
 */
public class MongoSessionStepLogManagerImpl extends BaseMongoDao<MongoSessionStepLogImpl> 
		implements ISessionStepLogManager<MongoSessionStepLogImpl> 
{
	
	static Logger logger = LoggerFactory.getLogger(MongoSessionStepLogManagerImpl.class);

	public MongoSessionStepLogManagerImpl() {
		super(MongoSessionStepLogImpl.class);
	}
	
	@Override
	public MongoSessionStepLogImpl newInstance()
			throws DataAccessRuntimeException {
		return new MongoSessionStepLogImpl();
	}

	protected void beforeSave(MongoSessionStepLogImpl data) throws DataAccessException {
		if (data.getStepId() <= 0) {
			data.setStepId(database.getNextValue(BaseSequenceEnum.SEQ_FLOW_STEP_ID));
		}
	}
	
	protected void afterDelete(MongoSessionStepLogImpl data) {
		data.setStepId(0);
	}

	@Override
	public void queuedSave(MongoSessionStepLogImpl data) {
		try {
			this.save(data);
		} catch (DataAccessException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public List<MongoSessionStepLogImpl> findByFlowId(long flowId) throws DataAccessException {
		return super.search(new Query().addCriteria(Criteria.where("sessionId").is(flowId)));
	}

	@Override
	public void setDbEnum(BaseDatabaseType dbEnum) {
		setDatabase((MongoDatabaseType) dbEnum);
	}
	
}
