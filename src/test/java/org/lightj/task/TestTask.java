package org.lightj.task;

import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.lightj.BaseTestCase;
import org.lightj.example.session.simplehttpflow.HttpTaskUtil;
import org.lightj.example.session.simplehttpflow.HttpTaskUtil.HttpTaskWrapper;
import org.lightj.initialization.BaseModule;
import org.lightj.session.FlowContext;
import org.lightj.session.FlowModule;
import org.lightj.session.step.StepTransition;
import org.lightj.task.asynchttp.UrlTemplate;
import org.lightj.util.ConcurrentUtil;
import org.lightj.util.SpringContextUtil;
import org.lightj.util.StringUtil;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TestTask extends BaseTestCase {
	
	/** pause lock */
	protected ReentrantLock lock = new ReentrantLock();
	
	/** pause condition */
	protected Condition cond = lock.newCondition();
	

	public void testStandaloneTaskExecutor() throws Exception {
		// 2 async http req
		String[] sites = new String[] {"www.yahoo.com","www.facebook.com"};
		// 1 async group http req
		HttpTaskWrapper tw2 = new HttpTaskWrapper();
		tw2.setTaskType("asyncgroup");
		tw2.setHttpClientType("httpClient");
		tw2.setExecutionOption(new ExecuteOption());
		tw2.setUrlTemplate(new UrlTemplate("https://#host"));
		tw2.setFanoutFactor("#host");
		tw2.setFanoutValues(sites);
		StandaloneTaskListener listener = new StandaloneTaskListener();
		listener.setDelegateHandler(new ITaskEventHandler<FlowContext>() {

			@Override
			public void executeOnCreated(FlowContext ctx, Task task) {
			}

			@Override
			public void executeOnSubmitted(FlowContext ctx, Task task) {
			}

			@Override
			public void executeOnResult(FlowContext ctx, Task task,
					TaskResult result) {
				System.out.print(StringUtil.trimToLength((String) result.getRealResult(), 100));
			}

			@Override
			public StepTransition executeOnCompleted(FlowContext ctx,
					Map<String, TaskResult> results) {
				ConcurrentUtil.signal(lock, cond);
				return null;
			}
		});
		new StandaloneTaskExecutor(null, listener, HttpTaskUtil.buildTask(tw2)).execute();
		ConcurrentUtil.wait(lock, cond, 15000);
	}

	@Override
	protected BaseModule[] getDependentModules() {
		AnnotationConfigApplicationContext flowCtx = new AnnotationConfigApplicationContext("org.lightj.example");
		SpringContextUtil.registerContext(FlowModule.FLOW_CTX, flowCtx);
		return new BaseModule[] {new TaskModule().getModule()};
	}

}
