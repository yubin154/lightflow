package org.lightj.dal;

public class OracleDatabaseType extends BaseDatabaseType {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1535982925247047356L;
	
	static final String driverKlassName = "oracle.jdbc.OracleDriver";

	public OracleDatabaseType(String name, String url, String username, String password) {
		super(name, new OracleSequencerTypeImpl());
		this.setDriverClass(driverKlassName);
		this.setUrl(url);
		this.setUn(username);
		this.setPwd(password);
		this.setShared(true);
	}

}
