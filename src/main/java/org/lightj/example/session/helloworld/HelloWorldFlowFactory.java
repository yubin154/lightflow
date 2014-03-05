package org.lightj.example.session.helloworld;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lightj.session.FlowSession;
import org.lightj.session.FlowSessionFactory;
import org.lightj.session.exception.FlowExecutionException;
import org.lightj.session.step.DelayedEnclosure;
import org.lightj.session.step.IAroundExecution;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.RetryEnclosure;
import org.lightj.session.step.SimpleStepExecution;
import org.lightj.session.step.StepBuilder;
import org.lightj.session.step.StepCallbackHandler;
import org.lightj.session.step.StepExecution;
import org.lightj.session.step.StepTransition;
import org.lightj.task.BatchOption;
import org.lightj.task.BatchOption.Strategy;
import org.lightj.task.ExecutableTask;
import org.lightj.task.ExecuteOption;
import org.lightj.task.FlowTask;
import org.lightj.task.ITaskEventHandler;
import org.lightj.task.MonitorOption;
import org.lightj.task.Task;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;
import org.lightj.task.asynchttp.IPollProcessor;
import org.lightj.task.asynchttp.SimpleHttpAsyncPollTask;
import org.lightj.task.asynchttp.UrlRequest;
import org.lightj.task.asynchttp.UrlTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.ning.http.client.AsyncHttpClient;
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
		
		return new StepBuilder().executeTasks(task).getFlowStep();
		
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
		StepCallbackHandler<HelloWorldFlowContext> resultHandler = new StepCallbackHandler<HelloWorldFlowContext>();
		resultHandler.setDelegateHandler(new ITaskEventHandler<HelloWorldFlowContext>() {

			@Override
			public void executeOnCreated(HelloWorldFlowContext ctx, Task task) {
			}

			@Override
			public void executeOnSubmitted(HelloWorldFlowContext ctx, Task task) {
			}

			@Override
			public void executeOnResult(HelloWorldFlowContext ctx, Task task,
					TaskResult result) {
				if (task instanceof FlowTask) {
					ctx.incSplitCount();
				}
			}

			@Override
			public StepTransition executeOnCompleted(HelloWorldFlowContext ctx,
					Map<String, TaskResult> results) {
				return null;
			}
			
		});
		// build the step
		return new StepBuilder().executeTasks(tasks.toArray(new FlowTask[0])).onResult(resultHandler).getFlowStep();
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
		StepTransition matchTran = StepTransition.runToStep("timeoutStep");
		return RetryEnclosure.retryIf(DelayedEnclosure.delay(1000, step), 1, matchTran);		
	}
	
	/**
	 * build a step with timeout option
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public @Bean @Scope("prototype") IFlowStep helloWorldTimeoutStep() 
	{
		// task
		DummyTask task = new DummyTask(new ExecuteOption(0, 100));
		
		// result handler, increment counter if task result if timeout
		StepCallbackHandler resultHandler = new StepCallbackHandler<HelloWorldFlowContext>();
		resultHandler.mapResultTo("asyncPollStep", TaskResultEnum.Timeout, TaskResultEnum.Success);
		resultHandler.setDelegateHandler(new ITaskEventHandler<HelloWorldFlowContext>() {

			@Override
			public void executeOnCreated(HelloWorldFlowContext ctx, Task task) {
			}

			@Override
			public void executeOnSubmitted(HelloWorldFlowContext ctx, Task task) {
			}

			@Override
			public void executeOnResult(HelloWorldFlowContext ctx, Task task,
					TaskResult result) {
				if (TaskResultEnum.Timeout == result.getStatus()) {
					ctx.incTimeoutCount();
				}
			}

			@Override
			public StepTransition executeOnCompleted(HelloWorldFlowContext ctx,
					Map<String, TaskResult> results) {
				return null;
			}

		});
			
		return new StepBuilder().executeTasks(task).onResult(resultHandler).getFlowStep();

	}
	
	/**
	 * actor step with polling
	 * @return
	 */
	public @Bean @Scope("prototype") IFlowStep helloWorldAsyncPollStep()
	{
		return new StepBuilder().executeTasksFromScrapbook("asyncPollTasks", true, null, new IAroundExecution<HelloWorldFlowContext>() {

			@Override
			public void preExecute(HelloWorldFlowContext ctx)
					throws FlowExecutionException {
				// create and configure async http client
				AsyncHttpClientConfigBean config = new AsyncHttpClientConfigBean();
				config.setConnectionTimeOutInMs(3000);
				final AsyncHttpClient client = new AsyncHttpClient(config);

				// poll every second, up to 5 seconds
				final MonitorOption monitorOption = new MonitorOption(1000, 5000);
				final UrlTemplate template = new UrlTemplate("https://#host");
				
				ArrayList<SimpleHttpAsyncPollTask> tasks = new ArrayList<SimpleHttpAsyncPollTask>();
				for (String host : ctx.getGoodHosts()) {
					SimpleHttpAsyncPollTask task = new SimpleHttpAsyncPollTask(client, new ExecuteOption(), monitorOption, 
							new IPollProcessor() {

						@Override
						public TaskResultEnum checkPollProgress(Response response) {
							return TaskResultEnum.Success;
						}

						@Override
						public TaskResultEnum preparePollTask(Response response,
								UrlRequest pollReq) {
							return TaskResultEnum.Success;
						}						
					});
					task.setHttpParams(new UrlRequest(template).addTemplateValue("#host", host), new UrlRequest(template), "#host");
					tasks.add(task);
				}
				ctx.addToScrapbook("asyncPollTasks", tasks);
			}

			@Override
			public void postExecute(HelloWorldFlowContext ctx)
					throws FlowExecutionException {
			}

		}).getFlowStep();
	}


	/**
	 * execute batch of tasks
	 * @return
	 */
	public @Bean @Scope("prototype") IFlowStep helloWorldActorBatchStep() {
		
		// build tasks
		List<ExecutableTask> tasks = new ArrayList<ExecutableTask>();
		for (int i = 0; i < 10; i++) {
			tasks.add(new DummyTask() {
				public TaskResult execute() {
					context.incBatchCount();
					return super.execute();
				}				
			});
		}
		
		// build execution with batching option of max concurrency of 5 tasks
		return new StepBuilder().executeTasks(
				new BatchOption(10, Strategy.MAX_CONCURRENT_RATE_SLIDING),
				null,
				tasks.toArray(new ExecutableTask[0])).getFlowStep();
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
		
		return new StepBuilder().executeTasks(task).getFlowStep();
		
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
