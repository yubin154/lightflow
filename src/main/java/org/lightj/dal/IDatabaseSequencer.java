package org.lightj.dal;

import java.sql.Connection;


public interface IDatabaseSequencer {

	/**
	 * generate next value
	 * @param dbEnum
	 * @param seqEnum
	 * @return
	 * @throws DataAccessException
	 */
	public long getNextValue(BaseSequenceEnum seqEnum)	throws DataAccessException;
	
	/**
	 * generate next value
	 * @param con
	 * @param seqEnum
	 * @return
	 * @throws DataAccessException
	 */
	public long getNextValue(Connection con, BaseSequenceEnum seqEnum)	throws DataAccessException;

	/**
	 * get current value
	 * @param dbEnum
	 * @param seqEnum
	 * @return
	 * @throws DataAccessException
	 */
	public long getCurrentValue(BaseSequenceEnum seqEnum) throws DataAccessException;

	
	/**
	 * get current value
	 * @param con
	 * @param seqEnum
	 * @return
	 * @throws DataAccessException
	 */
	public long getCurrentValue(Connection con, BaseSequenceEnum seqEnum) throws DataAccessException;
}
