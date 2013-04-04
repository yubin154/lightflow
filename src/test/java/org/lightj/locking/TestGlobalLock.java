package org.lightj.locking;

import org.junit.Assert;
import org.junit.Test;
import org.lightj.BaseTestCase;
import org.lightj.dal.DatabaseModule;
import org.lightj.dal.ITransactional;
import org.lightj.example.dal.SampleDatabaseEnum;
import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;
import org.lightj.initialization.ShutdownException;


public class TestGlobalLock extends BaseTestCase {

	@Test
	public void testSemaphoreSuccess() throws Exception {
		
		ILockManager lockManager = new LockManagerImpl(SampleDatabaseEnum.LOCK);
		
		lockManager.synchronizeObject(TestGlobalLock.class.getName(), 
				new ITransactional() {

					public void execute() throws Throwable {
						Thread.sleep(1000);
					}
						
				});
			
	}
	
	@Test
	public void testSemaphoreFailure() throws Exception {
		ILockManager lockManager = new LockManagerImpl(SampleDatabaseEnum.LOCK);
		
		try {
			lockManager.synchronizeObject(TestGlobalLock.class.getName(), 
					new ITransactional() {

						public void execute() throws Throwable {
							// insert a dummy record into the same locking table
							Thread.sleep(1000);
							throw new RuntimeException("some failure");
						}
						
					});
			
		} 
		catch (LockException e) {
			Assert.assertTrue(e.getMessage().indexOf("some failure") >= 0);
		}
		
	}
	
	public void testSemaphoreConcurrency() throws Exception {

		final ILockManager lockManager = new LockManagerImpl(SampleDatabaseEnum.LOCK);
		final int[] counter = new int[2];
		
		for (int i = 0; i < 10; i++) {
			new Thread(new Runnable() {

				public void run() {
					try {
						lockManager.synchronizeObject(TestGlobalLock.class.getName(), 
								new ITransactional() {

									public void execute() throws Throwable {
										try {
											counter[0]++;
											counter[1]++;
											Thread.sleep(100);
										} finally {
											counter[0]--;
										}
									}
									
								});
					} catch (RuntimeException e) {
					} catch (LockException e) {
					}
				}
				
			}).start();
		}
		
		while (counter[1] < 9) {
			Assert.assertTrue("Global semaphore should not allow concurrent access", counter[0] <= 1);
			Thread.sleep(50);
		}
		Assert.assertEquals(9, counter[1]);
	}

	@Test
	public void testLockSuccess() throws Exception {
		
		ILockManager lockManager = new LockManagerImpl(SampleDatabaseEnum.LOCK);
		String key = TestGlobalLock.class.getName();
		lockManager.lock(key, true);
		Assert.assertEquals(1, lockManager.getLockCount(key));
		lockManager.unlock(key);
		Assert.assertEquals(0, lockManager.getLockCount(key));
			
	}

	@Override
	protected void afterInitialize(String home) throws InitializationException {
	}

	@Override
	protected void afterShutdown() throws ShutdownException {
	}

	@Override
	protected BaseModule[] getDependentModules() {
		return new BaseModule[] {new DatabaseModule().addDatabases(SampleDatabaseEnum.LOCK).getModule()};
	}

}
