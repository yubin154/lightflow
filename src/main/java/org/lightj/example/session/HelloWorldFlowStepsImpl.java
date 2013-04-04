package org.lightj.example.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lightj.dal.SimpleLocatable;
import org.lightj.example.session.HelloWorldFlow.steps;
import org.lightj.session.FlowExecutionException;
import org.lightj.session.FlowSession;
import org.lightj.session.FlowSessionFactory;
import org.lightj.session.FlowState;
import org.lightj.session.step.DelayedEnclosure;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.MappedCallbackHandler;
import org.lightj.session.step.RetryEnclosure;
import org.lightj.session.step.SimpleStepExecution;
import org.lightj.session.step.StepBuilder;
import org.lightj.session.step.StepErrorHandler;
import org.lightj.session.step.StepExecution;
import org.lightj.session.step.StepTransition;
import org.lightj.task.AsyncPollTaskWorker;
import org.lightj.task.AsyncTaskWorker;
import org.lightj.task.BatchTask;
import org.lightj.task.ExecutableTask;
import org.lightj.task.FlowTask;
import org.lightj.task.MonitorOption;
import org.lightj.task.Task;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;
import org.lightj.task.asynchttp.HttpTask;
import org.lightj.task.asynchttp.HttpWorker;
import org.lightj.util.ActorUtil;
import org.lightj.util.StringUtil;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.UntypedActorFactory;

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
@SuppressWarnings("rawtypes")
public class HelloWorldFlowStepsImpl {
	
	/**
	 * a simple async task
	 * @return
	 */
	public static IFlowStep buildAsyncTaskStep() {
		
		ExecutableTask task = new DummyTask() {
			public TaskResult execute(ActorRef executingActor) {
				context.incTaskCount();
				return super.execute(executingActor);
			}			
		};
		
		// result handler, with default transition
		final MappedCallbackHandler resultHandler = MappedCallbackHandler.onResult(steps.sessionJoinStep, steps.error);

		return new StepBuilder().executeAsyncTasks(resultHandler, task).getFlowStep();
		
	}
	
	/**
	 * build join step
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static IFlowStep buildJoinStep() 
	{
		List<FlowSession> children = new ArrayList<FlowSession>();
		for (int i = 0; i < 2; i++) {
			FlowSession child = FlowSessionFactory.getInstance().createSession(
					HelloWorldFlow.class, 
					new SimpleLocatable(),
					new SimpleLocatable());
			children.add(child);
		}

		// result handler, with default transition
		MappedCallbackHandler<HelloWorldFlowContext> resultHandler = new MappedCallbackHandler<HelloWorldFlowContext>(StepTransition.runToStep(steps.delayStep)) {

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
	 * build a step with delay
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
		StepExecution execution = new SimpleStepExecution<HelloWorldFlowContext>(steps.timeoutStep) {
			@Override
			public StepTransition execute() throws FlowExecutionException {
				this.sessionContext.incRetryCount();
				return super.execute();
			}
		};
		IFlowStep step = new StepBuilder().execute(execution).getFlowStep();
		StepTransition matchTran = StepTransition.runToStep(steps.timeoutStep);
		return RetryEnclosure.retryIf(DelayedEnclosure.delay(1000, step), 3, matchTran);		
	}
	
	/**
	 * build a step with timeout option
	 * @return
	 */
	public static IFlowStep buildTimeoutStep() 
	{
		StepExecution execution = new SimpleStepExecution<HelloWorldFlowContext>(new StepTransition().inState(FlowState.Callback));

		StepErrorHandler<HelloWorldFlowContext> timeoutHandler = new StepErrorHandler<HelloWorldFlowContext>(steps.actorStep) {
			
			@Override
			public StepTransition executeOnError(Throwable t) {
				assert t instanceof FlowExecutionException;
				this.sessionContext.setTimeoutCount(this.sessionContext.getTimeoutCount()+1);
				return defResult;
			}
		};
		
		
		IFlowStep step = new StepBuilder().execute(execution).onException(timeoutHandler).getFlowStep();
		step.setTimeoutMilliSec(2000);
		return step;
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
		
		// build http task
		final HttpTask<HelloWorldFlowContext> task = new HttpTask<HelloWorldFlowContext>(client) {

			@Override
			public TaskResult processRequestResult(Response response) {
				TaskResult res = this.createTaskResult(TaskResultEnum.Success, "");
				try {
					res.setRealResult(response.getResponseBody());
				} catch (IOException e) {
					return this.createErrorResult(TaskResultEnum.Failed, e.getMessage(), StringUtil.getStackTrace(e, 2000));
				}
				return res;
			}

			@Override
			public BoundRequestBuilder createRequest() {
				return client.prepareGet(context.getBadHost());
			}

		};
		
		// result handler, with default transition
		final MappedCallbackHandler resultHandler = MappedCallbackHandler.onResult(steps.error, steps.actorPollStep);

		// build the step
		ActorRef actor = ActorUtil.createActor(new UntypedActorFactory() {
			private static final long serialVersionUID = 1L;

			@Override
			public Actor create() throws Exception {
				return new AsyncTaskWorker<HttpTask>(task) {

					@Override
					public TaskResult processRequestResult(HttpTask task, TaskResult result) {
						return (TaskResult) result;
					}

					@Override
					public Actor createRequestWorker(HttpTask task) {
						return new HttpWorker(task);
					}

				};
			}

		});

		return new StepBuilder().executeAsyncActors(resultHandler, new BatchTask(task), actor).getFlowStep();
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
		final HttpTask<HelloWorldFlowContext> task = new HttpTask<HelloWorldFlowContext>(client) {

			@Override
			public TaskResult processRequestResult(Response response) {
				TaskResult res = this.createTaskResult(TaskResultEnum.Success, "");
				try {
					res.setRealResult(response.getResponseBody());
				} catch (IOException e) {
					return this.createErrorResult(TaskResultEnum.Failed, e.getMessage(), StringUtil.getStackTrace(e, 2000));
				}
				return res;
			}

			@Override
			public BoundRequestBuilder createRequest() {
				return client.prepareGet(context.getGoodHost());
			}
			
		};
		final MonitorOption monitorOption = new MonitorOption(1000);
		
		// result handler, with default transition
		final MappedCallbackHandler resultHandler = MappedCallbackHandler.onResult(steps.actorBatchStep, steps.error);
		
		// build the step
		ActorRef actor = ActorUtil.createActor(new UntypedActorFactory() {
			private static final long serialVersionUID = 1L;

			@Override
			public Actor create() throws Exception {
				return new AsyncPollTaskWorker<HttpTask>(task, monitorOption) {

					@Override
					public Actor createPollWorker(HttpTask task, TaskResult result) {
						return new HttpWorker(task);
					}

					@Override
					public TaskResult processPollResult(TaskResult result) {
						return (TaskResult) result;
					}

					@Override
					public Actor createRequestWorker(HttpTask task) {
						return new HttpWorker(task);
					}

				};
			}

		});

		return new StepBuilder().executeAsyncActors(resultHandler, new BatchTask(task), actor).getFlowStep();
	}


	public static IFlowStep buildActorBatchStep() {
		
		List<ExecutableTask> tasks = new ArrayList<ExecutableTask>();
		for (int i = 0; i < 10; i++) {
			tasks.add(new DummyTask() {
				public TaskResult execute(ActorRef executingActor) {
					context.incBatchCount();
					return super.execute(executingActor);
				}				
			});
		}
		
		// result handler, with default transition
		final MappedCallbackHandler resultHandler = MappedCallbackHandler.onResult(steps.stop, steps.error);

		return new StepBuilder().executeAsyncTasks(resultHandler, tasks.toArray(new ExecutableTask[0])).getFlowStep();
	}


	/**
	 * dummy task
	 * @author biyu
	 *
	 */
	static class DummyTask extends ExecutableTask<HelloWorldFlowContext> {
		
		@Override
		public TaskResult execute(ActorRef executingActor) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				return this.createTaskResult(TaskResultEnum.Failed, e.getMessage());
			}
			return this.createTaskResult(TaskResultEnum.Success, null);
		}

		@Override
		public String getTaskDetail() {
			return "dummy exeutable task";
		}

	}
}
