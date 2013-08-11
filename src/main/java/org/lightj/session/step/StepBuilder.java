package org.lightj.session.step;

import static akka.pattern.Patterns.ask;

import java.util.concurrent.TimeUnit;

import org.lightj.session.FlowContext;
import org.lightj.session.FlowExecutionException;
import org.lightj.session.FlowResult;
import org.lightj.task.AsyncPollTaskWorker;
import org.lightj.task.AsyncTaskWorker;
import org.lightj.task.BatchOption;
import org.lightj.task.BatchTask;
import org.lightj.task.BatchTaskWorker;
import org.lightj.task.ExecutableTask;
import org.lightj.task.IWorker;
import org.lightj.task.Task;
import org.lightj.task.TaskResultEnum;
import org.lightj.util.ActorUtil;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.UntypedActorFactory;
import akka.util.Timeout;

@SuppressWarnings({"rawtypes", "unchecked"})
public class StepBuilder {

	/**
	 * flow step concrete impl
	 */
	private StepImpl flowStep = new StepImpl();
	
	/**
	 * get flow step impl
	 * @return
	 */
	public StepImpl getFlowStep() {
		return flowStep;
	}

	/**
	 * execute
	 * @param execution
	 * @return
	 */
	public StepBuilder execute(StepExecution execution) {
		if (execution != null) {
			flowStep.setExecution(execution);
		}
		return this;
	}
	
	/**
	 * run to to step
	 * @param step
	 * @return
	 */
	public StepBuilder runTo(String step) {
		flowStep.setExecution(new SimpleStepExecution(StepTransition.runToStep(step)));
		return this;
	}

	/**
	 * run to step
	 * @param step
	 * @return
	 */
	public StepBuilder runTo(Enum step) {
		flowStep.setExecution(new SimpleStepExecution(StepTransition.runToStep(step)));
		return this;
	}
	
	/**
	 * 
	 * @param trans
	 * @return
	 */
	public StepBuilder parkInState(StepTransition trans) {
		flowStep.setExecution(new SimpleStepExecution(trans));
		return this;
	}
	
	/**
	 * handle result
	 * @param callbackHandler
	 * @return
	 */
	public StepBuilder onResult(StepCallbackHandler resultHandler) {
		flowStep.setResultHandler(resultHandler);
		return this;
	}

	/**
	 * result handler, go to step when for success/fail result
	 * @param stepOnSuccess
	 * @param stepOnElse
	 * @return
	 */
	public StepBuilder onResult(String stepOnSuccess, String stepOnElse) {
		StepTransition onElse = StepTransition.runToStep(stepOnElse).withResult(FlowResult.Failed);
		StepCallbackHandler handler = new StepCallbackHandler();
		handler.mapResultTo(onElse, TaskResultEnum.values());
		handler.mapResultTo(StepTransition.runToStep(stepOnSuccess), TaskResultEnum.Success);
		flowStep.setOrUpdateResultHandler(handler);
		return this;
	}
	
	/**
	 * result handler, go to step when success, all other will go to error step
	 * @param stepOnSuccess
	 * @return
	 */
	public StepBuilder onSuccess(String stepOnSuccess) {
		StepCallbackHandler handler = new StepCallbackHandler(stepOnSuccess);
		flowStep.setOrUpdateResultHandler(handler);
		return this;
	}
	
	/**
	 * result handler, go to step when success, all other will go to error step
	 * @param stepOnSuccess
	 * @return
	 */
	public StepBuilder onSuccess(Enum stepOnSuccess) {
		StepCallbackHandler handler = new StepCallbackHandler(stepOnSuccess);
		flowStep.setOrUpdateResultHandler(handler);
		return this;
	}

	/**
	 * result handler, go to step when for success/fail result
	 * @param stepOnSuccess
	 * @param stepOnElse
	 * @return
	 */
	public StepBuilder onResult(Enum stepOnSuccess, Enum stepOnElse) {
		return onResult(stepOnSuccess.name(), stepOnElse.name());
	}
	
	/**
	 * exception handler
	 * @param errorHandler
	 * @return
	 */
	public StepBuilder onException(StepErrorHandler errorHandler) {
		if (errorHandler != null) {
			flowStep.setErrorHandler(errorHandler);
		}
		return this;
	}

	/**
	 * exception handler, go to step
	 * @param step
	 * @return
	 */
	public StepBuilder onException(String step) {
		StepErrorHandler handler = new StepErrorHandler(StepTransition.runToStep(step).withResult(FlowResult.Failed));
		flowStep.setErrorHandler(handler);
		return this;
	}
	
	/**
	 * exception handler, go to step
	 * @param step
	 * @return
	 */
	public StepBuilder onException(Enum step) {
		return onException(step.name());
	}
	
	/**
	 * execute one or more tasks in actor, with result handler
	 * @param resultHandler
	 * @param workers
	 * @return
	 */
	public StepBuilder executeActors(
			final UntypedActorFactory actorFactory,
			final BatchOption batchOption,
			final Task...tasks) 
	{
		this.execute(
			new SimpleStepExecution(StepTransition.CALLBACK) {
				@Override
				public StepTransition execute() throws FlowExecutionException {
					final FlowContext mycontext = this.sessionContext;
					final StepCallbackHandler chandler = this.flowStep.getResultHandler();
					final BatchTask batchTask = new BatchTask(batchOption, tasks);
					for (Task task : batchTask.getTasks()) {
						// inject the context
						task.setContext(mycontext);
					}
					ActorRef batchWorker = ActorUtil.createActor(new UntypedActorFactory() {
					
						private static final long serialVersionUID = 1L;

						@Override
						public Actor create() throws Exception {
							return new BatchTaskWorker(batchTask, actorFactory, chandler);
						}
					});
					final FiniteDuration timeout = Duration.create(10, TimeUnit.MINUTES);
					ask(batchWorker, IWorker.WorkerMessageType.REPROCESS_REQUEST, new Timeout(timeout));
					return super.execute();
				}
		});
		
		return this;
	}
	
	/**
	 * execute one or more executable tasks, with result handler
	 * @param resultHandler
	 * @param tasks
	 * @return
	 */
	public StepBuilder executeAsyncTasks(final ExecutableTask...tasks) 
	{
		
		final UntypedActorFactory workerFactory = createAsyncActorFactory();
		return executeActors(workerFactory, null, tasks);
		
	}
	
	/**
	 * execute one or more executable tasks, with result handler
	 * @param resultHandler
	 * @param tasks
	 * @return
	 */
	public StepBuilder executeAsyncPollTasks(
			final ExecutableTask...tasks) 
	{
		
		final UntypedActorFactory workerFactory = createAsyncPollActorFactory();
		return executeActors(workerFactory, null, tasks);
		
	}	

	/**
	 * execute one or more executable tasks, with result handler
	 * @param resultHandler
	 * @param tasks
	 * @return
	 */
	public StepBuilder batchExecuteAsyncTasks(final BatchOption batchOption, final ExecutableTask...tasks) 
	{
		
		final UntypedActorFactory workerFactory = createAsyncActorFactory();
		return executeActors(workerFactory, batchOption, tasks);
		
	}
	
	/**
	 * execute one or more executable tasks, with result handler
	 * @param resultHandler
	 * @param tasks
	 * @return
	 */
	public StepBuilder batchExecuteAsyncPollTasks(
			final BatchOption batchOption,
			final ExecutableTask...tasks) 
	{
		
		final UntypedActorFactory workerFactory = createAsyncPollActorFactory();
		return executeActors(workerFactory, batchOption, tasks);
		
	}	

	/**
	 * utility to create actor factory for async poll task
	 * @param pollMonitor
	 * @return
	 */
	public static UntypedActorFactory createAsyncPollActorFactory() {
		return new UntypedActorFactory() {
			private static final long serialVersionUID = 1L;

			@Override
			public Actor create() throws Exception {
				return new AsyncPollTaskWorker<ExecutableTask>();
			}

		};
	}
	
	/**
	 * utility to create actor fatory for async task
	 * @return
	 */
	public static UntypedActorFactory createAsyncActorFactory() {
		return new UntypedActorFactory() {
			private static final long serialVersionUID = 1L;

			@Override
			public Actor create() throws Exception {
				return new AsyncTaskWorker<ExecutableTask>();
			}

		};
	}

	/**
	 * convenient method
	 * @return
	 */
	public static StepBuilder newBuilder() {
		return new StepBuilder();
	}
}
