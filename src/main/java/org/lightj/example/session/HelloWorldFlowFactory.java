package org.lightj.example.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lightj.example.session.HelloWorldFlow.steps;
import org.lightj.session.FlowSession;
import org.lightj.session.FlowSessionFactory;
import org.lightj.session.exception.FlowExecutionException;
import org.lightj.session.step.DelayedEnclosure;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.RetryEnclosure;
import org.lightj.session.step.SimpleStepExecution;
import org.lightj.session.step.StepBuilder;
import org.lightj.session.step.StepCallbackHandler;
import org.lightj.session.step.StepExecution;
import org.lightj.session.step.StepTransition;
import org.lightj.task.BatchOption;
import org.lightj.task.ExecutableTask;
import org.lightj.task.ExecuteOption;
import org.lightj.task.FlowTask;
import org.lightj.task.MonitorOption;
import org.lightj.task.Task;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;
import org.lightj.task.asynchttp.AsyncHttpTask;
import org.lightj.util.JsonUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfigBean;
import com.ning.http.client.Response;

/**
 * a spring factory creates steps in a workflow
 * 
 * @author binyu
 *
 */
@SuppressWarnings({"rawtypes"})
@Configuration
public class HelloWorldFlowFactory {
	
	public @Bean @Scope("prototype") HelloWorldFlow helloWorldFlow() {
		return new HelloWorldFlow();
	}
	
	/**
	 * running a simple asynchronous task
	 * @return
	 */
	public @Bean @Scope("prototype") IFlowStep helloWorldAsyncTaskStep() {
		
		// create the task
		ExecutableTask task = new DummyTask() {
			public TaskResult execute() {
				context.incTaskCount();
				return super.execute();
			}			
		};
		
		return new StepBuilder().executeAsyncTasks(task).getFlowStep();
		
	}
	
	/**
	 * run 2 sub workflow and join from parent flow
	 * @return
	 */
	public @Bean @Scope("prototype") IFlowStep helloWorldSessionJoinStep() 
	{
		// build child flows
		List<FlowTask> tasks = new ArrayList<FlowTask>();
		for (int i = 0; i < 2; i++) {

			tasks.add(new FlowTask() {
				@Override
				public FlowSession createSubFlow() {
					return FlowSessionFactory.getInstance().createSession(HelloWorldFlow.class);
				}
			});

		}

		// result handler, the handler increment the counter on each child flow completion
		StepCallbackHandler<HelloWorldFlowContext> resultHandler = new StepCallbackHandler<HelloWorldFlowContext>() {

			@Override
			protected void handleResult(Task task, TaskResult result) throws FlowExecutionException {
				// remember result
				if (task instanceof FlowTask) {
					sessionContext.incSplitCount();
				}
			}
			
		};

		// build the step
		return new StepBuilder().executeAsyncTasks(tasks.toArray(new FlowTask[0])).onResult(resultHandler).getFlowStep();
	}

	/**
	 * build a step with initial delay
	 * @return
	 */
	public @Bean @Scope("prototype") IFlowStep helloWorldDelayStep() 
	{
		IFlowStep step = new StepBuilder().getFlowStep();
		return DelayedEnclosure.delay(3000, step);		
	}

	/**
	 * build a retry step
	 * @return
	 */
	public @Bean @Scope("prototype") IFlowStep helloWorldRetryStep() 
	{
		// increment count every time this step is executed
		StepExecution execution = new SimpleStepExecution<HelloWorldFlowContext>() {
			@Override
			public StepTransition execute() throws FlowExecutionException {
				this.sessionContext.incRetryCount();
				return super.execute();
			}
		};
		
		IFlowStep step = new StepBuilder().execute(execution).getFlowStep();
		
		// retry until match a transition or max retry limit is reached
		StepTransition matchTran = StepTransition.runToStep(steps.timeoutStep);
		return RetryEnclosure.retryIf(DelayedEnclosure.delay(1000, step), 1, matchTran);		
	}
	
	/**
	 * build a step with timeout option
	 * @return
	 */
	public @Bean @Scope("prototype") IFlowStep helloWorldTimeoutStep() 
	{
		// task
		DummyTask task = new DummyTask(new ExecuteOption(0, 100));
		
		// result handler, increment counter if task result if timeout
		final StepCallbackHandler resultHandler = new StepCallbackHandler<HelloWorldFlowContext>() {
			
			@Override
			protected void handleResult(Task atask, TaskResult result) {
				if (TaskResultEnum.Timeout == result.getStatus()) {
					sessionContext.incTimeoutCount();
				}
			}
			
		}.mapResultTo(steps.asyncPollStep, TaskResultEnum.Timeout, TaskResultEnum.Success);

		return new StepBuilder().executeAsyncTasks(task).onResult(resultHandler).getFlowStep();

	}
	
	/**
	 * actor step with polling
	 * @return
	 */
	public @Bean @Scope("prototype") IFlowStep helloWorldAsyncPollStep()
	{
		// create and configure async http client
		AsyncHttpClientConfigBean config = new AsyncHttpClientConfigBean();
		config.setConnectionTimeOutInMs(3000);
		final AsyncHttpClient client = new AsyncHttpClient(config);

		// poll every second, up to 5 seconds
		final MonitorOption monitorOption = new MonitorOption(1000, 5000);
		
		// build http task
		final AsyncHttpTask<HelloWorldFlowContext> task = new AsyncHttpTask<HelloWorldFlowContext>(client, new ExecuteOption(), monitorOption) {

			@Override
			public BoundRequestBuilder createRequest() {
				return client.preparePost("https://" + context.getGoodHost() + "/admin/executeCmd")
						.addHeader("Authorization", "Basic YWdlbnQ6dG95YWdlbnQ=")
						.addHeader("content-type", "application/json")
						.addHeader("AUTHZ_TOKEN", "donoevil")
						.setBody("{\"cmd\": \"netstat\"}");
			}

			@Override
			public TaskResult onComplete(Response response) {
				TaskResult res = createTaskResult(TaskResultEnum.Success, "");
				try {
					res.setRealResult(response.getResponseBody());
					AgentStatus aStatus = JsonUtil.decode((String) res.getRealResult(), AgentStatus.class);
					final String pollUrl = "https://" + context.getGoodHost() + aStatus.getStatus();
					this.setExtTaskUuid(pollUrl);
					AsyncHttpTask pollTask = createPollTask(pollUrl);
					res.setRealResult(pollTask);
				} catch (IOException e) {
					return createErrorResult(TaskResultEnum.Failed, e.getMessage(), e);
				}
				return res;
			}

			@Override
			public TaskResult onThrowable(Throwable t) {
				return this.createErrorResult(TaskResultEnum.Failed, t.getMessage(), t);
			}
			
			/** create poll task */
			private AsyncHttpTask createPollTask(final String pollUrl) {
				
				return new AsyncHttpTask<HelloWorldFlowContext>(client) {

					@Override
					public BoundRequestBuilder createRequest() {
						return client.prepareGet(pollUrl);
					}

					@Override
					public TaskResult onComplete(Response response) {
						TaskResult res = null;
						try {
							AgentStatus aStatus = JsonUtil.decode((String) response.getResponseBody(), AgentStatus.class);
							if (aStatus.getProgress() == 100) {
								if (aStatus.getError() == 0) {
									res = createTaskResult(TaskResultEnum.Success, aStatus.getStatus());
								}
								else {
									res = createTaskResult(TaskResultEnum.Failed, aStatus.getErrorMsg());
								}
								res.setRealResult(aStatus);
							}
							else {
								res = createTaskResult(TaskResultEnum.Running, null);
								res.setRealResult(this);
							}
							
						} catch (IOException e) {
							return createErrorResult(TaskResultEnum.Failed, e.getMessage(), e);
						}
						return res;
					}

					@Override
					public TaskResult onThrowable(Throwable t) {
						return this.createErrorResult(TaskResultEnum.Failed, t.getMessage(), t);
					}
				};

			}
		};
			
		return new StepBuilder().executeAsyncPollTasks(task).getFlowStep();
	}


	/**
	 * execute batch of tasks
	 * @return
	 */
	public @Bean @Scope("prototype") IFlowStep helloWorldActorBatchStep() {
		
		// build tasks
		List<ExecutableTask> tasks = new ArrayList<ExecutableTask>();
		for (int i = 0; i < 2; i++) {
			tasks.add(new DummyTask() {
				public TaskResult execute() {
					context.incBatchCount();
					return super.execute();
				}				
			});
		}
		
		// build execution with batching option of max concurrency of 5 tasks
		return new StepBuilder().batchExecuteAsyncTasks(new BatchOption(1), tasks.toArray(new ExecutableTask[0])).getFlowStep();
	}


	/**
	 * task with an injected failure
	 * @return
	 */
	public @Bean @Scope("prototype") IFlowStep helloWorldTestFailureStep() {
		
		// task
		ExecutableTask task = new DummyTask() {
			
			public TaskResult execute() {
				if (context.isInjectFailure()) {
					if (context.isControlledFailure()) {
						return this.createErrorResult(TaskResultEnum.Failed, "unit test injected failure", new Exception("unit test injected failure"));
					}
					else {
						throw new RuntimeException("unit test injected runtime failure");
					}
				}
				return super.execute();
			}
			
		};
		
		return new StepBuilder().executeAsyncTasks(task).getFlowStep();
		
	}

	/**
	 * dummy task
	 * @author biyu
	 *
	 */
	static class DummyTask extends ExecutableTask<HelloWorldFlowContext> {
		
		TaskResult result;
		DummyTask() {super();}
		DummyTask(ExecuteOption option) { super(option); }
		
		@Override
		public TaskResult execute() {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return this.createTaskResult(TaskResultEnum.Failed, e.getMessage());
			}
			return result==null ? this.createTaskResult(TaskResultEnum.Success, null) : result;
		}

		public String toString() {
			return "dummy exeutable task";
		}

	}
}
