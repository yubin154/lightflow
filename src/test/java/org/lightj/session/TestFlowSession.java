package org.lightj.session;

import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.Assert;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.lightj.BaseTestCase;
import org.lightj.example.dal.LocalDatabaseEnum;
import org.lightj.example.session.helloworld.HelloWorldFlow;
import org.lightj.example.session.helloworld.HelloWorldFlowEventListener;
import org.lightj.example.session.simplehttpflow.SimpleHttpFlow;
import org.lightj.example.task.HttpTaskRequest;
import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;
import org.lightj.initialization.ShutdownException;
import org.lightj.task.ExecuteOption;
import org.lightj.task.MonitorOption;
import org.lightj.task.asynchttp.UrlTemplate;
import org.lightj.util.ConcurrentUtil;
import org.lightj.util.JsonUtil;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class TestFlowSession extends BaseTestCase {

	/** pause lock */
	protected ReentrantLock lock = new ReentrantLock();
	
	/** pause condition */
	protected Condition cond = lock.newCondition();
	
	@Test
	public void testSimpleHttpFlow() throws Exception {
		// create an instance of skeleton flow, fill in the flesh for each steps
		SimpleHttpFlow flow = FlowSessionFactory.getInstance().createSession(SimpleHttpFlow.class);
		
		// 2 async http req
		String[] sites = new String[] {"www.yahoo.com","www.facebook.com"};
		for (int i = 0 ; i < 2; i++) {
			HttpTaskRequest tw = new HttpTaskRequest();
			tw.setTaskType("async");
			tw.setHttpClientType("httpClient");
			tw.setExecutionOption(new ExecuteOption());
			tw.setUrlTemplate(new UrlTemplate(UrlTemplate.encodeAllVariables("https://host", "host")));
			tw.setHost(sites[i]);
			flow.getSessionContext().addUserRequests(tw);
		}
		
		// 1 asyncpoll http req
		HttpTaskRequest tw1 = new HttpTaskRequest();
		tw1.setTaskType("asyncpoll");
		tw1.setHttpClientType("httpClient");
		tw1.setExecutionOption(new ExecuteOption());
		
		// ebay specific
//		UrlTemplate template = new UrlTemplate("https://#host:12020/admin/executeCmd", HttpMethod.POST, "{\"cmd\": \"netstat\", \"params\": \"-a\"}");
//		template.addHeader("Authorization", "Basic YWdlbnQ6dG95YWdlbnQ=")
//				.addHeader("content-type", "application/json")
//				.addHeader("AUTHZ_TOKEN", "donoevil");
//		tw1.setUrlTemplate(template);
//		
//		HashMap<String, String> tv = new HashMap<String, String>();
//		tv.put("#host", "phx7b02c-2dac.stratus.phx.ebay.com");
//		tw1.setTemplateValues(tv);
//		tw1.setPollProcessorName("agentPollProcessor");

		UrlTemplate template = new UrlTemplate(UrlTemplate.encodeAllVariables("https://host", "host"));
		tw1.setUrlTemplate(template);
		tw1.setHost("www.yahoo.com");

		tw1.setMonitorOption(new MonitorOption(1000, 10000));
		tw1.setPollTemplate(new UrlTemplate(UrlTemplate.encodeAllVariables("https://host", "host")));
		tw1.setPollProcessorName("dummyPollProcessor");

		flow.getSessionContext().addUserRequests(tw1);

		// 1 async group http req
		HttpTaskRequest tw2 = new HttpTaskRequest();
		tw2.setTaskType("async");
		tw2.setHttpClientType("httpClient");
		tw2.setExecutionOption(new ExecuteOption());
		tw2.setUrlTemplate(new UrlTemplate(UrlTemplate.encodeAllVariables("https://host", "host")));
		tw2.setHosts(sites);
		flow.getSessionContext().addUserRequests(tw2);
		
		// 1 asyncpoll http req
		HttpTaskRequest tw3 = new HttpTaskRequest();
		tw3.setTaskType("asyncpoll");
		tw3.setHttpClientType("httpClient");
		tw3.setExecutionOption(new ExecuteOption());
		tw3.setUrlTemplate(new UrlTemplate(UrlTemplate.encodeAllVariables("https://host", "host")));
		tw3.setHosts(sites);

		tw3.setMonitorOption(new MonitorOption(1000, 5000));
		tw3.setPollTemplate(new UrlTemplate(UrlTemplate.encodeAllVariables("https://host", "host")));
		tw3.setPollProcessorName("dummyPollProcessor");

		flow.getSessionContext().addUserRequests(tw3);

		flow.save();
		// kick off flow
		flow.runFlow();
		
		// checking flow state and print progress
		while (!flow.getState().isComplete()) {
			System.out.println(flow.getFlowInfo().getProgress());
			Thread.sleep(1000);
		}
		System.out.println(JsonUtil.encode(flow.getFlowInfo()));
	}

	@Test
	public void testHelloWorld() throws Exception {
		HelloWorldFlow session = FlowSessionFactory.getInstance().createSession(HelloWorldFlow.class);
		session.getSessionContext().setGoodHosts("www.yahoo.com", "www.yahoo.com");
		session.save();
		session.addEventListener(new HelloWorldFlowEventListener(lock, cond));
		session.runFlow();
		ConcurrentUtil.wait(lock, cond, 30 * 1000);
		System.out.println(JsonUtil.encode(session.getFlowInfo()));
		Assert.assertEquals(1, session.getSessionContext().getTaskCount());
		Assert.assertEquals(2, session.getSessionContext().getSplitCount());
		Assert.assertEquals(2, session.getSessionContext().getRetryCount());
		Assert.assertEquals(1, session.getSessionContext().getTimeoutCount());
		Assert.assertEquals(10, session.getSessionContext().getBatchCount());
		Assert.assertEquals(0, session.getSessionContext().getErrorStepCount());
		
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
		Assert.assertEquals("testFailureStep", session.getCurrentAction());
		Assert.assertEquals(FlowResult.Failed, session.getResult());
		Assert.assertEquals(FlowState.Completed, session.getState());
		System.out.println(new ObjectMapper().writeValueAsString(session.getSessionContext().getLastErrors()));
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
		Assert.assertEquals("testFailureStep", session.getCurrentAction());
		Assert.assertEquals(FlowResult.Failed, session.getResult());
		Assert.assertEquals(FlowState.Completed, session.getState());
		System.out.println(new ObjectMapper().writeValueAsString(session.getSessionContext().getLastErrors()));
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
		Assert.assertEquals("testFailureStep", session.getCurrentAction());
		Assert.assertEquals(1, session.getSessionContext().getErrorStepCount());
		Assert.assertEquals(FlowResult.Failed, session.getResult());
		Assert.assertEquals(FlowState.Paused, session.getState());
		
		// reset current step to the desirable step and resume flow
		HelloWorldFlow session1 = (HelloWorldFlow) FlowSessionFactory.getInstance().findByKey(session.getKey());
		session1.setCurrentAction("testFailureStep");
		// don't pause this time
		session1.getSessionContext().setPauseOnError(false);
		session1.addEventListener(new HelloWorldFlowEventListener(lock, cond));
		session1.runFlow();
		ConcurrentUtil.wait(lock, cond);
		
		System.out.println(new ObjectMapper().writeValueAsString(session1.getFlowInfo()));

		// second time we did not set private error flag in context, so flow did not go to the error step
		Assert.assertEquals(1, session1.getSessionContext().getErrorStepCount());
		Assert.assertEquals("stop", session1.getCurrentAction());
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
				new FlowModule().setDb(LocalDatabaseEnum.TESTMEMDB)
								.enableCluster()
								.setSpringContext(flowCtx)
								.setExectuorService(Executors.newFixedThreadPool(5))
								.getModule(),
		};
	}
}
