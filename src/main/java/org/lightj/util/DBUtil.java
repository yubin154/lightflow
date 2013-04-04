/*
 * Created on Feb 3, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.util;

import java.io.Reader;
import java.sql.Clob;
import java.sql.SQLException;

/**
 * @author biyu
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class DBUtil {
	
	public static final String OP_CONCAT = "||";

	/**
	 * by Lawrence Wong
	 * 
	 * @param clob
	 * @return
	 * @throws SQLException
	 * @throws java.io.IOException
	 */
	public static String readClob(Clob clob) throws SQLException,
			java.io.IOException {
		if (clob == null)
			return null;
		// Open a stream to read Clob data
		Reader clobStream = clob.getCharacterStream();
		// Holds the Clob data when the Clob stream is being read
		// Read from the Clob stream and write to the stringbuffer
		int nchars = 0;

		// Number of characters read
		char[] buffer = new char[1024];
		StringBuffer result = new StringBuffer();

		// Buffer holding characters being transferred
		while ((nchars = clobStream.read(buffer)) != -1) {
			// Read from Clob
			result.append(buffer, 0, nchars);
			// Write to StringBuffer
		}
		clobStream.close();
		// Close the Clob input stream
		return result.toString();
	}

	public static String dbC(String src) {
		return (!StringUtil.isNullOrEmpty(src)) ? StringUtil.quote(escape(src)) : "NULL";
	}

	/**
	 *	developers are adviced not to override this method in subclasses.
	 * 	instead, override escape(char) to provide more escaped sequences.
	 */
	public static String escape(String src) {
		return src.replaceAll("'", "''");
	}
	
	/**
	 * @param strings
	 * @param separator
	 * @return
	 */
	public static String joinDbC(String[] strings, String separator) {
		if (strings != null) {
			for (int i = 0, len = strings.length; i < len; i++) {
				strings[i] = dbC(strings[i]);
			}
		}
		return StringUtil.join(strings, separator); 
	}
	
}
