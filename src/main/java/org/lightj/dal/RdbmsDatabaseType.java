package org.lightj.dal;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class RdbmsDatabaseType extends BaseDatabaseType {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6440483781470041212L;

	protected RdbmsDatabaseType(String name, IDatabaseSequencerType sequenerType) {
		super(name, sequenerType);
	}

	private DataSource ds;
	
	/** information to create data source */
	private String driverClass;
	private String url;
	private String un;
	private String pwd;
	
	/** initialization */
	public synchronized void initialize() {
		if (driverClass != null) {
			this.ds = createDataSource();
			ConnectionHelper.initDataSource(this, ds);
		}
	}

	private DataSource createDataSource() {
		DriverManagerDataSource ds = new DriverManagerDataSource(url, un, pwd);
		ds.setDriverClassName(driverClass);
		return ds;
	}
	
	public String getDriverClass() {
		return driverClass;
	}

	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUn() {
		return un;
	}

	public void setUn(String un) {
		this.un = un;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

}
