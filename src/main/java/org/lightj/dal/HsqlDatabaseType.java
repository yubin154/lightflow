package org.lightj.dal;

import java.sql.SQLException;

import org.lightj.initialization.ShutdownException;

/**
 * in memory database comes with lightj, it uses hsqldb
 * 
 * @author biyu
 * 
 */
public class HsqlDatabaseType extends BaseDatabaseType {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3180114079772758855L;

	static String driverKlassName = "org.hsqldb.jdbcDriver";
	static String urlPrefix = "jdbc:hsqldb:mem:";
	
	
	public HsqlDatabaseType(String name, String dbName, String username, String password) {
		super(name, new HsqldbSequencerTypeImpl("dual"));
		setDriverClass(driverKlassName);
		setUrl(urlPrefix + dbName);
		setUn(username);
		setPwd(password);
		setShared(false);
	}

	/**
	 * initialize
	 */
	public synchronized void initialize() {
		super.initialize();
		try {
			ConnectionHelper.executeUpdate(this, "drop table dual if exists");
			ConnectionHelper.executeUpdate(this, "create table dual (id integer)");
			ConnectionHelper.executeUpdate(this, "insert into dual values (1)");
		} 
		catch (SQLException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public synchronized void shutdown() {
		try {
			ConnectionHelper.executeUpdate(this, "drop table dual");
		} catch (SQLException e) {
			throw new ShutdownException(e);
		}
	}
}
