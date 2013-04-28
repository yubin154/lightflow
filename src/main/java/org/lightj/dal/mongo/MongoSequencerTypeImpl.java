package org.lightj.dal.mongo;

import java.sql.Connection;

import org.lightj.dal.BaseDatabaseType;
import org.lightj.dal.BaseSequenceEnum;
import org.lightj.dal.DataAccessException;
import org.lightj.dal.IDatabaseSequencerType;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class MongoSequencerTypeImpl extends BaseMongoDao<MongoSequence> implements IDatabaseSequencerType {
	
	public MongoSequencerTypeImpl() {
		super(MongoSequence.class);
	}

    private long increaseCounter(BaseDatabaseType dbEnum, String counterName){
    	MongoDatabaseType mongodb = (MongoDatabaseType)dbEnum;
    	if(!mongodb.mongoTemplate().collectionExists(MongoSequence.class)) {
    		mongodb.mongoTemplate().createCollection(MongoSequence.class);
    	}
        Query query = new Query(Criteria.where("name").is(counterName));
        Update update = new Update().inc("sequence", 1);
        MongoSequence counter = mongodb.mongoTemplate().findAndModify(query, update, MongoSequence.class); // return old Counter object
        if(counter == null) {
        	MongoSequence sequence = new MongoSequence();
        	sequence.setName(counterName);
        	sequence.setSequence(1);
        	mongodb.mongoTemplate().insert(sequence);
        	return 1;
        } else {
        	return counter.getSequence();
        }
    }
    
	@Override
	public long getCurrentValue(Connection con, BaseSequenceEnum seqEnum)
			throws DataAccessException {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public long getCurrentValue(BaseDatabaseType dbEnum,
			BaseSequenceEnum seqEnum) throws DataAccessException {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public long getNextValue(Connection con, BaseSequenceEnum seqEnum)
			throws DataAccessException {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public long getNextValue(BaseDatabaseType dbEnum, BaseSequenceEnum seqEnum)
			throws DataAccessException {
		return increaseCounter(dbEnum, seqEnum.getName());
	}
}