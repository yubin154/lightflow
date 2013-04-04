package org.lightj.dal;

import java.sql.Connection;

/**
 * database sequencer type
 * @author biyu
 *
 */
public interface IDatabaseSequencerType {

	/**
	 * get current value
	 * @param con
	 * @param seqEnum
	 * @return
	 * @throws DataAccessException
	 */
	public long getCurrentValue(Connection con, BaseSequenceEnum seqEnum) throws DataAccessException;

	/**
	 * get current value
	 * @param dbEnum
	 * @param seqEnum
	 * @return
	 * @throws DataAccessException
	 */
	public long getCurrentValue(BaseDatabaseType dbEnum, BaseSequenceEnum seqEnum) throws DataAccessException;

	/**
	 * generate next value
	 * @param con
	 * @param seqEnum
	 * @return
	 * @throws DataAccessException
	 */
	public long getNextValue(Connection con, BaseSequenceEnum seqEnum)	throws DataAccessException;

	/**
	 * get next value
	 * @param dbEnum
	 * @param seqEnum
	 * @return
	 * @throws DataAccessException
	 */
	public long getNextValue(BaseDatabaseType dbEnum, BaseSequenceEnum seqEnum) throws DataAccessException;
	
}
