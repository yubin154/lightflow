/**
 * Created on Feb 2, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.example.dal;

import org.lightj.dal.HsqlDatabaseType;
import org.lightj.dal.MySQLDatabaseType;
import org.lightj.dal.mongo.MongoDatabaseType;


/**
 * @author biyu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class SampleDatabaseEnum {
	
	/** used for unit tests */
	public static final HsqlDatabaseType TEST = new HsqlDatabaseType("TEST_MEMDB", "test", "sa", "");
	public static final HsqlDatabaseType TESTLOCK = new HsqlDatabaseType("TESTLOCK_MEMDB", "test", "sa", "");
	
	/** used for real */
	public static final MySQLDatabaseType QRTZ = new MySQLDatabaseType("FLOW_MYSQL", "jdbc:mysql://localhost:3306/test", "root", "password");
	public static final MySQLDatabaseType LOCK = new MySQLDatabaseType("LOCK_MYSQL", "jdbc:mysql://localhost:3306/test", "root", "password");
	
	/** mongo */
	public static final MongoDatabaseType FLOW_MONGO = new MongoDatabaseType("FLOW_MONGO", "test", new String[] {"localhost"}, 27017);

	static {
		TESTLOCK.setShared(true);
	}
	
}
