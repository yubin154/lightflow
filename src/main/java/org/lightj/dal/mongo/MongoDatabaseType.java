package org.lightj.dal.mongo;

import java.util.ArrayList;
import java.util.List;

import org.lightj.dal.BaseDatabaseType;
import org.lightj.initialization.InitializationException;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.Mongo;
import com.mongodb.ServerAddress;

/**
 * mongo database
 * @author biyu
 *
 */
public final class MongoDatabaseType extends BaseDatabaseType {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2484832227417069854L;
	
	private MongoTemplate template = null;

	private final String dbName;
	private final String[] mongoHosts;
	private final int mongoPort;

	public MongoDatabaseType(String name, String dbName, String[] mongoHosts, int mongoPort) {
		super(name, new MongoSequencerTypeImpl());
		this.dbName = dbName;
		this.mongoHosts = mongoHosts;
		this.mongoPort = mongoPort;
		this.setShared(true);
	}

	public String getDbName() {
		return dbName;
	}
	public String[] getMongoHosts() {
		return mongoHosts;
	}
	public int getMongoPort() {
		return mongoPort;
	}	

	public MongoTemplate mongoTemplate() {
		return template;
	}

	@Override
	public synchronized void initialize() {
		if (template == null) {
			Mongo mongo = null;
			
			try {
				List<ServerAddress> addrs = new ArrayList<ServerAddress>();
				for (String mongoHost : mongoHosts) {
					 addrs.add( new ServerAddress( mongoHost , mongoPort ) );
				}
				mongo = new Mongo(addrs);
		
			} catch (Exception e) {
				LoggerFactory.getLogger(BaseMongoDao.class).error("ERROR connecting to mongo DB..!",e);
				throw new InitializationException(e);
			}
			template = new MongoTemplate(mongo, dbName);
		}
	}

}
