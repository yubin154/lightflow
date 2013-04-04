package org.lightj.example.dal;

import java.lang.reflect.Method;

import org.lightj.dal.AbstractDAO;


/**
 * @author biyu Sample DAO
 */
public class SampleDAO extends AbstractDAO<SampleDO> {

	private static final SampleDAO me = new SampleDAO();

	public static final SampleDAO getInstance() {
		return me;
	}

	@SuppressWarnings("rawtypes")
	private SampleDAO() {
		super();
		try {
			this.doKlass = SampleDO.class;
			String[] colNames = new String[] { "id", "col_vc", "col_long",
					"col_float", "col_date", "col_clob", "col_blob" };
			Class[] colTypes = new Class[] { long.class, String.class,
					long.class, double.class, java.util.Date.class,
					java.sql.Clob.class, java.sql.Blob.class };
			String[] javaNames = new String[] { "id", "colVc", "colLong",
					"colFloat", "colDate", "colClob", "colBlob" };
			Method[] getters = findGetters(javaNames);
			Method[] setters = findSetters(javaNames, colTypes);
			register(SampleDO.class, SampleDO.TABLENAME,
					SampleDatabaseEnum.TEST, SampleDO.SEQNAME,
					colNames, colTypes, getters, setters);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
	

}
