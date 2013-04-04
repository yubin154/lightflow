package org.lightj.dal;

import java.sql.Connection;

import javax.sql.DataSource;

import org.lightj.BaseTypeHolder;
import org.springframework.jdbc.datasource.DriverManagerDataSource;


/**
 * @author biyu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
@SuppressWarnings({"serial"})
public class BaseDatabaseType extends BaseTypeHolder implements IDatabaseSequencer {
	
	/** sequencer associated with this db */
	private IDatabaseSequencerType sequencer;

	/** information to create data source */
	private String driverClass;
	private String url;
	private String un;
	private String pwd;
	
	/** if this database is shared among multiple instances */
	private boolean shared = true;

	private DataSource ds;
	
	/** constructor */
	protected BaseDatabaseType(String name, IDatabaseSequencerType sequenerType) {
		super(name);
		this.sequencer = sequenerType;
	}
	
	/** initialization */
	public synchronized void initialize() {
		if (driverClass != null) {
			this.ds = createDataSource();
			ConnectionHelper.initDataSource(this, ds);
		}
	}
	
	/** shutdown */
	public synchronized void shutdown() {
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

	public boolean isShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	@Override
	public long getCurrentValue(BaseSequenceEnum seqEnum) throws DataAccessException {
		return sequencer.getCurrentValue(this, seqEnum);
	}

	@Override
	public long getNextValue(BaseSequenceEnum seqEnum) throws DataAccessException {
		return sequencer.getNextValue(this, seqEnum);
	}

	@Override
	public long getCurrentValue(Connection con, BaseSequenceEnum seqEnum) throws DataAccessException {
		return sequencer.getCurrentValue(con, seqEnum);
	}

	@Override
	public long getNextValue(Connection con, BaseSequenceEnum seqEnum) throws DataAccessException {
		return sequencer.getNextValue(con, seqEnum);
	}

}
