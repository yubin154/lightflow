package org.lightj.example.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lightj.example.session.HelloWorldFlow.steps;
import org.lightj.session.FlowExecutionException;
import org.lightj.session.FlowSession;
import org.lightj.session.step.DelayedEnclosure;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.RetryEnclosure;
import org.lightj.session.step.SimpleStepExecution;
import org.lightj.session.step.StepBuilder;
import org.lightj.session.step.StepCallbackHandler;
import org.lightj.session.step.StepExecution;
import org.lightj.session.step.StepTransition;
import org.lightj.task.AsyncPollMonitor;
import org.lightj.task.BatchOption;
import org.lightj.task.ExecutableTask;
import org.lightj.task.ExecuteOption;
import org.lightj.task.FlowTask;
import org.lightj.task.MonitorOption;
import org.lightj.task.Task;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;
import org.lightj.task.asynchttp.AsyncHttpTask;
import org.lightj.util.StringUtil;

import akka.actor.ActorRef;

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
@SuppressWarnings({"rawtypes", "unchecked"})
public class HelloWorldFlowStepsImpl {
	
	/**
	 * running a simple asynchronous task
	 * @return
	 */
	public static IFlowStep buildAsyncTaskStep() {
		
		// create the task
		ExecutableTask task = new DummyTask() {
			public TaskResult execute(ActorRef executingActor) {
				context.incTaskCount();
				return super.execute(executingActor);
			}			
		};
		
		return new StepBuilder().executeAsyncTasks(task)
								.onResult(steps.sessionJoinStep, steps.error)
								.getFlowStep();
		
	}
	
	/**
	 * run 2 sub workflow and join from parent flow
	 * @return
	 */
	public static IFlowStep buildJoinStep() 
	{
		// build child flows
		List<FlowSession> children = new ArrayList<FlowSession>();
		for (int i = 0; i < 2; i++) {
			FlowSession child = new HelloWorldFlow();
			children.add(child);
		}

		// result handler, the handler increment the counter on each child flow completion
		StepCallbackHandler<HelloWorldFlowContext> resultHandler = new StepCallbackHandler<HelloWorldFlowContext>(StepTransition.runToStep(steps.delayStep)) {

			@Override
			public synchronized StepTransition executeOnResult(Task task, TaskResult result) throws FlowExecutionException {
				// remember result
				if (task instanceof FlowTask) {
					sessionContext.incSplitCount();
				}
				return super.executeOnResult(task, result);
			}
			
		}.mapResult(steps.delayStep, steps.error);

		// build the step
		return new StepBuilder().launchSubFlows(children, resultHandler).getFlowStep();
	}

	/**
	 * build a step with initial delay
	 * @return
	 */
	public static IFlowStep buildDelayStep() 
	{
		IFlowStep step = new StepBuilder().runTo(steps.retryStep).getFlowStep();
		return DelayedEnclosure.delay(3000, step);		
	}

	/**
	 * build a retry step
	 * @return
	 */
	public static IFlowStep buildRetryStep() 
	{
		// increment count every time this step is executed
		StepExecution execution = new SimpleStepExecution<HelloWorldFlowContext>(steps.timeoutStep) {
			@Override
			public StepTransition execute() throws FlowExecutionException {
				this.sessionContext.incRetryCount();
				return super.execute();
			}
		};
		
		IFlowStep step = new StepBuilder().execute(execution).getFlowStep();
		
		// retry until match a transition or max retry limit is reached
		StepTransition matchTran = StepTransition.runToStep(steps.timeoutStep);
		return RetryEnclosure.retryIf(DelayedEnclosure.delay(1000, step), 3, matchTran);		
	}
	
	/**
	 * build a step with timeout option
	 * @return
	 */
	public static IFlowStep buildTimeoutStep() 
	{
		// task
		DummyTask task = new DummyTask(new ExecuteOption(0, 100));
		
		// result handler, increment counter if task result if timeout
		final StepCallbackHandler resultHandler = new StepCallbackHandler<HelloWorldFlowContext>(StepTransition.runToStep(steps.error)) {
			
			@Override
			public synchronized StepTransition executeOnResult(Task atask, TaskResult result) {
				if (TaskResultEnum.Timeout == result.getStatus()) {
					sessionContext.incTimeoutCount();
				}
				return super.executeOnResult(atask, result);
			}
			
		}.mapResultTo(steps.actorStep, TaskResultEnum.Timeout, TaskResultEnum.Success);

		return new StepBuilder().executeAsyncTasks(task).onResult(resultHandler).getFlowStep();

	}
	
	/**
	 * actor step with async task
	 * @return
	 */
	public static IFlowStep buildActorStep()
	{
		// create async http client, should be shared
		AsyncHttpClientConfigBean config = new AsyncHttpClientConfigBean();
		config.setConnectionTimeOutInMs(3000);
		AsyncHttpClient client = new AsyncHttpClient(config);
		
		// build async http task
		final AsyncHttpTask<HelloWorldFlowContext> task = new AsyncHttpTask<HelloWorldFlowContext>(client){

			@Override
			public BoundRequestBuilder createRequest() {
				return client.prepareGet(context.getBadHost());
			}

			@Override
			public TaskResult onComplete(Response response) {
				TaskResult res = createTaskResult(TaskResultEnum.Success, "");
				try {
					res.setRealResult(response.getResponseBody());
				} catch (IOException e) {
					return createErrorResult(TaskResultEnum.Failed, e.getMessage(), StringUtil.getStackTrace(e, 2000));
				}
				return res;
			}

			@Override
			public TaskResult onThrowable(Throwable t) {
				return this.createErrorResult(TaskResultEnum.Failed, t.getMessage(), StringUtil.getStackTrace(t));
			}
		};
		
		// result handler, with default transition
		StepCallbackHandler resultHandler = new StepCallbackHandler(steps.actorPollStep).mapResultTo(steps.actorPollStep, TaskResultEnum.values());

		return new StepBuilder().executeAsyncTasks(task).onResult(resultHandler).getFlowStep();
	}
	
	/**
	 * actor step with polling
	 * @return
	 */
	public static IFlowStep buildActorPollStep()
	{
		// create and configure async http client
		AsyncHttpClientConfigBean config = new AsyncHttpClientConfigBean();
		config.setConnectionTimeOutInMs(3000);
		AsyncHttpClient client = new AsyncHttpClient(config);

		// build http task
		final AsyncHttpTask<HelloWorldFlowContext> task = new AsyncHttpTask<HelloWorldFlowContext>(client) {

			@Override
			public BoundRequestBuilder createRequest() {
				return client.prepareGet(context.getGoodHost());
			}

			@Override
			public TaskResult onComplete(Response response) {
				TaskResult res = createTaskResult(TaskResultEnum.Success, "");
				try {
					res.setRealResult(response.getResponseBody());
				} catch (IOException e) {
					return createErrorResult(TaskResultEnum.Failed, e.getMessage(), StringUtil.getStackTrace(e, 2000));
				}
				return res;
			}

			@Override
			public TaskResult onThrowable(Throwable t) {
				return this.createErrorResult(TaskResultEnum.Failed, t.getMessage(), StringUtil.getStackTrace(t));
			}
		};
			
		// poll every second, upto 5 seconds
		final MonitorOption monitorOption = new MonitorOption(1000, 5000);
		
		// poll monitor impl
		final AsyncPollMonitor monitor = new AsyncPollMonitor(monitorOption) 
		{
			@Override
			public TaskResult processPollResult(TaskResult result) {
				return result;
			}

			@Override
			public ExecutableTask createPollTask(ExecutableTask task, TaskResult reqResult) {
				return task;
			}
			
		};
		
		return new StepBuilder().executeAsyncPollTasks(monitor, task).onResult(steps.actorBatchStep, steps.error).getFlowStep();
	}


	/**
	 * execute batch of tasks
	 * @return
	 */
	public static IFlowStep buildActorBatchStep() {
		
		// build tasks
		List<ExecutableTask> tasks = new ArrayList<ExecutableTask>();
		for (int i = 0; i < 10; i++) {
			tasks.add(new DummyTask() {
				public TaskResult execute(ActorRef executingActor) {
					context.incBatchCount();
					return super.execute(executingActor);
				}				
			});
		}
		
		// build execution with batching option of max concurrency of 5 tasks
		return new StepBuilder().executeActors(null, StepBuilder.createAsyncActorFactory(), new BatchOption(5), tasks.toArray(new ExecutableTask[0]))
								.onResult(steps.testFailureStep, steps.error)
								.getFlowStep();
	}


	/**
	 * task with an injected failure
	 * @return
	 */
	public static IFlowStep buildTestFailureStep() {
		
		// task
		ExecutableTask task = new DummyTask() {
			
			public TaskResult execute(ActorRef executingActor) {
				if (context.isInjectFailure()) {
					if (context.isControlledFailure()) {
						return this.createErrorResult(TaskResultEnum.Failed, "unit test injected failure", null);
					}
					else {
						throw new RuntimeException("unit test injected runtime failure");
					}
				}
				return super.execute(executingActor);
			}
			
		};
		
		return new StepBuilder().executeAsyncTasks(task)
								.onResult(steps.stop, steps.error)
								.getFlowStep();
		
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
		public TaskResult execute(ActorRef executingActor) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return this.createTaskResult(TaskResultEnum.Failed, e.getMessage());
			}
			return result==null ? this.createTaskResult(TaskResultEnum.Success, null) : result;
		}

		@Override
		public String getTaskDetail() {
			return "dummy exeutable task";
		}

	}
}
