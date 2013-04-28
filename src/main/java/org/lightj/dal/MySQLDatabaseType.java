package org.lightj.dal;

public class MySQLDatabaseType extends RdbmsDatabaseType {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7580917345853269395L;
	
	static final String driverKlassName = "com.mysql.jdbc.Driver";

	public MySQLDatabaseType(String name, String url, String username, String password) {
		super(name, new MySQLSequenerTypeImpl());
		this.setDriverClass(driverKlassName);
		this.setUrl(url);
		this.setUn(username);
		this.setPwd(password);
		this.setShared(true);
	}

}
