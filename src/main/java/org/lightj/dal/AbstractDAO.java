/*
 * Created on Nov 22, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.dal;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.lightj.Constants;
import org.lightj.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author biyu
 *
 * This class provides basic functionalities of a DAO. 
 * Limitations:
 * 1. it can only deal with tables with primary key, it has to be the first in colNames array
 * 2. not yet support bitmap, clob and blob columns
 * 3. DO has to have public getters and setters for all its private variables
 * 4. to be accessed via singleton in subclass at all time
 * 5. support types: char, string, boolean - map to varchar/char, 
 * 					 int, long, double - map to number, 
 * 					 date - map to date/time
 */
@SuppressWarnings({"rawtypes","unchecked"})
public abstract class AbstractDAO<T extends IData> extends AbstractDbBasic {

	// logger
	protected static Logger logger = LoggerFactory.getLogger(AbstractDAO.class); 
	
	// db table to java object mapping information
	protected Class<T> doKlass;
	protected String tablePrefix;
	protected String tableName;
	protected BaseDatabaseType dbEnum;
	protected BaseSequenceEnum sequence;
	protected String[] colNames = null;
	protected Class[] colTypes = null;
	protected Method[] getters = null;
	protected Method[] setters = null;

	// cached commonly used db statement
	protected String delSql;
	protected String insertSql;
	protected String updateSql;
	
	/**
	 * A convenient register method, requires elements in javaNames array
	 * to be variable names in DO class mapped to colNames in that order,
	 * also requires getters and setters exist with naming convention
	 * get<i>JavaFieldName</i> and set<i>JavaFieldName</i> 
	 * 
	 * @param doKlass
	 * @param tableName
	 * @param sequenceName
	 * @param colNames
	 * @param javaNames
	 */
	public final void register(Class<T> doKlass, String tableName, 
						BaseDatabaseType db, BaseSequenceEnum sequence,
						String[] colNames, String[] javaNames)
	{
		this.doKlass = doKlass;
		try {
			Method[] getters = findGetters(javaNames);
			Class[] colTypes = new Class[getters.length];
			for (int i = 0, len = getters.length; i < len; i++) {
				colTypes[i] = getters[i].getReturnType();
			}
			Method[] setters = findSetters(javaNames, colTypes);
			register(doKlass, tableName, db, sequence, colNames, colTypes, getters, setters);
		}
		catch (NoSuchMethodException e) {
			logger.error("AbstractDAO.register(), java method is invalid, " + e.getMessage());
			throw new IllegalArgumentException(e.getMessage());
		}
	}
	
	/**
	 * Register a DAO class to handle read/write for a DO class.
	 * 
	 * @param doKlass		DO class
	 * @param tableName		db table used by DO object
	 * @param sequenceName	sequence name for the primary key
	 * @param colNames		db table columns
	 * @param colTypes		db table columns mapped to java types
	 * @param getters		DO object getters
	 * @param setters		DO object setters
	 */
	public final void register(
		Class<T> doKlass,
		String tableName,
		BaseDatabaseType db,
		BaseSequenceEnum sequence,
		String[] colNames,
		Class[] colTypes,
		Method[] getters,
		Method[] setters) {
		this.doKlass = doKlass;
		this.tableName = tableName;
		this.dbEnum = db;
		this.sequence = sequence;
		this.colNames = colNames;
		this.colTypes = colTypes;
		this.getters = getters;
		this.setters = setters;

		// cache commonly used sql statement
		delSql = "DELETE FROM " + tableName + " WHERE " + colNames[0] + "=?";
		String[] questionMarks = new String[colNames.length-1];
		Arrays.fill(questionMarks, "?");
		String[] colNamesWoPk = new String[colNames.length-1];
		System.arraycopy(colNames, 1, colNamesWoPk, 0, colNamesWoPk.length);
		// insert statement, with primary key at the last position
		insertSql =
			"INSERT INTO "
				+ tableName
				+ " ("
				+ StringUtil.join(colNamesWoPk, ",")
				+ (colNames.length > 1 ? "," : "") + colNames[0]
				+ ") VALUES ("
				+ StringUtil.join(questionMarks, ",")
				+ (colNames.length > 1 ? ",?)" : "?)");
		// update statement, exclude primary key				
		for (int i = 0, len = colNamesWoPk.length; i < len; i++) {
			questionMarks[i] = colNamesWoPk[i] + "=?";
		}
		updateSql =
			"UPDATE "
				+ tableName
				+ " SET "
				+ StringUtil.join(questionMarks, ",")
				+ " WHERE "
				+ colNames[0]
				+ "=?";
	}

	/**
	 * find out all getters from java variable names
	 * @param javaNames
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	protected Method[] findGetters(String[] javaNames) throws SecurityException, NoSuchMethodException {
		int size = javaNames.length;
		Method[] beanMethods = new Method[size];
		for (int i = 0; i < size; i++) {
			beanMethods[i] = findGetter(javaNames[i]);
		}
		return beanMethods;
	}
	
	/**
	 * find out getter from java variable name
	 * @param javaName
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	protected Method findGetter(String javaName) throws SecurityException, NoSuchMethodException {
		return doKlass.getMethod("get" + (javaName.length()>1 ? 
				(javaName.substring(0,1).toUpperCase() + javaName.substring(1)) : 
				javaName.toUpperCase()), Constants.NO_PARAMETER_TYPES);	
	}
	
	/**
	 * find out all setters from java variable names
	 * @param javaNames
	 * @param colTypes
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	protected Method[] findSetters(String[] javaNames, Class[] colTypes) throws SecurityException, NoSuchMethodException {
		int size = javaNames.length;
		Method[] beanMethods = new Method[size];
		for (int i = 0; i < size; i++) {
			Class colType = colTypes[i];
			if (colType == Clob.class) colType = String.class;
			else if (colType == Blob.class) colType = Serializable.class;
			beanMethods[i] = findSetter(javaNames[i], colType);
		}
		return beanMethods;
	}
	
	/**
	 * find out setter from java variable name
	 * @param javaName
	 * @param colType
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	protected Method findSetter(String javaName, Class colType) throws SecurityException, NoSuchMethodException {
		return doKlass.getMethod("set" + (javaName.length()>1 ? 
				(javaName.substring(0,1).toUpperCase() + javaName.substring(1)) : 
					javaName.toUpperCase()), new Class[] {colType});
	}
	
	/**
	 * given a col name, return its java type class
	 * @param colName
	 * @return
	 */
	protected Class getColType(String colName) {
		for (int i = 0; i < colNames.length; i++) {
			if (colNames[i].equalsIgnoreCase(colName)) {
				return colTypes[i];
			}
		}
		return String.class;
	}
	
	/**
	 * Find an object by its primary key, fetch it from cache if needed.
	 * 
	 * @param id	primary key id
	 * @param lock	lock the data for update, if true, fromCache is ignored
	 * @param fromCache	load data from cache
	 * @return
	 * @throws DataAccessException
	 */
	public T findById(long id) throws FinderException, DataAccessException {
		Long i = Long.valueOf(id);
		T data = null;
		try {
			data = doKlass.newInstance();
		}
		catch (Exception e) {
			logger.error("Exception finding by id " + doKlass.getName() + " because " + e.getMessage());
			throw new DataAccessException(e);
		}
		initUnique(data, colNames[0], i);
		return data;
	}
	
	/**
	 * Insert or update a DO object to database.
	 * 
	 * @param data	DO object to be inserted/updated
	 */
	public void save(T data) throws DataAccessException {
		// insert or update a row in database based on its persistency
		if (isPersistent(data)) {
			update(data);
		} else {
			insert(data);
		}
	}

	/**
	 * Delete a DO object from database
	 * 
	 * @param data	DO object to be deleted
	 */
	public void delete(T data) throws DataAccessException {
		if (!isPersistent(data)) {
			throw new IllegalArgumentException("Data to be deleted is not persistent");
		}
		beforeDelete(data);
		// delete data from database
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = ConnectionHelper.getConnection(getDbEnum());
			pstmt = conn.prepareStatement(delSql);
			pstmt.setLong(1, data.getPrimaryKey());
			pstmt.executeUpdate();
		} catch (SQLException sqle) {
			logger.error("SQLException deleting " + doKlass.getName() + " because " + sqle.getMessage());
			throw new DataAccessException(sqle);
		} catch (Exception e) {
			logger.error("Reflection exception deleting " + doKlass.getName() + " because " + e.getMessage());
		} finally {
			ConnectionHelper.cleanupDBResources(null, pstmt, conn);
		}
		afterDelete(data);
	}

	/**
	 * Instantiate a DO object based on its unique key
	 * 
	 * @param data	DO object to be instantiated
	 * @param colName	unique column
	 * @param uniqueKey unique value
	 * @param lock  wether to lock the data for update
	 */
	public boolean initUnique(T data, String colName, Object uniqueKey)
		throws DataAccessException {
		if (data.getClass() != this.doKlass) {
			throw new IllegalArgumentException(
				"Wrong DAO to handle " + data.getClass().getName());
		}
		// instantiate an object from a unique column
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = ConnectionHelper.getConnection(getDbEnum());
			pstmt =
				conn.prepareStatement(
					"SELECT * FROM "
						+ this.tableName
						+ " WHERE "
						+ colName
						+ "=?");
			pstmt.setObject(1, uniqueKey);
			rs = pstmt.executeQuery();
			boolean rst = rs.next();
			if (rst) {
				result2Object(data, rs, colNames, colTypes, setters);
			}
			if (rs.next()) {
				// more than one row returned, not unique, throw an exception
				throw new FinderException("Data is not unique");
			}
			return rst;
		} catch (Exception e) {
			logger.error("Exception instantiating " + doKlass.getName() + " because " + e.getMessage());
			throw new DataAccessException(e);
		} finally {
			ConnectionHelper.cleanupDBResources(rs, pstmt, conn);
		}
	}
	
	/**
	 * Instantiate a DO object based on unique key combinations
	 * 
	 * @param data
	 * @param cols
	 * @param vals
	 * @throws DataAccessException
	 */
	public void initUnique(T data, String[] cols, Object[] vals)
		throws DataAccessException {
		if (data.getClass() != this.doKlass) {
			throw new IllegalArgumentException("Wrong DAO to handle " + data.getClass().getName());
		}
		// instantiate an object from unique column combination
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = ConnectionHelper.getConnection(getDbEnum());
			Query query = new Query().select("*").from(this.tableName);
			for (int i = 0, len = cols.length; i < len; i++) {
				if (vals[i] != null) {
					query.and(cols[i], "=", vals[i]);
				}
				else {
					query.and(cols[i] + " is null");
				}
			}
			pstmt =	conn.prepareStatement(query.toString());
			for (int i = 0, len = query.getArgs().size(); i < len; i++) {
				Object val = query.getArgs().get(i);
				pstmt.setObject(i+1, val);
			}
			rs = pstmt.executeQuery();
			if (rs.next()) {
				result2Object(data, rs, colNames, colTypes, setters);
			}
			if (rs.next()) {
				// more than one row returned, not unique, throw an exception
				throw new DataAccessException("Data is not unique");
			}
		} catch (Exception e) {
			logger.error("Exception instantiating " + doKlass.getName() + " because " + e.getMessage());
			throw new DataAccessException(e);
		} finally {
			ConnectionHelper.cleanupDBResources(rs, pstmt, conn);
		}
	}
	
	/**
	 * Search database for a list of DO objects
	 *
	 * @param query 
	 */
	public List<T> search(Query query)	throws DataAccessException {
		StringBuffer sql = new StringBuffer();
		if(!query.isFullQuery()){
			sql.append("SELECT * FROM ").append(tableName).append(query.daoString());
		}else{
			sql.append(query.toString());
		}
		//System.out.println(" ***********SQL Query in Search of Abstarct DAO>>"+sql.toString());
		logger.debug(this.getClass().getName() + ".search(...) is executing " + query.debugString());
		return search(sql.toString(), query.getArgs(), query.getTop(),query.getFetchSize());
	}
	

	/**
	 * Search a possibly joined table for a list of DO objects
	 * @param query
	 * @return
	 */
	public List<T> searchJoin(Query query) throws DataAccessException {
		logger.debug(this.getClass().getName() + ".search(...) is executing " + query.toString());
		return search(query.toString(), query.getArgs(), query.getTop(),query.getFetchSize());
	}
	
	/**
	 * search by a query string for a list of DO objects
	 * 
	 * @param sql
	 * @param args
	 * @param top
	 * @return
	 * @throws DataAccessException
	 */
	private List<T> search(String sql, List args, int top, int fetchSize) throws DataAccessException {
		// search and instantiate list of IData objects
		List<T> rst = new ArrayList<T>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = ConnectionHelper.getConnection(getDbEnum());
			pstmt =	conn.prepareStatement(sql);
			
			if (fetchSize > 0){
				pstmt.setFetchSize(fetchSize);
			}
			for (int i = 1, len = args.size(); i <= len; i++) {
				pstmt.setObject(i, args.get(i-1));
			}
			rs = pstmt.executeQuery();

			/*This is required for top to work all the time*/
			top = (top == 0) ? -1 : top;

			while (rs.next() && (top < 0 || top-- > 0)) {
				T data = doKlass.newInstance();
				result2Object(data, rs, colNames, colTypes, setters);
				rst.add(data);
			}
		} catch (Exception e) {
			logger.error("Exception searching " + doKlass.getName() + " because " + e.getMessage()  + " for sql " + sql);
			throw new DataAccessException(e);
		} finally {
			ConnectionHelper.cleanupDBResources(rs, pstmt, conn);
		}
		return rst;
	}
	
	/**
	 * Aggregate based on query provided. Currently only support aggregate on a number column.
	 * 
	 * @param colName		db column to be aggregated on
	 * @param aggFunc		aggregate function see {@link IDataAccess#AGGFUNC_AVG}
	 * @param whereClause	where clause, eg. "where col1=? group by col2"
	 * @param args			values to be set to prepared statement
	 * @return
	 * @throws DataAccessException
	 */
	public static long aggregate(BaseDatabaseType dbEnum, Query query)
		throws DataAccessException
	{
		long rst = 0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try  {
			conn = ConnectionHelper.getConnection(dbEnum);
			pstmt = conn.prepareStatement(query.toString());
			List args = query.getArgs();
			for (int i = 1, len = args.size(); i <= len; i++) {
				pstmt.setObject(i, args.get(i - 1));
			}
			logger.debug("aggregate(...) executing " + query.debugString());
			rs = pstmt.executeQuery();
			if (rs.next()) {
				rst = rs.getLong(1);
			}
		}
		catch (SQLException sqle) {
			logger.error("Exception aggregating because " + sqle.getMessage());
			throw new DataAccessException(sqle);
		} finally {
			ConnectionHelper.cleanupDBResources(rs, pstmt, conn);
		}
		return rst;
	}
	
	/**
	 * Convenient method of instantiate a java object from resultset
	 * 
	 * @param rs	database resultset
	 * @return		java object instantiated
	 * @throws DataAccessException
	 */
	public T result2Object(ResultSet rs) throws DataAccessException {
		try {
			T data = doKlass.newInstance();
			result2Object(data, rs, colNames, colTypes, setters);
			return data;
		}
		catch (Exception e) {
			logger.error("Exception instantiating " + doKlass.getName() + " from resultset because " + e.getMessage());
			throw new DataAccessException(e);
		}
	}

	/**
	 * Search database for one column, generate a collection of corresponding java objects.
	 * What collection to be used is completely upto user, if user provides a List, it allows duplicates,
	 * if user provides a Set, it disallow duplicates, if user provides a synchronized collection,
	 * it will be thread safe.
	 * 
	 * @param rst			the collection to be populated
	 * @param query			the query
	 * @param klass			target java class to be returned in a collection
	 * @return
	 * @throws DataAccessException
	 */	
	public void search(BaseDatabaseType dbEnum, Collection rst, Query query, Class klass) throws DataAccessException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int sqlType = Types.NULL;
		if (klass == Integer.class || klass == int.class) {
			sqlType = Types.INTEGER;
		}
		else if (klass == Long.class || klass == long.class) {
			sqlType = Types.BIGINT;
		}
		else if (klass == Double.class || klass == double.class) {
			sqlType = Types.FLOAT;
		}
		else if (Date.class.isAssignableFrom(klass)) {
			sqlType = Types.TIMESTAMP;
		}
		else if (klass == String.class) {
			sqlType = Types.VARCHAR;
		}
		else {
			throw new DataAccessException("Unsupported data type " + klass.getName());
		}
		try {
			conn = ConnectionHelper.getConnection(dbEnum);
			stmt = conn.prepareStatement(query.toString());
			List args = query.getArgs();
			for (int i = 1, size = args.size(); i <= size; i++ ) {
				stmt.setObject(i, args.get(i-1));
			}
			logger.debug("search(...) is executing " + query.debugString());
			rs = stmt.executeQuery();
			while (rs.next()) {
				rst.add(result2Object(rs, sqlType));
			}
		}
		catch (SQLException sqle) {
			logger.error("Exception search4List " + klass.getName() + " with query " + query.debugString() + " because " + sqle.getMessage());
			throw new DataAccessException(sqle);
		}
		catch (IOException ioe) {
			logger.error("Exception search4List " + klass.getName() + " with query " + query.debugString() + " because " + ioe.getMessage());
			throw new DataAccessException(ioe);
		}
		catch (ClassNotFoundException cnf) {
			logger.error("Exception search4List, blob object not found " + klass.getName() + " with query " + query.debugString() + " because " + cnf.getMessage());
			throw new DataAccessException(cnf);
		}
		finally {
			ConnectionHelper.cleanupDBResources(rs, stmt, conn);
		}
	}

	/**
	 * Convert a DO object to readable string. 
	 * 
	 * @param data
	 * @return a string with all its fields and values.
	 */
	public String toString(T data) {
		StringBuffer buf = new StringBuffer();
		try {
		for (int i = 0, len = getters.length; i < len; i++) {
			Object value = getters[i].invoke(data, Constants.NO_PARAMETER_VALUES);
			buf.append(colNames[i]).append('=').append(value!=null ? value.toString() : "").append(","); 
		}
		}
		catch (Exception e) {
			logger.error("Exception printing " + doKlass.getName() + " because " + e.getMessage());
			// ignore
		}
		return buf.toString();
	}
	
	/**
	 * Check two DOs for equalness
	 * @param d1
	 * @param d2
	 * @return
	 */
	public boolean equals(T d1, T d2) {
		if (d1.getClass() != doKlass || d2.getClass() != doKlass) {
			throw new RuntimeException("Cannot compare " + d1.getClass().getName() + 
				" with " + d2.getClass().getName() + " by " + this.getClass().getName());
		}
		if (d1 == d2) return true;
		try {
			for (int i = 0, len = getters.length; i < len; i++) {
				Object rv1 = getters[i].invoke(d1, Constants.NO_PARAMETER_VALUES);
				Object rv2 = getters[i].invoke(d2, Constants.NO_PARAMETER_VALUES);
				if (rv1 != null && rv2 != null) {
					if (!rv1.equals(rv2)) {
						return false;
					}
				}
			}
		}
		catch (Exception e) {
			logger.error("Exception comparing " + doKlass.getName() + " because " + e.getMessage());
			throw new RuntimeException(e.getMessage());
		}
		return true;
	}
	
	/**
	 * Search for result and process it by a external ResultSet handler
	 * @param query			query to be executed
	 * @param handler		ResultSet handler
	 * @throws DataAccessException
	 */
	public void search(final Query query, final IDataAccess.IResultSetHandler handler, final boolean lock) 
		throws DataAccessException 
	{
		StringBuffer sql = new StringBuffer();
		if (query.isFullQuery()) {
			sql.append(query.toString());
		} 
		else  {
			sql.append("SELECT * FROM ").append(this.tableName).append(query.daoString());
		}
		if (lock) {
			sql.append(" ").append("FOR UPDATE");
		}
		logger.debug(this.getClass().getName() + ".search(...) is executing " + query.debugString());
		final String sqlStr = sql.toString();
		final List args = query.getArgs();
		try {
			if (lock) {
				// SELECT ..... FOR UPDATE have to be done in a transaction context
				final BaseDatabaseType db = this.getDbEnum(); 
				TransactionManager.execute(this.getDbEnum(), new ITransactional() {
					public void execute() throws Exception {
						int count = query.getTop();
						boolean noTop = (query.getTop() <= 0);
						Connection conn = null;
						PreparedStatement stmt = null;
						ResultSet rs = null;
						try {
							conn = ConnectionHelper.getConnection(db);
							stmt = conn.prepareStatement(sqlStr);
							for (int i = 1, len = args.size(); i <= len; i++) {
								stmt.setObject(i, args.get(i-1));
							}
							rs = stmt.executeQuery();
							if (rs.next()) {
								do {
									handler.next(conn, rs);
								} while (rs.next() && (noTop || --count > 0));
									handler.postProcess(conn);
							}
							else {
								handler.handleEmptyResult();
							}
						}
						finally {
							ConnectionHelper.cleanupDBResources(rs, stmt, conn);
						}
					}
				});
			}
			else {
				Connection conn = null;
				PreparedStatement stmt = null;
				ResultSet rs = null;
				int count = query.getTop();
				boolean noTop = (query.getTop() <= 0);
				try {
					conn = ConnectionHelper.getConnection(this.getDbEnum());
					stmt = conn.prepareStatement(sqlStr);
					for (int i = 1, len = args.size(); i <= len; i++) {
						stmt.setObject(i, args.get(i-1));
					}
					rs = stmt.executeQuery();
					if (rs.next()) {
						do {
							handler.next(conn, rs);
						} while (rs.next() && (noTop || --count > 0));
						handler.postProcess(conn);
					}
					else {
						handler.handleEmptyResult();
					}
				}
				finally {
					ConnectionHelper.cleanupDBResources(rs, stmt, conn);
				}
			}
		}
		catch (Exception e) {
			logger.error("Exception locking " + doKlass.getName() + " because " + e.getMessage());
			throw new DataAccessException(e);
		}
		catch (Throwable t) {
			logger.error("Exception locking " + doKlass.getName() + " because " + t.getMessage());
			throw new DataAccessException(t.getMessage());
		}
	}

	/**
	 * To determine if an entity is persistent.
	 * default implementation, check {@link IData#getPrimaryKey()}
	 *  
	 * @param data DO object to be tested
	 * @return
	 */
	protected boolean isPersistent(T data) {
		// weather the data is persistent in db
		return (!isNull(long.class, data.getPrimaryKey()));
	}

	/**
	 * Do the actual db insert.
	 * @param data
	 * @throws DataAccessException
	 */
	protected void insert(T data) throws DataAccessException {
		// insert a row into database
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			// before insert
			beforeInsert(data);

			conn = ConnectionHelper.getConnection(getDbEnum());
			pstmt = conn.prepareStatement(insertSql);
			// populate primary key
			long nextVal = dbEnum.getNextValue(conn, sequence);
			pstmt.setLong(colNames.length, nextVal);
			object2Stmt(data, pstmt, colNames, colTypes, getters);
			pstmt.executeUpdate();
			
			// populate the primary key back to the object
			if (isNull(Long.class, nextVal)) {
				// db with auto_increment column, get current value of seq
				try {
					setters[0].invoke(data, new Object[] {Long.valueOf(dbEnum.getCurrentValue(sequence))});
				} catch (Throwable t) {
					logger.error("", t);
				}
			} 
			else {
				// for db with sequence
				setters[0].invoke(data, new Object[] {Long.valueOf(nextVal)});
			}

			// after insert
			afterInsert(data);

		} catch (Exception e) {
			logger.error("Exception inserting " + doKlass.getName() + " because " + e.getMessage());
			throw new DataAccessException(e);
		} finally {
			ConnectionHelper.cleanupDBResources(null, pstmt, conn);
		}
	}

	/**
	 * Do the actual db update.
	 * @param data
	 * @throws DataAccessException
	 */
	protected void update(T data) throws DataAccessException {
		beforeUpdate(data);
		// update a row in database
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = ConnectionHelper.getConnection(getDbEnum());
			pstmt = conn.prepareStatement(updateSql);
			object2Stmt(data, pstmt, colNames, colTypes, getters);
			pstmt.setLong(colNames.length, data.getPrimaryKey());
			pstmt.executeUpdate();
		} catch (Exception e) {
			logger.error("Exception updating " + doKlass.getName() + " because " + e.getMessage());
			throw new DataAccessException(e);
		} finally {
			ConnectionHelper.cleanupDBResources(null, pstmt, conn);
		}
		afterUpdate(data);
	}

	////////////// Call back methods //////////////

	/**
	 * Anything to do before insert, override in subclass
	 */
	protected void beforeInsert(T data) {
		// anything to be done before insert
	}

	/**
	 * Anything to do after insert, override in subclass
	 */
	protected void afterInsert(T data) {
		// anything to be done after insert
	}

	/**
	 * Anything to do before update, override in subclass
	 */
	protected void beforeUpdate(T data) {
		// anything to be done before update
	}

	/**
	 * Anything to do after update, override in subclass
	 */
	protected void afterUpdate(T data) {
		// anything to be done after update
	}

	/**
	 * Anything to do before delete, override in subclass
	 */
	protected void beforeDelete(T data) {
		// anything to be done before delete
	}

	/**
	 * Anything to do after delete, override in subclass
	 */
	protected void afterDelete(T data) {
		// anything to be done after delete
		try {
			// set the primary key to null
			setters[0].invoke(data, new Object[]{getNullValue(Integer.class)});
		}
		catch (IllegalAccessException e) {
			logger.error("Exception in afterDelete: " + this.getClass().getName() + " because " + e.getMessage());
			throw new RuntimeException(e.getMessage());
		}
		catch (InvocationTargetException e) {
			logger.error("Exception in afterDelete: " + this.getClass().getName() + " because " + e.getMessage());
			throw new RuntimeException(e.getMessage());
		}
	}
	
	/**
	 * anything can be runned here, use with caution
	 * @param updateQuery
	 * @return
	 * @throws SQLException
	 */
	protected int runUpdate(String updateQuery) throws SQLException {
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = ConnectionHelper.getConnection(this.getDbEnum());
			stmt = conn.createStatement();
			return stmt.executeUpdate(updateQuery);
		}
		finally {
			ConnectionHelper.cleanupDBResources(null, stmt, conn);
		}
	}

	////////////// setter/getter //////////////
	/**
	 * Which database should registered DO be read/write to, concrete implementation in subclass
	 */
	public BaseDatabaseType getDbEnum() {
		return this.dbEnum; 
	}
	
	public void setDbEnum(BaseDatabaseType dbEnum) {
		this.dbEnum = dbEnum;
	}
	
	public final String getTableName() {
		return this.tableName;
	}
	
	/**
	 * save homogenous data in batch
	 * @param datas
	 * @throws DataAccessException
	 */
	public void saveBatch(Collection<T> datas) throws DataAccessException {
		Connection con = null;
		PreparedStatement psInsert = null;
		PreparedStatement psUpdate = null;
		try {
			con = ConnectionHelper.getConnection(this.getDbEnum());
			psInsert = con.prepareStatement(insertSql);
			boolean hasInsert = false;
			psUpdate = con.prepareStatement(updateSql);
			boolean hasUpdate = false;
			for (T data : datas) {
				if (isPersistent(data)) {
					object2Stmt(data, psUpdate, colNames, colTypes, getters);
					psUpdate.setLong(colNames.length, data.getPrimaryKey());
					psUpdate.addBatch();
					hasUpdate |= true;
				}
				else {
					long nextVal = getDbEnum().getNextValue(con, sequence);
					psInsert.setLong(colNames.length, nextVal);
					object2Stmt(data, psInsert, colNames, colTypes, getters);
					psInsert.addBatch();
					hasInsert |= true;
					// populate the primary key back to the object
					setters[0].invoke(data, new Object[] {Long.valueOf(nextVal)});
				}
			}
			if (hasInsert) psInsert.executeBatch();
			if (hasUpdate) psUpdate.executeBatch();
			psInsert.close();
			psUpdate.close();
		} catch (Exception e) {
			throw new DataAccessException(e);
		} finally {
			ConnectionHelper.cleanupDBResources(null, null, con);
		}
		
	}
	
	public void saveBatch(Collection<T> datas, int sequenceIncrementSize) throws DataAccessException {
		Connection con = null;
		PreparedStatement psInsert = null;
		PreparedStatement psUpdate = null;
		try {
			con = ConnectionHelper.getConnection(this.getDbEnum());
			con.setAutoCommit(false);
			psInsert = con.prepareStatement(insertSql);
			boolean hasInsert = false;
			psUpdate = con.prepareStatement(updateSql);
			boolean hasUpdate = false;
			int sequencesRemaining = 0;
			long nextVal = 0;
			for (T data : datas) {
				if (isPersistent(data)) {
					object2Stmt(data, psUpdate, colNames, colTypes, getters);
					psUpdate.setLong(colNames.length, data.getPrimaryKey());
					psUpdate.addBatch();
					hasUpdate |= true;
				}
				else {
					//for batch inserts, selecting the ids one at a time takes a while.
					if (sequencesRemaining == 0){
						 sequencesRemaining = sequenceIncrementSize;
						nextVal = dbEnum.getNextValue(con, sequence);
					}else{
						nextVal ++;
						
					}
					sequencesRemaining --;
					psInsert.setLong(colNames.length, nextVal);
					object2Stmt(data, psInsert, colNames, colTypes, getters);
					psInsert.addBatch();
					hasInsert |= true;
					// populate the primary key back to the object
					setters[0].invoke(data, new Object[] {Long.valueOf(nextVal)});
				}
			}
			if (hasInsert) psInsert.executeBatch();
			if (hasUpdate) psUpdate.executeBatch();
			psInsert.close();
			psUpdate.close();
			con.commit();

		} catch (Exception e) {
			try {
				con.rollback();
			} catch (SQLException e1) {
			}
			throw new DataAccessException(e);
			
		} finally {
			ConnectionHelper.cleanupDBResources(null, null, con);
		}
		
	}

	public String getTablePrefix() {
		return tablePrefix;
	}

	public synchronized final void setTablePrefix(String tablePrefix) {
		if (tablePrefix != null) {
			if (this.tablePrefix != null) {
				// replace
				tableName = tableName.replaceFirst("^" + this.tablePrefix, tablePrefix);
			}
			else {
				// set
				tableName = tablePrefix + tableName;
			}
		}
		else if (this.tablePrefix != null) {
			// reset
			tableName = tableName.replaceFirst("^" + this.tablePrefix, "");
		}
		this.tablePrefix = tablePrefix;
	}
	
	/**
	 * create a new instance of query
	 * @return
	 */
	public Query newQuery() {
		return new Query();
	}
}
