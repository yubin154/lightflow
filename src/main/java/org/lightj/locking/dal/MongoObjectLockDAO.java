package org.lightj.locking.dal;

import org.lightj.dal.BaseDatabaseType;
import org.lightj.dal.DataAccessException;
import org.lightj.dal.mongo.BaseMongoDao;
import org.lightj.dal.mongo.MongoDatabaseType;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class MongoObjectLockDAO extends BaseMongoDao<MongoObjectLockDO> implements IObjectLockDAO {
	
	
	public MongoObjectLockDAO(MongoDatabaseType db) {
		super(MongoObjectLockDO.class);
		this.setDatabase(db);
	}
	
    protected synchronized int increaseCounter(String lockKey) throws DataAccessException {
        Query query = new Query();
        Criteria criteria = Criteria.where("lockKey").is(lockKey).and("lockCount").is(0);
        query.addCriteria(criteria);
        Update update = new Update().inc("lockCount", 1);
        MongoObjectLockDO counter = database.mongoTemplate().findAndModify(query, update, MongoObjectLockDO.class); // return old Counter object
        if (counter == null) {
        	// now check if the counter already exist
        	MongoObjectLockDO lock = database.mongoTemplate().findOne(new Query().addCriteria(Criteria.where("lockKey").is(lockKey)), MongoObjectLockDO.class);
        	if (lock == null) {
        		lock = new MongoObjectLockDO();
        		lock.setLockKey(lockKey);
        		lock.setLockCount(1);
        		save(lock);
        		return 1;
        	}
        	else {
            	throw new DataAccessException("unable to lock");
        	}
        }
        else if (counter.getLockCount() < 0) {
        	throw new DataAccessException("unable to lock");
        }
        return counter.getLockCount()+1;
    }

    protected synchronized int decreaseCounter(String lockKey) {
        Query query = new Query();
        Criteria criteria = Criteria.where("lockKey").is(lockKey);
        criteria.and("lockCount").gt(0);
        query.addCriteria(criteria);
        Update update = new Update().inc("lockCount", -1);
        MongoObjectLockDO counter = database.mongoTemplate().findAndModify(query, update, MongoObjectLockDO.class); // return old Counter object
        if (counter != null) {
            return counter.getLockCount()-1;
        }
        return 0;
    }

    @Override
	public int lock(String lockKey) throws DataAccessException {
		return increaseCounter(lockKey);
	}

	@Override
	public int unlock(String lockKey) {
		return decreaseCounter(lockKey);
	}

	@Override
	public BaseDatabaseType getDbEnum() {
		return database;
	}

	@Override
	public int getLockCount(String targetKey) {
        Query query = new Query();
        Criteria criteria = Criteria.where("lockKey").is(targetKey);
        query.addCriteria(criteria);
        MongoObjectLockDO counter = database.mongoTemplate().findOne(query, MongoObjectLockDO.class); // return old Counter object
        if (counter != null) {
            return counter.getLockCount();
        }
        return 0;
	}

	@Override
	public void aquireSemaphore(String semaphore) throws DataAccessException {
		while (true) {
			try {
				lock(semaphore);
				break;
			} catch (DataAccessException e) {
				// ignore
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					// ignore
				}
			}
		}
	}
	
}
