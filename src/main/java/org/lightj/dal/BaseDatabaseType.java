package org.lightj.dal;

import java.sql.Connection;

import org.lightj.BaseTypeHolder;


/**
 * @author biyu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
@SuppressWarnings({"serial"})
public abstract class BaseDatabaseType extends BaseTypeHolder implements IDatabaseSequencer {
	
	/** sequencer associated with this db */
	private IDatabaseSequencerType sequencer;

	/** if this database is shared among multiple instances */
	private boolean shared = true;

	/** constructor */
	protected BaseDatabaseType(String name, IDatabaseSequencerType sequenerType) {
		super(name);
		this.sequencer = sequenerType;
	}
	
	/** initialization */
	public abstract void initialize();
	
	/** shutdown */
	public synchronized void shutdown() {
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
