package org.lightj.session;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.lightj.BaseTestCase;
import org.lightj.example.dal.SampleDatabaseEnum;
import org.lightj.example.session.DummyFlowContext;
import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;
import org.lightj.initialization.ShutdownException;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepImpl;
import org.lightj.task.ExecutableTask;
import org.lightj.task.TaskExecutionException;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TestFlowContext extends BaseTestCase {

	@Test
	public void testContext() throws Exception {
		DummyFlowContext test = new DummyFlowContext();
		test.setParam1(1);
		test.setParam2("test");
		test.setParam3(new Date());
		test.setParam4(new HashMap<String, String>());
		test.setParam5(new ArrayList<String>());
		IFlowStep step = new StepImpl();
		ExecutableTask<DummyFlowContext> task = new ExecutableTask<DummyFlowContext>(){
			@Override
			public TaskResult execute() throws TaskExecutionException {
				return null;
		}};
		test.addStep(step);		
		test.addTask(task.getTaskId(), task);
		test.saveTaskResult(step.getStepId(), task, task.createTaskResult(TaskResultEnum.Success, "success"));
		test.setStepComplete(step.getStepId());
		test.prepareSave();
		assertEquals(7, test.getDirtyMetas().size());
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
