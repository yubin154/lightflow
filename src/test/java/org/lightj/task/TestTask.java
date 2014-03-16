package org.lightj.task;

import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.lightj.BaseTestCase;
import org.lightj.example.task.HttpTaskBuilder;
import org.lightj.example.task.HttpTaskRequest;
import org.lightj.initialization.BaseModule;
import org.lightj.session.FlowContext;
import org.lightj.task.asynchttp.AsyncHttpTask.HttpMethod;
import org.lightj.task.asynchttp.UrlTemplate;
import org.lightj.util.ConcurrentUtil;
import org.lightj.util.SpringContextUtil;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@SuppressWarnings("rawtypes")
public class TestTask extends BaseTestCase {
	
	/** pause lock */
	protected ReentrantLock lock = new ReentrantLock();
	
	/** pause condition */
	protected Condition cond = lock.newCondition();
	

	/**
	 * ebay specific
	 * @throws Exception
	 */
	public void testStandaloneTaskExecutor() throws Exception {
		// ebay specific
//		String[] sites = new String[] {"slc4b01c-9dee.stratus.slc.ebay.com","slc4b01c-accc.stratus.slc.ebay.com"};
//		// 1 async group http req
//		HttpTaskRequest tw2 = new HttpTaskRequest();
//		tw2.setTaskType("asyncpoll");
//		tw2.setHttpClientType("httpClient");
//		tw2.setExecutionOption(new ExecuteOption());
//		UrlTemplate template = new UrlTemplate("https://#host:12020/admin/executeCmd", HttpMethod.POST, "{\"cmd\": \"netstat\", \"params\": \"-a\"}");
//		template.addHeader("Authorization", "Basic YWdlbnQ6dG95YWdlbnQ=")
//				.addHeader("content-type", "application/json")
//				.addHeader("AUTHZ_TOKEN", "donoevil");
//		tw2.setUrlTemplate(template);
//		tw2.setHosts(sites);
//		
//		tw2.setMonitorOption(new MonitorOption(1000, 10000));
//		tw2.setPollTemplate(new UrlTemplate("https://#host:12020/status/#uuid"));
//		tw2.setPollProcessorName("agentPollProcessor");
		
		// general
		String[] sites = new String[] {"www.yahoo.com","www.facebook.com"};
		// 1 async group http req
		HttpTaskRequest tw2 = new HttpTaskRequest();
		tw2.setTaskType("asyncpoll");
		tw2.setHttpClientType("httpClient");
		tw2.setExecutionOption(new ExecuteOption());
		UrlTemplate template = new UrlTemplate("https://#host");
		tw2.setUrlTemplate(template);
		tw2.setHosts(sites);
		
		tw2.setMonitorOption(new MonitorOption(1000, 10000));
		tw2.setPollTemplate(new UrlTemplate("https://#host"));
		tw2.setPollProcessorName("dummyPollProcessor");

		StandaloneTaskListener listener = new StandaloneTaskListener();
		listener.setDelegateHandler(new SimpleTaskEventHandler<FlowContext>() {

			@Override
			public void executeOnResult(FlowContext ctx, Task task, TaskResult result) {
				System.out.print(String.format("%s,%s", result, result.<String>getRealResult()));
			}

			@Override
			public TaskResultEnum executeOnCompleted(FlowContext ctx,
					Map<String, TaskResult> results) {
				ConcurrentUtil.signal(lock, cond);
				return super.executeOnCompleted(ctx, results);
			}
		});
		new StandaloneTaskExecutor(null, listener, HttpTaskBuilder.buildTask(tw2)).execute();
		ConcurrentUtil.wait(lock, cond, 10000);
	}

	@Override
	protected BaseModule[] getDependentModules() {
		AnnotationConfigApplicationContext flowCtx = new AnnotationConfigApplicationContext("org.lightj.example.task");
		SpringContextUtil.registerContext("TaskModule", flowCtx);
		return new BaseModule[] {new TaskModule().getModule()};
	}

}
