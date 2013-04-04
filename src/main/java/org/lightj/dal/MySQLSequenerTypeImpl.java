package org.lightj.dal;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * mysql sequencer, note mysql uses auto_increment column for id, so no sequence value available before insert
 * @author biyu
 *
 */
public class MySQLSequenerTypeImpl implements IDatabaseSequencerType {

	@Override
	public long getCurrentValue(Connection con, BaseSequenceEnum seqEnum)
			throws DataAccessException {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery("select last_insert_id()");
			if (rs.next()) {
				return rs.getLong(1);
			}
			else {
				return 0;
			}
		} catch (SQLException e) {
			throw new DataAccessException(e);
		} finally {
			ConnectionHelper.cleanupDBResources(rs, stmt, null);
		}
		
	}

	@Override
	public long getCurrentValue(BaseDatabaseType dbEnum,
			BaseSequenceEnum seqEnum) throws DataAccessException {
		Connection con = null;
		try {
			con = ConnectionHelper.getConnection(dbEnum);
			return getCurrentValue(con, seqEnum);
		} catch (SQLException e) {
			throw new DataAccessException(e);
		} finally {
			ConnectionHelper.cleanupDBResources(null, null, con);
		}
	}

	@Override
	public long getNextValue(Connection con, BaseSequenceEnum seqEnum)
			throws DataAccessException {
		return 0;
	}

	@Override
	public long getNextValue(BaseDatabaseType dbEnum, BaseSequenceEnum seqEnum)
			throws DataAccessException {
		return 0;
	}

}
