package org.lightj.session;

import java.util.concurrent.Executors;

import org.junit.Test;
import org.lightj.BaseTestCase;
import org.lightj.example.dal.LocalDatabaseEnum;
import org.lightj.example.session.helloworld.HelloWorldFlow;
import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;
import org.lightj.initialization.ShutdownException;
import org.lightj.session.eventlistener.FlowStatistics;
import org.lightj.session.eventlistener.FlowStatsTracker;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepImpl;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TestFlowStats extends BaseTestCase {

	@Test
	public void testStats() throws Exception {
		HelloWorldFlow session = FlowSessionFactory.getInstance().createSession(HelloWorldFlow.class);
		FlowStatistics stats = new FlowStatistics(session.getOrderedStepProperties(), "start");
		String[] steps = {"start", "stop"};
		for (String s : steps) {
			stats.updatePercentComplete(s);
			System.out.println(s + ':' + stats.getPercentComplete());
		}
		assertEquals(100, stats.getPercentComplete());
		stats.setPercentComplete(0);
		assertEquals(0, stats.getPercentComplete());
	}
	
	@Test
	public void testStatsTracker() throws Exception {
		FlowStatsTracker statsTracker = new FlowStatsTracker();
		HelloWorldFlow session = FlowSessionFactory.getInstance().createSession(HelloWorldFlow.class);
		statsTracker.handleFlowEvent(FlowEvent.start, session, null);
		assertEquals(0, session.getPercentComplete());
		
		IFlowStep step = new StepImpl();
		step.setStepName("asyncTaskStep");
		statsTracker.handleStepEvent(FlowEvent.stepExit, session, step, null);
		assertTrue(session.getPercentComplete()>0 && session.getPercentComplete()<100);
		
		step.setStepName("dummy");
		statsTracker.handleStepEvent(FlowEvent.stepExit, session, step, null);
		assertTrue(session.getPercentComplete()>0 && session.getPercentComplete()<100);
		
		statsTracker.handleFlowEvent(FlowEvent.stop, session, null);
		assertEquals(100, session.getPercentComplete());
		
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
				new FlowModule().setDb(LocalDatabaseEnum.TESTMEMDB)
								.setSpringContext(flowCtx)
								.setExectuorService(Executors.newFixedThreadPool(5))
								.getModule(),
		};
	}
}
