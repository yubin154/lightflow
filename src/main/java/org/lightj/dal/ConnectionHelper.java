/*
 * Created on Dec 2, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.sql.DataSource;

import org.lightj.dal.IDataAccess.IResultSetHandler;
import org.lightj.util.Log4jProxy;

/**
 * @author biyu
 *
 * Connection helper that guarantee one connection per db per thread 
 */
public class ConnectionHelper {
	
	// Create Log4j logger instance for logging
	static Log4jProxy cat = Log4jProxy.getInstance(ConnectionHelper.class.getName());
	
	// data sources
	private static HashMap<BaseDatabaseType, DataSource> dataSources = new HashMap<BaseDatabaseType, DataSource>();

	// keeps a mapping of thread to db connection, if thread holding a connection,
	// or a thread locked the connection by start a transaction on it, we always
	// return the same connection when the same thread is asking for connection.
	// the map is thread safe
	private static ThreadLocal<Map<String, ConnectionWrapper>> threadConnections = new ThreadLocal<Map<String, ConnectionWrapper>>() {
		protected synchronized Map<String, ConnectionWrapper> initialValue() {
			return new HashMap<String, ConnectionWrapper>();
		}
	};
	
	/**
	 * Instantiate a new connection pool in a stand alone environment such as testing environment
	 * 
	 * @param dbenum
	 * @throws SQLException
	 */	
	public static final void initDataSource(BaseDatabaseType dbenum, DataSource dataSource) {
		dataSources.put(dbenum, dataSource);
	}

	/**
	 * Get a db connection from the pool.
	 * 
	 * @param dbEnum
	 * @return
	 * @throws SQLException
	 */
	public static final Connection getConnection(BaseDatabaseType dbEnum) throws SQLException
	{
		Connection conn = null;
		ConnectionWrapper conWrapper = null;
		if (dbEnum == null) { 
			cat.error("Invalid DBEnum passed, dbenum value is null.");
			throw new SQLException ("Invalid DBEnum passed, dbenum value is null.");
		}
		
		final String key = dbEnum.toString();
		// this thread already has a open connection, just return it
		Map<String, ConnectionWrapper> connections = threadConnections.get();
		if (connections.containsKey(key)) 
		{
			//cat.debug(Thread.currentThread().getName() + " found and reusing existing db connection.");
			conWrapper = (ConnectionWrapper) connections.get(key);
			conWrapper.checkedOutNum++;
			conn = conWrapper.con;
			if (conn == null || conn.isClosed()) {
				// connection is closed out of the context, warning, and clean it up form thread local map
				cat.warn("Checked out connection is closed or empty, it was closed out of context");
				connections.remove(key);
				return getConnection(dbEnum);
			}
		}
		else {
			// we cannot find any existing connection, now grab one from data source
			//cat.debug(Thread.currentThread().getName() + " creating a new db connection.");
			
			if (dataSources.containsKey(dbEnum)) {
				DataSource dataSource = (DataSource) dataSources.get(dbEnum);
				conn = dataSource.getConnection();
			}
			else {
				throw new SQLException("unable to find Datasource "	+ dbEnum.getName());
			}
			conWrapper = new ConnectionWrapper(conn);
			// put it in threadConnections map
			connections.put(key, conWrapper);
		}
		return conn;
	}

	/**
	 * Return db resources back to the pool
	 * 
	 * @param rs
	 * @param ps
	 * @param con
	 */
	@SuppressWarnings("rawtypes")
	public static void cleanupDBResources(ResultSet rs, Statement ps, Connection con) 
	{
		try {
			if (rs != null) rs.close();
			rs = null;
		} catch (Exception e) {
			cat.error("Error closing ResultSet: " + e.getMessage());
			throw new DataAccessRuntimeException("Error closing ResultSet: " + e.getMessage());
		}

		try {
			if (ps != null) ps.close();
			ps = null;
		} catch (Exception e) {
			cat.error("Error closing Statement: " + e.getMessage());
			throw new DataAccessRuntimeException("Error closing Statement: " + e.getMessage());
		}

		try {
			if (con != null && !con.isClosed()){
				// if connection is not in a transaction context
				// and if the same thread doesn't have any more connection checked out from the same data source
				// close it and remove it from thread map
				Map connections = (Map) threadConnections.get();
				for (Iterator iter = connections.keySet().iterator(); iter.hasNext(); ) {
					Object key = iter.next();
					ConnectionWrapper conWrapper = (ConnectionWrapper) connections.get(key);
					if (conWrapper.con == con) {
						conWrapper.checkedOutNum--;
						if (conWrapper.checkedOutNum > 10) {
							cat.warn("Potential problem!!!, db connection checkout counter bigger than 10: Thread=" + Thread.currentThread().getName() + ",count=" + conWrapper.checkedOutNum + ",autocommit=" + conWrapper.con.getAutoCommit());
						}
						if (conWrapper.checkedOutNum <= 0) {
							boolean closeUncommittedCon = false;
							if (!con.getAutoCommit()) {
								closeUncommittedCon = true;
							}
							con.close();
							connections.remove(key);
							if (closeUncommittedCon) cat.warn("Closed a db connection with uncommitted transaction");
						}
						break;
					} 
				}
			}
		} catch (Exception e) {
			cat.error("Error closing Connection: " + e.getMessage());
			throw new DataAccessRuntimeException("Error closing Connection: " + e.getMessage());
		}
	}

	/**
	 * After this call, all connections ever checked out from this thread will be
	 * in a transaction. This is introduced to integrate Quartz into our db framework,
	 * use it with extreme caution, always end the transaction by commit or rollback, 
	 * it otherwise would cause db locking.
	 * @param dbEnum
	 */
	public static final Connection startTr(BaseDatabaseType dbEnum) {
		Connection conn = null;
		try{
			// reserve a connection for this thread and make it transactional
			// all the subsequent call is going to use the same connection 
			conn = getConnection(dbEnum) ;
			// turn on db transaction
			conn.setAutoCommit(false);
		}
		catch (Exception e) {
			cat.debug("Failed to start transaction because " + e.getMessage());
		}
		return conn;
	}

	/**
	 * Commit a started transaction. Used it with caution.
	 * CAUTION: this method didn't cleanup db resource explicitly, users should do that themselves
	 * @param dbEnum
	 * @throws SQLException
	 */		
	public static final void commitTr(ResultSet rs, Statement stmt, Connection conn) throws SQLException {
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	/**
	 * Commit a transaction, and cleanup db resource
	 * CAUTION: use this method only in business layer, and don't call cleanupDBResources explicitly afterwards
	 * @param dbEnum
	 * @throws SQLException
	 */
	@SuppressWarnings("rawtypes")
	public static final void commitTr(BaseDatabaseType dbEnum) throws SQLException {
		// find the connection this thread checked out and commit it
		String key = dbEnum.toString();
		Map connections = (Map) threadConnections.get();
		if (connections.containsKey(key)) {
			ConnectionWrapper conWrapper = (ConnectionWrapper) connections.get(key);
			commitTr(null, null, conWrapper.con);
			cleanupDBResources(null, null, conWrapper.con);
		}
		else {
			cat.warn("Can not find transaction to commit");
		}
	}
	
	/**
	 * Rollback a started transaction. Use it with caution.
	 * CAUTION: this method didn't cleanup db resource explicitly, users should do that themselves
	 * @param dbEnum
	 */
	public static final void rollbackTr(ResultSet rs, Statement stmt, Connection conn) {
		try{
			conn.rollback();
			conn.setAutoCommit(true);
		}
		catch (Exception e) {
			cat.debug("Failed to rollback transaction because " + e.getMessage());
		}
	}
	
	
	/**
	 * Rollback a transaction, and cleanup all db resources.
	 * CAUTION: use this method only in business layer, and don't call cleanupDBResources explicitly afterwards 
	 * @param dbEnum
	 */
	@SuppressWarnings("rawtypes")
	public static final void rollbackTr(BaseDatabaseType dbEnum) {
		String key = dbEnum.toString();
		Map connections = (Map) threadConnections.get();
		if (connections.containsKey(key)) {
			ConnectionWrapper conWrapper = (ConnectionWrapper) connections.get(key);
			rollbackTr(null, null, conWrapper.con);
			cleanupDBResources(null, null, conWrapper.con);
		}
		else {
			cat.warn("Can not find transaction to rollback");
		}
	}
	
	/**
	 * execute update query 
	 * @param dbEnum
	 * @param query
	 * @return
	 * @throws SQLException
	 */
	public static int executeUpdate(BaseDatabaseType dbEnum, String query) throws SQLException {
		return executeUpdate(dbEnum, query, new Object[] {});
	}
	
	/**
	 * execute update prepared statement
	 * @param dbEnum
	 * @param query
	 * @param args
	 * @return
	 * @throws SQLException
	 */
	public static int executeUpdate(BaseDatabaseType dbEnum, String query, Object[] args) throws SQLException {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = getConnection(dbEnum);
			ps = con.prepareStatement(query);
			for (int i = 1, size = args.length; i <= size; i++) {
				ps.setObject(i, args[i-1]);
			}
			return ps.executeUpdate();
		}
		finally {
			cleanupDBResources(null, ps, con);
		}
	}
	
	/**
	 * execute query with result handler passed in
	 * @param dbEnum
	 * @param query
	 * @param handler
	 * @throws SQLException
	 * @throws DataAccessException
	 */
	public static void executeQuery(BaseDatabaseType dbEnum, String query, IResultSetHandler handler) 
		throws SQLException, DataAccessException 
	{
		executeQuery(dbEnum, query, new Object[] {}, handler);
	}
	
	/**
	 * execute prepared query with result handler passed in
	 * @param dbEnum
	 * @param query
	 * @param args
	 * @param handler
	 * @throws SQLException
	 * @throws DataAccessException
	 */
	public static void executeQuery(BaseDatabaseType dbEnum, String query, Object[] args, IResultSetHandler handler) 
		throws SQLException, DataAccessException 
	{
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = getConnection(dbEnum);
			ps = con.prepareStatement(query);
			for (int i = 1, size = args.length; i <= size; i++) {
				ps.setObject(i, args[i-1]);
			}
			rs = ps.executeQuery();
			if (rs.next()) {
				handler.next(con, rs);
			}
			else {
				handler.handleEmptyResult();
			}
			while (rs.next()) {
				handler.next(con, rs);
			}
			handler.postProcess(con);
		}
		finally {
			cleanupDBResources(rs, ps, con);
		}
	}
	
	/**
	 * conn wrapper
	 * 
	 * @author biyu
	 *
	 */
	static class ConnectionWrapper {
		Connection con;
		int checkedOutNum;
		ConnectionWrapper(Connection con) {
			this.con = con;
			checkedOutNum = 1;
		}
	}


}
