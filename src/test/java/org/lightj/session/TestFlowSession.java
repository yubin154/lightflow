package org.lightj.session;

import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.Assert;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.lightj.BaseTestCase;
import org.lightj.example.dal.SampleDatabaseEnum;
import org.lightj.example.session.HelloWorldFlow;
import org.lightj.example.session.HelloWorldFlow.steps;
import org.lightj.example.session.HelloWorldFlowEventListener;
import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;
import org.lightj.initialization.ShutdownException;
import org.lightj.locking.LockManagerImpl;
import org.lightj.util.ConcurrentUtil;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class TestFlowSession extends BaseTestCase {

	/** pause lock */
	protected ReentrantLock lock = new ReentrantLock();
	
	/** pause condition */
	protected Condition cond = lock.newCondition();

	@Test
	public void testHelloWorld() throws Exception {
		HelloWorldFlow session = FlowSessionFactory.getInstance().createSession(HelloWorldFlow.class);
		session.save();
		session.addEventListener(new HelloWorldFlowEventListener(lock, cond));
		session.runFlow();
		ConcurrentUtil.wait(lock, cond);
		Assert.assertEquals(1, session.getSessionContext().getTaskCount());
		Assert.assertEquals(2, session.getSessionContext().getSplitCount());
		Assert.assertEquals(2, session.getSessionContext().getRetryCount());
		Assert.assertEquals(1, session.getSessionContext().getTimeoutCount());
		Assert.assertEquals(2, session.getSessionContext().getBatchCount());
		Assert.assertEquals(0, session.getSessionContext().getErrorStepCount());
		
		System.out.println(new ObjectMapper().writeValueAsString(session.getFlowInfo()));
	}

	@Test
	public void testHelloWorldFailureRuntime() throws Exception {
		HelloWorldFlow session = FlowSessionFactory.getInstance().createSession(HelloWorldFlow.class);
		session.getSessionContext().setInjectFailure(true);
		session.getSessionContext().setControlledFailure(false);
		
		// use DI to set step impl
		session.save();
		session.addEventListener(new HelloWorldFlowEventListener(lock, cond));
		session.runFlow();
		ConcurrentUtil.wait(lock, cond);
		Assert.assertEquals(1, session.getSessionContext().getErrorStepCount());
		Assert.assertEquals(HelloWorldFlow.steps.testFailureStep.name(), session.getCurrentAction());
		Assert.assertEquals(FlowResult.Failed, session.getResult());
		Assert.assertEquals(FlowState.Completed, session.getState());
	}
	
	@Test
	public void testHelloWorldFailureControlled() throws Exception {
		HelloWorldFlow session = FlowSessionFactory.getInstance().createSession(HelloWorldFlow.class);
		session.getSessionContext().setInjectFailure(true);
		
		// use DI to set step impl
		session.save();
		session.addEventListener(new HelloWorldFlowEventListener(lock, cond));
		session.runFlow();
		ConcurrentUtil.wait(lock, cond);
		Assert.assertEquals(1, session.getSessionContext().getErrorStepCount());
		Assert.assertEquals(HelloWorldFlow.steps.testFailureStep.name(), session.getCurrentAction());
		Assert.assertEquals(FlowResult.Failed, session.getResult());
		Assert.assertEquals(FlowState.Completed, session.getState());
	}

	@Test
	public void testPauseResume() throws Exception {
		HelloWorldFlow session = FlowSessionFactory.getInstance().createSession(HelloWorldFlow.class);
		session.getSessionContext().setInjectFailure(true);
		session.getSessionContext().setControlledFailure(false);
		// pause on error
		session.getSessionContext().setPauseOnError(true);
		// use DI to set step impl
		session.save();
		session.addEventListener(new HelloWorldFlowEventListener(lock, cond));
		session.runFlow();
		ConcurrentUtil.wait(lock, cond);
		Assert.assertEquals(HelloWorldFlow.steps.testFailureStep.name(), session.getCurrentAction());
		Assert.assertEquals(FlowResult.Failed, session.getResult());
		Assert.assertEquals(FlowState.Paused, session.getState());
		
		// reset current step to the desireable step and resume flow
		HelloWorldFlow session1 = (HelloWorldFlow) FlowSessionFactory.getInstance().findByKey(session.getKey());
		session1.setCurrentAction(steps.testFailureStep.name());
		// don't pause this time
		session1.getSessionContext().setPauseOnError(false);
		session1.addEventListener(new HelloWorldFlowEventListener(lock, cond));
		session1.runFlow();
		ConcurrentUtil.wait(lock, cond);
		
		System.out.println(new ObjectMapper().writeValueAsString(session1.getFlowInfo()));

		// second time we did not set private error flag in context, so flow did not go to the error step
		Assert.assertEquals(1, session1.getSessionContext().getErrorStepCount());
		Assert.assertEquals(HelloWorldFlow.steps.stop.name(), session1.getCurrentAction());
		Assert.assertEquals(FlowResult.Success, session1.getResult());
		Assert.assertEquals(FlowState.Completed, session1.getState());
		
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
								.enableCluster(new LockManagerImpl(SampleDatabaseEnum.TEST))
								.setSpringContext(flowCtx)
								.setExectuorService(Executors.newFixedThreadPool(5))
								.getModule(),
		};
	}
}
