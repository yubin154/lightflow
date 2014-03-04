/*
 * Created on Dec 5, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.dal;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.lightj.BaseTestCase;
import org.lightj.example.dal.SampleDAO;
import org.lightj.example.dal.SampleDO;
import org.lightj.example.dal.SampleDatabaseEnum;
import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;
import org.lightj.initialization.ShutdownException;


/**
 * @author biyu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestDbFramework extends BaseTestCase {
	
	/**
	 * test setting table prefix
	 * @throws Exception
	 */
	@Test
	public void testTablePrefix() throws Exception {
		String oldTableName = SampleDAO.getInstance().getTableName();
		// set
		SampleDAO.getInstance().setTablePrefix("prefix1_");
		Assert.assertEquals("prefix1_" + oldTableName, SampleDAO.getInstance().getTableName());
		Assert.assertEquals("prefix1_", SampleDAO.getInstance().getTablePrefix());
		// replace
		SampleDAO.getInstance().setTablePrefix("prefix2_");
		Assert.assertEquals("prefix2_" + oldTableName, SampleDAO.getInstance().getTableName());
		Assert.assertEquals("prefix2_", SampleDAO.getInstance().getTablePrefix());
		// reset
		SampleDAO.getInstance().setTablePrefix(null);
		Assert.assertEquals(oldTableName, SampleDAO.getInstance().getTableName());
		Assert.assertNull(SampleDAO.getInstance().getTablePrefix());
	}

	/**
	 * This is to test all sql types to see if they are supported correctly
	 * by the framework.
	 *
	 */
	@Test
	public void testDataTypes() throws Exception {
		final SampleDAO dao = SampleDAO.getInstance();
		final SampleDO data = new SampleDO();
		data.setColVc("test");
		data.setColLong(1000);
		data.setColFloat(3.1415);
		Calendar c = Calendar.getInstance();
		c.set(Calendar.MILLISECOND, 0);
		Date d = new Timestamp(c.getTime().getTime());
		data.setColDate(d);
		String clobStr = "some test clob text";
		data.setColClob(clobStr);
		Integer testBlob = new Integer(1000);
		data.setColBlob(testBlob);
		dao.save(data);
		try {
			// check if the primary key gets populated
			Assert.assertTrue((data.getId()>0));
			// check if the data is successfully loaded
			SampleDO another = dao.findById(data.getId());
			Assert.assertEquals(data.getId(), another.getId());
			Assert.assertEquals(data.getColVc(), another.getColVc());
			Assert.assertEquals(data.getColLong(), another.getColLong());
			Assert.assertEquals(data.getColFloat(), another.getColFloat(), 0.001);
			Assert.assertEquals(data.getColDate(), another.getColDate());
			Assert.assertEquals(data.getColClob(), another.getColClob());
			Assert.assertEquals(testBlob.intValue(), ((Integer)another.getColBlob()).intValue());
			// test update
			data.setColVc("test1");
			data.setColLong(2000);
			data.setColFloat(4.1415);
			c.setTimeInMillis(System.currentTimeMillis());
			c.set(Calendar.MILLISECOND, 0);
			d = c.getTime();
			data.setColDate(d);
			data.setColClob("test clob1");
			data.setColBlob(new Integer(2000));
			dao.save(data);
			dao.initUnique(another, "ID", new Long(data.getId()));
			Assert.assertEquals(data.getId(), another.getId());
			Assert.assertEquals(data.getColVc(), another.getColVc());
			Assert.assertEquals(data.getColLong(), another.getColLong());
			Assert.assertEquals(data.getColFloat(), another.getColFloat(), 0.001);
			Assert.assertEquals(data.getColDate(), another.getColDate());
			// test delete
			dao.delete(data);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testInsert() throws Exception {
		SampleDO data = new SampleDO();
		Date d = new Date();
		data.setColVc("test");
		data.setColLong(1000);
		data.setColFloat(3.1415);
		data.setColDate(d);
		data.setColClob("test clob");
		data.setColBlob(new Integer(1000));
		SampleDAO.getInstance().save(data);
		Assert.assertTrue((data.getId()>0));
		Assert.assertEquals("test", data.getColVc());
		Assert.assertEquals(3.1415, data.getColFloat(), 0.001);
		Assert.assertEquals(d, data.getColDate());
		Assert.assertEquals(1000, data.getColLong());
		Assert.assertEquals("test clob", data.getColClob());
		Assert.assertEquals(new Integer(1000), data.getColBlob());
	}
	
	@Test
	public void testDelete() throws Exception {
		SampleDO data = new SampleDO();
		data.setColVc("test");
		data.setColLong(1000);
		data.setColFloat(3.1415);
		data.setColDate(new Date());
		data.setColClob("test clob");
		data.setColBlob(new Integer(1000));
		SampleDAO.getInstance().save(data);
		Assert.assertTrue((data.getId()>0));
		SampleDAO.getInstance().delete(data);
		Assert.assertTrue((data.getId()<=0));
	}
	
	@Test
	public void testUpdate() throws Exception {
		Date d = new Date();
		SampleDO data = new SampleDO();
		data.setColVc("test");
		data.setColFloat(3.1415);
		data.setColDate(new Date());
		data.setColClob("test clob");
		data.setColBlob(new Integer(1000));
		SampleDAO.getInstance().save(data);
		Assert.assertTrue((data.getId()>0));
		data.setColVc("test1");
		data.setColLong(2000);
		data.setColFloat(4.1415);
		d = new Timestamp(d.getTime() + 1000);
		data.setColDate(d);
		data.setColClob("test clob1");
		data.setColBlob(new Integer(2000));
		SampleDAO.getInstance().save(data);
		SampleDO another = SampleDAO.getInstance().findById(data.getId());
		Assert.assertEquals("test1", another.getColVc());
		Assert.assertEquals(4.1415, another.getColFloat(), 0.001);
		Assert.assertEquals(d.getTime()/1000, another.getColDate().getTime()/1000);
		Assert.assertEquals(2000, another.getColLong());
		Assert.assertEquals("test clob1", data.getColClob());
		Assert.assertEquals(new Integer(2000), data.getColBlob());
	}
	
	@Test
	public void testSearch() throws Exception {
		SampleDO data = new SampleDO();
		data.setColVc("test");
		data.setColLong(1000);
		data.setColFloat(3.1415);
		data.setColDate(new Date());
		data.setColClob("test clob");
		data.setColBlob(new Integer(1000));
		SampleDAO.getInstance().save(data);
		// one entry by search
		List<SampleDO> datas = SampleDAO.getInstance().search(
				new Query().and("ID", "=", new Long(data.getId())).orderBy("ID").setTop(10));
		Assert.assertEquals(datas.size(), 1);
		Assert.assertEquals(data.getId(), datas.get(0).getId());
		// one entry by findById
		SampleDO d = SampleDAO.getInstance().findById(data.getId());
		Assert.assertEquals(data.getColVc(), d.getColVc());
		// multiple entries
		datas = SampleDAO.getInstance().search(new Query().setTop(10));
		Assert.assertTrue((datas.size()==1));
	}
	
	@Test
	public void testInitUnique() throws Exception {
		SampleDO data = new SampleDO();
		SampleDAO dao = SampleDAO.getInstance();
		data.setColVc("test");
		data.setColLong(1000);
		data.setColFloat(3.1415);
		data.setColDate(new Date());
		dao.save(data);
		SampleDO another = new SampleDO();
		dao.initUnique(another, new String[] {"col_vc", "col_long"}, new Object[] {"test", new Long(1000)});
		Assert.assertEquals(data.getId(), another.getId());
	}
	
	@Test
	public void testConnectionHelper() throws Exception {
		final SampleDAO dao = SampleDAO.getInstance();
		Connection conn = ConnectionHelper.getConnection(dao.getDbEnum());
		Connection anotherConn = ConnectionHelper.getConnection(dao.getDbEnum());
		// two connections should be exactly the same
		Assert.assertTrue((conn == anotherConn));
		ConnectionHelper.cleanupDBResources(null, null, conn);
		ConnectionHelper.cleanupDBResources(null, null, anotherConn);
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void testSearch4Collection() throws Exception {
		SampleDAO dao = SampleDAO.getInstance();
		SampleDO data = new SampleDO();
		data.setColVc("test");
		data.setColLong(1000);
		data.setColFloat(3.1415);
		Calendar c = Calendar.getInstance();
		c.set(Calendar.MILLISECOND, 0);
		Date d = c.getTime();
		data.setColDate(d);
		dao.save(data);
		SampleDO another = new SampleDO();
		another.setColVc("test");
		another.setColLong(1000);
		another.setColFloat(3.1415);
		c = Calendar.getInstance();
		c.set(Calendar.MILLISECOND, 0);
		d = c.getTime();
		another.setColDate(d);
		dao.save(another);
		List l = new ArrayList();
		Set s = new HashSet();
		//search method in AbstractDAO made non-static by Bin as part of DSConfig project
		SampleDAO.getInstance().search(SampleDatabaseEnum.TEST, l, new Query().select("id").from("test_dbframework").and("col_vc", "=", "test"), Integer.class);
		Assert.assertEquals(2, l.size());
		Assert.assertEquals(data.getId(), ((Integer) l.get(0)).intValue());
		Assert.assertEquals(another.getId(), ((Integer) l.get(1)).intValue());
		SampleDAO.getInstance().search(dao.getDbEnum(), s, new Query().select("DISTINCT col_vc").from("test_dbframework"), String.class);
		Assert.assertEquals(1, s.size());
		Assert.assertEquals(data.getColVc(), s.iterator().next());
		dao.delete(data);
		dao.delete(another);
	}

	@Override
	protected void afterInitialize(String home) throws InitializationException 
	{
		// delete db table
		try {
			ConnectionHelper.executeUpdate(SampleDatabaseEnum.TEST, "drop table test_dbframework if exists");
			ConnectionHelper.executeUpdate(SampleDatabaseEnum.TEST, "drop sequence seq_test_dbframework if exists");
		} catch (Exception e) {
		}

		// set up test db table
		try {
			ConnectionHelper.executeUpdate(SampleDatabaseEnum.TEST, "create table test_dbframework " +
						"(id integer, col_vc varchar(1000), col_long bigint, col_float float, " +
						"col_date datetime, col_clob varchar(1000), col_blob blob)");
			ConnectionHelper.executeUpdate(SampleDatabaseEnum.TEST, "create sequence seq_test_dbframework start with 1 increment by 1");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void afterShutdown() throws ShutdownException {
	}

	@Override
	protected BaseModule[] getDependentModules() {
		return new BaseModule[] {
				new DatabaseModule().addDatabases(new BaseDatabaseType[] {
						SampleDatabaseEnum.TEST
				}).getModule()
		};	
	}
	
}
