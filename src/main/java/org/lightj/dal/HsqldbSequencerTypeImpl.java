package org.lightj.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class HsqldbSequencerTypeImpl implements IDatabaseSequencerType {
	
	static Logger cat = Logger.getLogger(HsqldbSequencerTypeImpl.class);

	/** dual table */
	private String dualTable;
	
	/** constructor */
	public HsqldbSequencerTypeImpl(String dualTable) {
		this.dualTable = dualTable;
	}
	
	/**
	 * get next value
	 */
	public long getNextValue(BaseDatabaseType dbEnum, BaseSequenceEnum e) throws DataAccessException 
	{
    	long returnInt = 0;
		Connection con = null;
		try {
			con = ConnectionHelper.getConnection(dbEnum);
			returnInt = getNextValue(con, e);
		} catch (SQLException sqle) {
			throw new DataAccessException(sqle);
		} finally {
			ConnectionHelper.cleanupDBResources(null, null, con);
		}
		return returnInt;
	}
	
	/**
	 * Method transactionalGetNextValue.
	 * 
	 * @param sequenceEnum
	 * @return long
	 */
	public long getNextValue(Connection con, BaseSequenceEnum e) throws DataAccessException {
		long retVal = 0;
		final String sql= "SELECT NEXT VALUE FOR " + e.getName() + " FROM " + dualTable;
		PreparedStatement prepStmt = null;
		ResultSet rs = null;
		try {
			prepStmt = con.prepareStatement(sql);
			rs = prepStmt.executeQuery();
			if (rs.next()){
				retVal = rs.getLong(1);			
			}
		} catch(SQLException sqle) {
			cat.error("sql= " + sql);
			cat.error("SQLException in SequenceDAO.getNextValue(db,seq) because " + sqle.getMessage());
			throw new DataAccessException(sqle);
		} finally {
			ConnectionHelper.cleanupDBResources(rs, prepStmt, null);
		}
		return retVal;
	}
	
	/**
	 * current value of a sequence
	 */
	public long getCurrentValue(BaseDatabaseType dbEnum, BaseSequenceEnum seqEnum) throws DataAccessException {
		throw new UnsupportedOperationException("Get current value of a sequence is not supported");
	}

	@Override
	public long getCurrentValue(Connection con, BaseSequenceEnum seqEnum) throws DataAccessException {
		throw new UnsupportedOperationException("Get current value of a sequence is not supported");
	}

}
