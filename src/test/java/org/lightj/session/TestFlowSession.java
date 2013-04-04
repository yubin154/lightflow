package org.lightj.session;

import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.Assert;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.lightj.BaseTestCase;
import org.lightj.dal.SimpleLocatable;
import org.lightj.example.dal.SampleDatabaseEnum;
import org.lightj.example.session.HelloWorldFlow;
import org.lightj.example.session.HelloWorldFlowEventListener;
import org.lightj.example.session.HelloWorldFlowStepsImpl;
import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;
import org.lightj.initialization.ShutdownException;
import org.lightj.locking.LockManagerImpl;
import org.lightj.util.ConcurrentUtil;
import org.lightj.util.SpringContextUtil;
import org.springframework.context.ApplicationContext;


public class TestFlowSession extends BaseTestCase {

	/** pause lock */
	protected ReentrantLock lock = new ReentrantLock();
	
	/** pause condition */
	protected Condition cond = lock.newCondition();

	@Test
	public void testHelloWorldWithDI() throws Exception {
		ApplicationContext ctx = SpringContextUtil.loadContext("helloworld", "config/org/lightj/session/context-helloworld-flow.xml");
		HelloWorldFlow session = (HelloWorldFlow) ctx.getBean("helloWorldFlow");
		
		// use DI to set step impl
		FlowSessionFactory.getInstance().save(session);
		session.addEventListener(new HelloWorldFlowEventListener(lock, cond));
		session.runFlow();
		ConcurrentUtil.wait(lock, cond);
		Assert.assertEquals(1, session.getSessionContext().getTaskCount());
		Assert.assertEquals(2, session.getSessionContext().getSplitCount());
		Assert.assertEquals(4, session.getSessionContext().getRetryCount());
		Assert.assertEquals(1, session.getSessionContext().getTimeoutCount());
		Assert.assertEquals(10, session.getSessionContext().getBatchCount());
		
		Object history = FlowSessionFactory.getInstance().findByKey(session.getKey()).getSessionContext().getExecutionLogs();
		System.out.println(new ObjectMapper().writeValueAsString(history));
	}

	@Test
	public void testHelloWorld() throws Exception {
		HelloWorldFlow session = FlowSessionFactory.getInstance().createSession(
				HelloWorldFlow.class, new SimpleLocatable(Long.toString(System.currentTimeMillis())), new SimpleLocatable());
		// traditional way of setting the step impl
		session.setAsyncTaskStep(HelloWorldFlowStepsImpl.buildAsyncTaskStep());
		session.setSessionJoinStep(HelloWorldFlowStepsImpl.buildJoinStep());
		session.setDelayStep(HelloWorldFlowStepsImpl.buildDelayStep());
		session.setRetryStep(HelloWorldFlowStepsImpl.buildRetryStep());
		session.setTimeoutStep(HelloWorldFlowStepsImpl.buildTimeoutStep());
		session.setActorPollStep(HelloWorldFlowStepsImpl.buildActorPollStep());
		session.setActorStep(HelloWorldFlowStepsImpl.buildActorStep());
		session.setActorBatchStep(HelloWorldFlowStepsImpl.buildActorBatchStep());
		FlowSessionFactory.getInstance().save(session);
		session.addEventListener(new HelloWorldFlowEventListener(lock, cond));
		session.runFlow();
		ConcurrentUtil.wait(lock, cond);
		Assert.assertEquals(1, session.getSessionContext().getTaskCount());
		Assert.assertEquals(2, session.getSessionContext().getSplitCount());
		Assert.assertEquals(4, session.getSessionContext().getRetryCount());
		Assert.assertEquals(1, session.getSessionContext().getTimeoutCount());
		Assert.assertEquals(10, session.getSessionContext().getBatchCount());
	}

	@Override
	protected void afterInitialize(String home) throws InitializationException {
	}

	@Override
	protected void afterShutdown() throws ShutdownException {
	}

	@Override
	protected BaseModule[] getDependentModules() {
		String ctxPath = "config/org/lightj/session/context-flow.xml";
		return new BaseModule[] {
				new FlowModule().setDb(SampleDatabaseEnum.QRTZ)
								.enableCluster(new LockManagerImpl(SampleDatabaseEnum.QRTZ))
								.setExectuorService(Executors.newFixedThreadPool(5))
								.setSpringContext(ctxPath)
								.getModule(),
		};
	}
}
