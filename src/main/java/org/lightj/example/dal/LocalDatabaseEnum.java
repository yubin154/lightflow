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
public class LocalDatabaseEnum {
	
	/** used for unit tests */
	public static final HsqlDatabaseType TESTMEMDB = new HsqlDatabaseType("TEST_MEMDB", "test", "sa", "");
	
	/** used for real */
	public static final MySQLDatabaseType TESTMYSQL = new MySQLDatabaseType("FLOW_MYSQL", "jdbc:mysql://localhost:3306/test", "root", "password");
	
	/** mongo */
	public static final MongoDatabaseType TESTMONGO = new MongoDatabaseType("FLOW_MONGO", "test", new String[] {"localhost"}, 27017);

}
