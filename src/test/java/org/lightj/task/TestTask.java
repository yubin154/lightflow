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
import org.lightj.task.asynchttp.SimpleHttpResponse;
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
		UrlTemplate template = new UrlTemplate(UrlTemplate.encodeAllVariables("https://host", "host"), HttpMethod.GET, null);
		tw2.setUrlTemplate(template);
		tw2.setHosts(sites);
		
		tw2.setMonitorOption(new MonitorOption(1000, 10000));
		tw2.setPollTemplate(new UrlTemplate(UrlTemplate.encodeAllVariables("https://host", "host")));
		tw2.setPollProcessorName("dummyPollProcessor");

		// 1 async group http req
		HttpTaskRequest tw3 = new HttpTaskRequest();
		tw3.setTaskType("async");
		tw3.setHttpClientType("httpClient");
		tw3.setExecutionOption(new ExecuteOption());
		template = new UrlTemplate(UrlTemplate.encodeAllVariables("http://host/q?s=ebay&ql=1", "host"), HttpMethod.GET, null);
		template.addParameters("s", UrlTemplate.encodeIfNeeded("s")).addParameters("ql", "1");
		tw3.setUrlTemplate(template);
		tw3.setHost("finance.yahoo.com");
		tw3.addTemplateValueAsMap("s", "ebay");
		
		StandaloneTaskListener listener = new StandaloneTaskListener();
		listener.setDelegateHandler(new SimpleTaskEventHandler<FlowContext>() {

			@Override
			public void executeOnResult(FlowContext ctx, Task task, TaskResult result) {
				System.out.print(String.format("%s,%s", result.getStatus(), 
						StringUtil.trimToLength(result.<SimpleHttpResponse>getRealResult().getResponseBody(), 200)));
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

	public void testParameter() throws Exception {
		HttpTaskRequest tw = new HttpTaskRequest();
		tw.setTaskType("async");
		tw.setHttpClientType("httpClient");
		tw.setExecutionOption(new ExecuteOption());
		UrlTemplate template = new UrlTemplate(UrlTemplate.encodeAllVariables("https://host/q", "host"), HttpMethod.GET, null);
		template.addParameters("s", UrlTemplate.encodeIfNeeded("s")).addParameters("ql", "1");
		tw.setUrlTemplate(template);
		tw.setHost("finance.yahoo.com");
		tw.addTemplateValueAsMap("s", "ebay");
		
		StandaloneTaskListener listener = new StandaloneTaskListener();
		listener.setDelegateHandler(new SimpleTaskEventHandler<FlowContext>() {

			@Override
			public void executeOnResult(FlowContext ctx, Task task, TaskResult result) {
				System.out.print(String.format("%s,%s", result.getStatus(), 
						StringUtil.trimToLength(result.<SimpleHttpResponse>getRealResult().getResponseBody(), 200)));
			}

			@Override
			public TaskResultEnum executeOnCompleted(FlowContext ctx,
					Map<String, TaskResult> results) {
				ConcurrentUtil.signal(lock, cond);
				return super.executeOnCompleted(ctx, results);
			}
		});
		new StandaloneTaskExecutor(null, listener, HttpTaskBuilder.buildTask(tw)).execute();
		ConcurrentUtil.wait(lock, cond, 10000);
	}
	
	@Override
	protected BaseModule[] getDependentModules() {
		AnnotationConfigApplicationContext flowCtx = new AnnotationConfigApplicationContext("org.lightj.example.task");
		SpringContextUtil.registerContext("TaskModule", flowCtx);
		return new BaseModule[] {new TaskModule().getModule()};
	}

}
