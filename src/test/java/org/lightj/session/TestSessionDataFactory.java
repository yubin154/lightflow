package org.lightj.session;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.junit.Assert;
import org.junit.Test;
import org.lightj.BaseTestCase;
import org.lightj.example.dal.SampleDatabaseEnum;
import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;
import org.lightj.initialization.ShutdownException;
import org.lightj.session.dal.ISessionData;
import org.lightj.session.dal.ISessionDataManager;
import org.lightj.session.dal.ISessionMetaData;
import org.lightj.session.dal.ISessionMetaDataManager;
import org.lightj.session.dal.SessionDataFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


@SuppressWarnings({"rawtypes", "unchecked"})
public class TestSessionDataFactory extends BaseTestCase {

	@Test
	public void testSessionDataManager() throws Exception {
		ISessionDataManager sdm = SessionDataFactory.getInstance().getDataManager();
		ISessionData sd = sdm.newInstance();
		sd.setCreationDate(new Date());
		sd.setCurrentAction("step1");
		sd.setEndDate(new Date());
		sd.setLastModified(new Date());
		sd.setNextAction("step2");
//		sd.setParentId(1);
		sd.setRequesterKey("test@test.com");
		sd.setRunBy("lightj");
		sd.setStatus("success");
		sd.setTargetKey("target");
		sd.setFlowKey(UUID.randomUUID().toString());
		sd.setType("1");
		sd.setFlowState(FlowState.Completed);
		sd.setFlowResult(FlowResult.Success);
		sdm.save(sd);
		Assert.assertTrue("primary key is not populated", sd.getPrimaryKey()>0);
		sd.setCurrentAction("step3");
		sd.setNextAction(null);
		sd.setEndDate(null);
		sdm.save(sd);
		ISessionData sd1 = sdm.findById(sd.getFlowId());
		Assert.assertEquals(sd.getFlowId(), sd1.getFlowId());
		Assert.assertEquals("step3", sd1.getCurrentAction());
		Assert.assertNull(sd1.getNextAction());
		ISessionData search = sdm.findByKey(sd.getFlowKey());
		Assert.assertEquals(sd.getFlowId(), search.getFlowId());
		sdm.delete(sd);
		Assert.assertTrue(sd.getPrimaryKey()<=0);
		search = sdm.findById(sd.getFlowId());
	}
	
	@Test
	public void testSessionMetaDataManager() throws Exception {
		// prep
		ISessionDataManager sdm = SessionDataFactory.getInstance().getDataManager();
		ISessionData sd = sdm.newInstance();
		sd.setCreationDate(new Date());
		sd.setCurrentAction("step1");
		sd.setEndDate(new Date());
		sd.setLastModified(new Date());
		sd.setNextAction("step2");
//		sd.setParentId(1);
		sd.setRequesterKey("lightj@ebay.com");
		sd.setRunBy("lightj");
		sd.setStatus("success");
		sd.setTargetKey("target");
		sd.setType("1");
		sd.setFlowState(FlowState.Completed);
		sd.setFlowResult(FlowResult.Success);
		sdm.save(sd);
		Assert.assertTrue("primary key is not populated", sd.getPrimaryKey()>0);
		long sessId = sd.getFlowId();

		ISessionMetaDataManager smdm = SessionDataFactory.getInstance().getMetaDataManager();
		ISessionMetaData smd1 = smdm.newInstance();
		smd1.setName("metadata1");
		smd1.setStrValue("value1");
		smd1.setFlowId(sessId);
		ISessionMetaData smd2 = smdm.newInstance();
		smd2.setName("metadata2");
		smd2.setBlobValue(new Integer(1));
		smd2.setFlowId(sessId);
		// insert
		smdm.save(smd1);
		smdm.save(smd2);
		Assert.assertTrue("primary key is not populated", smd1.getPrimaryKey()>0);
		Assert.assertTrue("primary key is not populated", smd2.getPrimaryKey()>0);
		smd1.setStrValue("value2");
		smd2.setBlobValue(new Integer(2));
		// update and search
		smdm.save(smd1);
		smdm.save(smd2);
		// search by query
		List<ISessionMetaData> searches = smdm.findByFlowId(sessId);
		Assert.assertEquals(2, searches.size());
		for (ISessionMetaData smd : searches) {
			if (smd.getStrValue() != null)  {
				Assert.assertEquals(smd1.getStrValue(), smd.getStrValue());
			} else {
				Assert.assertEquals(smd2.getBlobValue(), smd.getBlobValue());
			}
		}
		// delete
		smdm.delete(smd1);
		smdm.delete(smd2);
		Assert.assertEquals(0, smd1.getFlowMetaId());
		Assert.assertEquals(0, smd2.getFlowMetaId());
		searches = smdm.findByFlowId(sessId);
		Assert.assertEquals(0, searches.size());

		// cleanup
		sdm.delete(sd);
		Assert.assertTrue(sd.getPrimaryKey()<=0);
	}
	
	@Test
	public void testSessionStepLogManager() throws Exception {
		// prep
		ISessionDataManager sdm = SessionDataFactory.getInstance().getDataManager();
		ISessionData sd = sdm.newInstance();
		sd.setCreationDate(new Date());
		sd.setCurrentAction("step1");
		sd.setEndDate(new Date());
		sd.setLastModified(new Date());
		sd.setNextAction("step2");
//		sd.setParentId(1);
		sd.setRequesterKey("lightj@ebay.com");
		sd.setRunBy("lightj");
		sd.setStatus("success");
		sd.setTargetKey("target");
		sd.setType("1");
		sd.setFlowState(FlowState.Completed);
		sd.setFlowResult(FlowResult.Success);
		sdm.save(sd);
		Assert.assertTrue("primary key is not populated", sd.getPrimaryKey()>0);

		// cleanup
		sdm.delete(sd);
		Assert.assertTrue(sd.getPrimaryKey()<=0);
	}

	@Override
	protected void afterInitialize(String home) throws InitializationException {
	}

	@Override
	protected void afterShutdown() throws ShutdownException {
	}

	@Override
	protected BaseModule[] getDependentModules() {
		AnnotationConfigApplicationContext flowCtx = new AnnotationConfigApplicationContext("org.lightj.example");
		return new BaseModule[] {
				new FlowModule().setDb(SampleDatabaseEnum.TEST)
								.setSpringContext(flowCtx)
								.setExectuorService(Executors.newFixedThreadPool(5))
								.getModule(),
		};
	}

}
