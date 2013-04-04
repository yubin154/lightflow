package org.lightj.dal;

public class MongoDatabaseType extends BaseDatabaseType {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2484832227417069854L;
	
	private final String dbName;
	private final String[] mongoHosts;
	private final int mongoPort;

	protected MongoDatabaseType(String name, String dbName, String[] mongoHosts, int mongoPort) {
		super(name, null);
		this.dbName = dbName;
		this.mongoHosts = mongoHosts;
		this.mongoPort = mongoPort;
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

}
