package org.lightj.dal;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @author biyu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
@SuppressWarnings("rawtypes")
public interface IDataAccess {
	
	public void save(IData data) throws DataAccessException;
	
	public void delete(IData data) throws DataAccessException;
	
	public List search(Query query) throws DataAccessException;
	
	public void search(Query query, IResultSetHandler handler, boolean lock) throws DataAccessException; 

	public void initUnique(IData data, String colName, Object value) throws DataAccessException;
	
	public IData findById(int id, boolean fromCache) throws FinderException, DataAccessException;
	
	public IData result2Object(ResultSet rs) throws DataAccessException;
	
	public static interface IResultSetHandler {
		public void next(Connection conn, ResultSet rs) throws SQLException, DataAccessException;
		public void handleEmptyResult() throws DataAccessException;
		public void postProcess(Connection conn) throws SQLException, DataAccessException;
	}
}
