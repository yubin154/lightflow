package org.lightj.dal;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.lightj.BaseTestCase;
import org.lightj.dal.Query;
import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;
import org.lightj.initialization.ShutdownException;


public class TestDbQuery extends BaseTestCase {

	private boolean VERBOSE = false;

	@Test
	public void testAndInWithoutValues1() {
		Query query = new Query();

		query.and("f1", "IN", Arrays.asList(new String[] {}));

		if (VERBOSE) {
			System.out.println(query.daoString());
		}

		Assert.assertTrue(query.daoString().contains("WHERE  1=0"));
		Assert.assertEquals(query.getArgs().size(), 0);
	}

	@Test
	public void testAndInWithoutValues2() {
		Query query = new Query();

		query.and("f1", "=", "v1");
		query.and("f2", "IN", Arrays.asList(new String[] {}));

		if (VERBOSE) {
			System.out.println(query.daoString());
		}

		Assert.assertTrue(query.daoString().contains("WHERE   f1=? and 1=0"));
		Assert.assertEquals(query.getArgs().size(), 1);
	}

	@Test
	public void testAndInWithoutValues3() {
		Query query = new Query();

		query.and("f1", "=", "v1");
		query.and("f2", "IN", Arrays.asList(new String[] {}));
		query.and("f3", "<", "v2");

		if (VERBOSE) {
			System.out.println(query.daoString());
		}

		Assert.assertTrue(query.daoString().contains("WHERE   f1=? and 1=0 and  f3<?"));
		Assert.assertEquals(query.getArgs().size(), 2);
	}

	@Test
	public void testAndInOne() {
		Query query = new Query();

		query.and("f1", "=", "v1");
		query.and("f2", "IN", Arrays.asList(new String[] { "v2" }));

		if (VERBOSE) {
			System.out.println(query.daoString());
		}

		Assert.assertTrue(query.daoString().contains("WHERE   f1=? and  f2 IN (?)"));
		Assert.assertEquals(query.getArgs().size(), 2);
	}

	@Test
	public void testAndInMany() {
		Query query = new Query();

		query.and("f1", "=", "v1");
		query.and("f2", "IN", Arrays
				.asList(new String[] { "v21", "v22", "v23" }));

		if (VERBOSE) {
			System.out.println(query.daoString());
		}

		Assert.assertTrue(query.daoString()
				.contains("WHERE   f1=? and  f2 IN (?,?,?)"));
		Assert.assertEquals(query.getArgs().size(), 4);
	}

	@Test
	public void testOrInWithoutValues() {
		Query query = new Query();

		query.or("f1", "=", "v1");
		query.or("f2", "IN", Arrays.asList(new String[] {}));

		if (VERBOSE) {
			System.out.println(query.daoString());
		}

		Assert.assertTrue(query.daoString().contains("WHERE   f1=?   or 1=0"));
		Assert.assertEquals(query.getArgs().size(), 1);
	}

	@Test
	public void testOrInOne() {
		Query query = new Query();

		query.or("f1", "=", "v1");
		query.or("f2", "IN", Arrays.asList(new String[] { "v2" }));

		if (VERBOSE) {
			System.out.println(query.daoString());
		}

		Assert.assertTrue(query.daoString().contains("WHERE   f1=?   or  f2 IN (?)"));
		Assert.assertEquals(query.getArgs().size(), 2);
	}

	@Test
	public void testOrInMany() {
		Query query = new Query();

		query.or("f1", "=", "v1");
		query.or("f2", "IN", Arrays
				.asList(new String[] { "v21", "v22", "v23" }));

		if (VERBOSE) {
			System.out.println(query.daoString());
		}

		Assert.assertTrue(query.daoString().contains(
				"WHERE   f1=?   or  f2 IN (?,?,?)"));
		Assert.assertEquals(query.getArgs().size(), 4);
	}

	@Override
	protected void afterInitialize(String home) throws InitializationException {
	}

	@Override
	protected void afterShutdown() throws ShutdownException {
	}

	@Override
	protected BaseModule[] getDependentModules() {
		return null;
	}
}
