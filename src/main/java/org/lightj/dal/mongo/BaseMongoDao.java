package org.lightj.dal.mongo;

import java.util.ArrayList;
import java.util.List;

import org.lightj.util.Log4jProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.Mongo;
import com.mongodb.ServerAddress;

public class BaseMongoDao {

	@Autowired(required=true)
	private MongoTemplate template = null;

	private String[] mongoHosts;
	private int mongoPort;
	private String dbName;
	
	public BaseMongoDao(String[] mongoHosts, int mongoPort, String dbName) {
		this.dbName = dbName;
		this.mongoHosts = mongoHosts;
		this.mongoPort = mongoPort;
	}
	
	
	protected MongoTemplate getTemplate() {
		if (template == null) {
			synchronized (BaseMongoDao.class) {
				if (template == null) {
					Mongo mongo = null;
			
					try {
						List<ServerAddress> addrs = new ArrayList<ServerAddress>();
						for (String mongoHost : mongoHosts) {
							 addrs.add( new ServerAddress( mongoHost , mongoPort ) );
						}
						mongo = new Mongo(addrs);
		
					} catch (Exception e) {
						Log4jProxy.getInstance(BaseMongoDao.class).error("ERROR connecting to mongo DB..!",e);
					}
					template = new MongoTemplate(mongo, dbName);
				}
			}
		}
		return template;
	}

	public void setTemplate(MongoTemplate temp) {
		this.template = temp;
	}
}
