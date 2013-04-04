package org.lightj.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.lightj.util.Log4jProxy;

/**
 * sequencer oracle implementation 
 * @author biyu
 */
public class OracleSequencerTypeImpl implements IDatabaseSequencerType {

	static Log4jProxy cat = Log4jProxy.getInstance(OracleSequencerTypeImpl.class.getName());

	/** constructor */
	public OracleSequencerTypeImpl() {}

	/**
	 * generate next value
	 */
	@Override
	public long getNextValue(BaseDatabaseType dbEnum, BaseSequenceEnum seqEnum)	throws DataAccessException {
		long retVal = 0;
		final String sql = "SELECT " + seqEnum.getName() + ".nextval as value FROM dual";
		Connection con = null;
		PreparedStatement prepStmt = null;
		ResultSet rs = null;
		try {
			con = ConnectionHelper.getConnection(dbEnum);
			prepStmt = con.prepareStatement(sql);
			rs = prepStmt.executeQuery();
			if (rs.next()) {
				retVal = rs.getLong("value");
			}
		} catch (SQLException sqle) {
			cat.error("sql= " + sql);
			cat.error("SQLException in SequenceDAO.getNextValue(db,seq) because "
							+ sqle.getMessage());
			throw new DataAccessException(sqle);
		} finally {
			ConnectionHelper.cleanupDBResources(rs, prepStmt, con);
		}
		return retVal;
	}

	/**
	 * Method transactionalGetNextValue.
	 * 
	 * @param sequenceEnum
	 * @return long
	 */
	public long getNextValue(Connection con, BaseSequenceEnum e) throws DataAccessException {
		long returnInt = 0;
		String sql = "SELECT " + e.getName() + ".nextval as value FROM dual";

		PreparedStatement prepStmt = null;
		ResultSet rs = null;
		try {
			prepStmt = con.prepareStatement(sql);
			rs = prepStmt.executeQuery();

			if (rs.next()) {
				returnInt = rs.getLong("value");
			}
		} catch (SQLException sqle) {
			cat.error("sql= " + sql);
			cat.error("SQLException in SequenceDAO.transactionalGetNextValue because "
							+ sqle.getMessage());
			throw new DataAccessException(sqle);
		} finally {
			ConnectionHelper.cleanupDBResources(rs, prepStmt, null);
		}
		return returnInt;
	}

	/**
	 * Method transactionalGetCurrentValue.
	 * 
	 * @param sequenceEnum
	 * @return long
	 */
	public long getCurrentValue(Connection con, BaseSequenceEnum e)
			throws DataAccessException {
		long returnInt = 0;
		String sql = "SELECT " + e.getName() + ".currval as value FROM dual";

		PreparedStatement prepStmt = null;
		ResultSet rs = null;
		try {
			prepStmt = con.prepareStatement(sql);
			rs = prepStmt.executeQuery();

			if (rs.next()) {
				returnInt = rs.getLong("value");
			}
		} catch (SQLException sqle) {
			cat.error("sql= " + sql);
			cat.error("SQLException in SequenceDAO.transactionalGetCurrentValue because "
							+ sqle.getMessage());
			throw new DataAccessException(sqle);
		} finally {
			ConnectionHelper.cleanupDBResources(rs, prepStmt, null);
		}
		return returnInt;
	}

	/**
	 * get current value
	 */
	public long getCurrentValue(BaseDatabaseType dbEnum, BaseSequenceEnum seqEnum) throws DataAccessException 
	{
		long retVal = 0;
		final String sql = "SELECT " + seqEnum.getName()
				+ ".currval as value FROM dual";
		Connection con = null;
		PreparedStatement prepStmt = null;
		ResultSet rs = null;
		try {
			con = ConnectionHelper.getConnection(dbEnum);
			prepStmt = con.prepareStatement(sql);
			rs = prepStmt.executeQuery();
			if (rs.next()) {
				retVal = rs.getLong("value");
			}
		} catch (SQLException sqle) {
			cat.error("sql= " + sql);
			cat	.error("SQLException in SequenceDAO.getCurrentValue(db,seq) because "
							+ sqle.getMessage());
			// ORA-8002 sequence string.CURRVAL is not yet defined in this
			// session
			retVal = getNextValue(dbEnum, seqEnum);
		} finally {
			ConnectionHelper.cleanupDBResources(rs, prepStmt, con);
		}
		return retVal;
	}

}
