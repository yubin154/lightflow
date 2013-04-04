/**
 * Created on Feb 2, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.example.dal;

import org.lightj.dal.HsqlDatabaseType;
import org.lightj.dal.MySQLDatabaseType;


/**
 * @author biyu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class SampleDatabaseEnum {
	
	/** used for unit tests */
	public static final HsqlDatabaseType TEST = new HsqlDatabaseType("TEST", "test", "sa", "");
	public static final HsqlDatabaseType TESTLOCK = new HsqlDatabaseType("TEST", "test", "sa", "");
	
	/** used for real */
	public static final MySQLDatabaseType QRTZ = new MySQLDatabaseType("QRTZ", "jdbc:mysql://localhost:3306/test", "root", "password");
	public static final MySQLDatabaseType LOCK = new MySQLDatabaseType("LOCK", "jdbc:mysql://localhost:3306/test", "root", "password");
	
	static {
		TESTLOCK.setShared(true);
	}
	
}
