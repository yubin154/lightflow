package org.lightj.dal.mongo;

import java.util.List;

import org.lightj.dal.DataAccessException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * base mongo dao
 * @author biyu
 *
 * @param <T>
 */
public class BaseMongoDao<T extends BaseEntity> {
	
	protected MongoDatabaseType database;
	private final Class<T> klazz;
	
	/** constructor */
	public BaseMongoDao(Class<T> klazz) {
		this.klazz = klazz;
	}
	
	/** save document */
	public void save(T data) throws DataAccessException {
		beforeSave(data);
		getTemplate().save(data);
		afterSave(data);
	}

	/** save document */
	public void upsert(Query query, Update update) throws DataAccessException {
		getTemplate().upsert(query, update, klazz);
	}

	/** delete doc */
	public void delete(T data) throws DataAccessException {
		beforeDelete(data);
		getTemplate().remove(data);
		afterDelete(data);
	}
	
	/** find by a key */
	public T findByKey(String key, Object value) {
		return getTemplate().findOne(new Query(Criteria.where(key).is(value)), klazz);
	}
	
	/** search by query */
	public List<T> search(Query query) {
		return getTemplate().find(query, klazz);
	}
	
	/** get mongo template */
	public final MongoOperations getTemplate() {
		return database.mongoTemplate();
	}

	public MongoDatabaseType getDatabase() {
		return database;
	}

	public void setDatabase(MongoDatabaseType database) {
		this.database = database;
	}

	public Query newQuery() {
		return new Query();
	}
	
	/**
	 * before save
	 * @param data
	 * @throws DataAccessException
	 */
	protected void beforeSave(T data) throws DataAccessException {}
	
	/**
	 * after save
	 * @param data
	 * @throws DataAccessException
	 */
	protected void afterSave(T data) throws DataAccessException {}
	
	/**
	 * before delete
	 * @param data
	 * @throws DataAccessException
	 */
	protected void beforeDelete(T data) throws DataAccessException {}
	
	/**
	 * after delete hook
	 * @param data
	 * @throws DataAccessException
	 */
	protected void afterDelete(T data) throws DataAccessException {}

}
