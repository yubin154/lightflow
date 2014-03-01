package org.lightj.session;

import java.util.concurrent.Executors;

import org.junit.Test;
import org.lightj.BaseTestCase;
import org.lightj.example.dal.SampleDatabaseEnum;
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
		FlowStatistics stats = new FlowStatistics(HelloWorldFlow.steps.class, "start");
		for (HelloWorldFlow.steps s : HelloWorldFlow.steps.values()) {
			stats.updatePercentComplete(s.name());
			System.out.println(s.name() + ':' + stats.getPercentComplete());
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
		step.setStepName(HelloWorldFlow.steps.asyncTaskStep.name());
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
				new FlowModule().setDb(SampleDatabaseEnum.TEST)
								.setSpringContext(flowCtx)
								.setExectuorService(Executors.newFixedThreadPool(5))
								.getModule(),
		};
	}
}
