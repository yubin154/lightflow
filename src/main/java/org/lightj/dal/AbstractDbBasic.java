/*
 * Created on Nov 23, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.dal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.lightj.Constants;


/**
 * @author biyu
 *
 * This class provides basic database functionalities to read/write 
 * a java object from/to database.
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractDbBasic {
	
    protected static final Class[] NO_PARAMETER_TYPES = {};
    protected static final Object[] NO_PARAMETER_VALUES = {};

	// java values that are treated as NULL when write to database.
	// et. if a DO has such value in its field, it will be saved as null in database.
    public static final Integer NULL_INTEGER = Integer.valueOf(0);
    public static final Long NULL_LONG = Long.valueOf(0);
	public static final Double NULL_FLOAT = new Double(0.0);
	public static final String NULL_STRING = null;
	public static final Date NULL_DATE = null;
	// boolean value is always saved as non-null 'T'/'F'
	
	/**
	 * Read a row from database and populate them into java object
	 * 
	 * @param target 	java object to be populated
	 * @param rs		query resultset
	 * @param colNames	db table column names
	 * @param colTypes	db table column java types
	 * @param setters	setters to populate java values
	 * @throws SQLException		database exception
	 * @throws IllegalAccessException	reflection exception
	 * @throws InvocationTargetException 	reflection exception
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	protected final void result2Object(Object target, ResultSet rs, String[] colNames, Class[] colTypes, Method[] setters)
		throws SQLException, IllegalAccessException, InvocationTargetException, IOException, ClassNotFoundException
	{
		Object[] objectArray = new Object[1] ;
		for (int i = 0, len = colNames.length; i < len; i++) {
			Object value = rs.getObject(colNames[i]);
			boolean isNull = rs.wasNull();
			// special dealing of number and date
			Class colType = colTypes[i];
			if (isNull) {
				value = getNullValue(colType);
			}
			else {
				if ((colType == long.class || colType == Long.class) && !(value instanceof Long)) {
					value = Long.valueOf(rs.getLong(colNames[i]));
				}
				else if (Date.class.isAssignableFrom(colType) && !(value instanceof Timestamp)) {
					value = rs.getTimestamp(colNames[i]);
				}
				else if ((colType == int.class || colType == Integer.class) && !(value instanceof Integer)) {
					value = Integer.valueOf(rs.getInt(colNames[i]));
				}
				else if ((colType == float.class || colType == Float.class) && !(value instanceof Float)) {
					value = new Float(rs.getFloat(colNames[i]));
				}
				else if ((colType == double.class || colType == Double.class) && !(value instanceof Double)) {
					value = new Double(rs.getDouble(colNames[i]));
				}
				else if (colType == Blob.class) {
			        Blob blob = rs.getBlob(colNames[i]);
			        if (blob != null && blob.length() > 0) {
			            InputStream binaryInput = blob.getBinaryStream(); 

			            if (null != binaryInput) {
			            	try {
			            		value = inflate(binaryInput, false);
			            	}catch (Exception e){
			            		value = inflate(binaryInput, true);
			            	}
			            }
			            if (null != value) {
			                ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream((byte[]) value));
							value = in.readObject();
			                in.close();
			            }
			        }
				}
				else if (colType == Clob.class) {
					Clob clob = rs.getClob(colNames[i]);
					if (clob != null) {
						value = clob.getSubString(1, (int)clob.length());
					}
				}
				else if (colType == boolean.class || colType == Boolean.class) {
					value = Boolean.valueOf(rs.getBoolean(colNames[i]));
				}
				else {
					value = rs.getObject(colNames[i]);
				}
			}
			objectArray[0]=value;
			setters[i].invoke(target, objectArray);				
		}
	}
	
	/**
	 * Prepare sql PreparedStatement based on a java object, make it ready to write to database. 
	 * Assumption: first column is a primary key
	 * 
	 * @param source		java object to be written to database
	 * @param stmt			prepared sql statement
	 * @param colNames		db table column names
	 * @param colTypes		db table column java types
	 * @param getters		getters to read java values
	 * @throws SQLException		database exception
	 * @throws IllegalAccessException	reflection exception
	 * @throws InvocationTargetException	reflection exception
	 * @throws IOException 
	 */
	protected final void object2Stmt(Object source, PreparedStatement stmt, String[] colNames, Class[] colTypes, Method[] getters)
		throws SQLException, IllegalAccessException, InvocationTargetException, IOException
	{
		for (int i = 1, len = colNames.length; i < len; i++) {
			Object value = convert(getters[i].invoke(source, Constants.NO_PARAMETER_VALUES));
			if (isNull(colTypes[i], value)) {
				stmt.setNull(i, getSqlType(colTypes[i]));
			}
			else {
				if (colTypes[i] == Blob.class) {
			        ByteArrayOutputStream os = null;
		            os = new ByteArrayOutputStream();
		            ObjectOutputStream oos = new ObjectOutputStream(os);
		            oos.writeObject(value);
		            oos.close();

		            byte[] buf = os.toByteArray();
		            ByteArrayInputStream is = new ByteArrayInputStream(buf);

		            stmt.setBinaryStream(i, is, buf.length);
				}
				else {
					stmt.setObject(i, value);
				}
			}		
		}
	}
	
	/**
	 * Get one java object from the result set.
	 * Assume one column in resultset in types of integer, long, float, timestamp and varchar
	 * 
	 * @param rs		Resultset
	 * @param sqlType	Sql type to convert to
	 * @return
	 * @throws SQLException
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	protected final Object result2Object(ResultSet rs, int sqlType) throws SQLException, IOException, ClassNotFoundException {
		Object rst = null;
		switch(sqlType) {
			case Types.INTEGER:
				rst = Integer.valueOf(rs.getInt(1));
				break;
			case Types.BIGINT:
				rst = Long.valueOf(rs.getLong(1));
				break;
			case Types.FLOAT:
				rst = new Double(rs.getDouble(1));
				break;
			case Types.TIMESTAMP:
			case Types.DATE:
				rst = rs.getTimestamp(1);
				break;
			case Types.BLOB:
		        Blob blob = rs.getBlob(1);
		        if (blob != null) {
		            InputStream binaryInput = blob.getBinaryStream();

		            if (null != binaryInput) {
		                ObjectInputStream in = new ObjectInputStream(binaryInput);
						rst = in.readObject();
		                in.close();
		            }
		        }
		        break;
			case Types.CLOB:
				Clob clob = rs.getClob(1);
				if (clob != null) {
					rst = clob.getSubString(1, (int)clob.length());
				}
				break;
			case Types.VARCHAR:
			case Types.CHAR:
				rst = rs.getString(1);
				break;

			default:
				rst = rs.getObject(1);
		}
		return rst;
	}
	
	/**
	 * what's considered the null value for this table
	 * @param colType
	 * @return
	 */
	protected Object getNullValue(Class colType) {
		if (colType == Integer.class || colType == int.class) {
			return NULL_INTEGER;
		}
		else if (colType == Long.class || colType == long.class) {
			return NULL_LONG;
		}
		else if (colType == Double.class || colType == double.class) {
			return NULL_FLOAT;
		}
		return null;
	}
	
	
	/**
	 * Wether a java value should be mapped to null value in database table.
	 * 
	 * @param colType
	 * @param value
	 * @return
	 */
	public final boolean isNull(Class colType, Object value) {
		Object nullVal = getNullValue(colType);
		return (nullVal != null ? nullVal.equals(value) : value == null); 
	}
	
	/**
	 * maps a java type to sql type.
	 * 
	 * @param colType
	 * @return	corresponding SQL type
	 */
	protected static final int getSqlType(Class colType) {
		if (colType == Integer.class || colType == int.class) {
			return Types.INTEGER;
		}
		else if (colType == Long.class || colType == long.class) {
			return Types.BIGINT;
		}
		else if (colType == Double.class || colType == double.class) {
			return Types.DOUBLE;
		}
		else if (colType == String.class || colType == Boolean.class || colType == boolean.class) {
			return Types.VARCHAR;
		}
		else if (Date.class.isAssignableFrom(colType)) {
			return Types.TIMESTAMP;
		}
		else if (colType == Blob.class) {
			return Types.BLOB;
		}
		else if (colType == Clob.class) {
			return Types.CLOB;
		}
		throw new RuntimeException("Unsupported sql type");		
	}
	
	/**
	 * Convert a non-sql compatible java type to sql compatible type
	 * @param value
	 * @return
	 */
	protected static final Object convert(Object value) {
		if (value == null) return null;
		if (value.getClass() == java.util.Date.class) {
			return new Timestamp(((Date) value).getTime());
		}
		return value;
	}

	/**
	 * gzip a byte stream to be saved in a blob
	 * @param bis
	 * @param isCompressed
	 * @return
	 * @throws IOException
	 */
	public static byte[] inflate(InputStream bis, boolean isCompressed)
			throws IOException {
		byte[] output;

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		InputStream dis = bis;
		try {
			if (isCompressed) {
				dis = new GZIPInputStream(bis);
			}
			byte[] buffer = new byte[256];
			int n;
			while ((n = dis.read(buffer, 0, 255)) >= 0) {
				bos.write(buffer, 0, n);
			}
			bos.flush();
			output = bos.toByteArray();
		} catch (IOException ex) {
			//rethrow it, so caller will handle it
			throw ex;
		} finally {
			//got a chance to close local stream
			// and blobstream
			try {
				bis.close();
			} catch (IOException ex) {
			}
			try {
				bis.close();
			} catch (IOException ex) {
			}
			try {
				bos.close();
			} catch (IOException ex) {
			}
		}
		return output;
	}
}
