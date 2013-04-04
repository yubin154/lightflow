package org.lightj.session.step;

import static akka.pattern.Patterns.ask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.lightj.session.FlowContext;
import org.lightj.session.FlowExecutionException;
import org.lightj.session.FlowResult;
import org.lightj.session.FlowSession;
import org.lightj.task.AsyncTaskWorker;
import org.lightj.task.BatchTask;
import org.lightj.task.BatchTaskWorker;
import org.lightj.task.ExecutableTask;
import org.lightj.task.ExecutableTaskWorker;
import org.lightj.task.ExecuteOption;
import org.lightj.task.FlowTask;
import org.lightj.task.IWorker;
import org.lightj.task.Task;
import org.lightj.task.TaskResult;
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
	protected StepImpl flowStep = new StepImpl();
	
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
	 * handle result
	 * @param callbackHandler
	 * @return
	 */
	public StepBuilder onResult(StepCallbackHandler resultHandler) {
		if (resultHandler != null) {
			flowStep.setResultHandler(resultHandler);
		}
		return this;
	}

	/**
	 * exception handler
	 * @param errorHandler
	 * @return
	 */
	public StepBuilder onException(StepErrorHandler errorHandler) {
		if (errorHandler != null) {
			flowStep.setExecutionErrorHandler(errorHandler);
			flowStep.setResultErrorHandler(errorHandler);
		}
		return this;
	}

	/**
	 * result handler, go to step when for success/fail result
	 * @param stepOnSuccess
	 * @param stepOnElse
	 * @return
	 */
	public StepBuilder onResult(String stepOnSuccess, String stepOnElse) {
		flowStep.setExecution(new SimpleStepExecution(StepTransition.runToStep(stepOnSuccess)));
		MappedCallbackHandler handler = new MappedCallbackHandler(StepTransition.runToStep(stepOnElse).withResult(FlowResult.Failed));
		handler.mapResultTo(TaskResultEnum.Success, StepTransition.runToStep(stepOnSuccess));
		flowStep.setResultHandler(handler);
		return this;
	}
	
	/**
	 * exception handler, go to step
	 * @param step
	 * @return
	 */
	public StepBuilder onException(String step) {
		StepErrorHandler handler = new StepErrorHandler(StepTransition.runToStep(step).withResult(FlowResult.Failed));
		flowStep.setExecutionErrorHandler(handler);
		flowStep.setResultErrorHandler(handler);
		return this;
	}
	
	/**
	 * result handler, go to step when for success/fail result
	 * @param stepOnSuccess
	 * @param stepOnElse
	 * @return
	 */
	public StepBuilder onResult(Enum stepOnSuccess, Enum stepOnElse) {
		flowStep.setExecution(new SimpleStepExecution(StepTransition.runToStep(stepOnSuccess)));
		MappedCallbackHandler handler = new MappedCallbackHandler(StepTransition.runToStep(stepOnElse).withResult(FlowResult.Failed));
		handler.mapResultTo(TaskResultEnum.Success, StepTransition.runToStep(stepOnSuccess));
		flowStep.setResultHandler(handler);
		return this;
	}
	
	/**
	 * exception handler, go to step
	 * @param step
	 * @return
	 */
	public StepBuilder onException(Enum step) {
		StepErrorHandler handler = new StepErrorHandler(StepTransition.runToStep(step).withResult(FlowResult.Failed));
		flowStep.setExecutionErrorHandler(handler);
		flowStep.setResultErrorHandler(handler);
		return this;
	}
	
	/**
	 * pass through to step
	 * @param step
	 * @return
	 */
	public StepBuilder runTo(String step) {
		flowStep.setExecution(new SimpleStepExecution(StepTransition.runToStep(step)));
		return this;
	}

	/**
	 * pass through to step
	 * @param step
	 * @return
	 */
	public StepBuilder runTo(Enum step) {
		flowStep.setExecution(new SimpleStepExecution(StepTransition.runToStep(step)));
		return this;
	}
	
	/**
	 * execute one or more async actors, with result handler
	 * @param resultHandler
	 * @param workers
	 * @return
	 */
	public StepBuilder executeAsyncActors(
			final StepCallbackHandler resultHandler,
			final BatchTask batchTask,
			final ActorRef... workers) 
	{
		this.execute(new SimpleStepExecution(StepTransition.CALLBACK) {
			@Override
			public StepTransition execute() throws FlowExecutionException {
				final FlowContext mycontext = this.sessionContext;
				ActorRef batchWorker = ActorUtil.createActor(new UntypedActorFactory() {
					
					private static final long serialVersionUID = 1L;

					@Override
					public Actor create() throws Exception {
						for (Task task : batchTask.getTasks()) {
							task.setContext(mycontext);
						}
						return new BatchTaskWorker(batchTask, workers, resultHandler);
					}
				});
				final FiniteDuration timeout = Duration.create(10, TimeUnit.MINUTES);
				ask(batchWorker, IWorker.WorkerMessageType.PROCESS_REQUEST, new Timeout(timeout));
				return super.execute();
			}
		});
		
		if (resultHandler != null) {
			this.onResult(resultHandler);
		}
		return this;
	}
	
	/**
	 * execute one or more async tasks, with result handler
	 * @param resultHandler
	 * @param tasks
	 * @return
	 */
	public StepBuilder executeAsyncTasks(
			final StepCallbackHandler resultHandler, 
			final ExecutableTask...tasks) 
	{
		
		List<ActorRef> actors = new ArrayList<ActorRef>();
		for (final ExecutableTask task : tasks) {
			actors.add(ActorUtil.createActor(new UntypedActorFactory() {
				private static final long serialVersionUID = 1L;

				@Override
				public Actor create() throws Exception {
					return new AsyncTaskWorker<ExecutableTask>(task) {

						@Override
						public TaskResult processRequestResult(ExecutableTask task, TaskResult result) {
							return result;
						}

						@Override
						public Actor createRequestWorker(ExecutableTask task) {
							return new ExecutableTaskWorker<ExecutableTask>(task);
						}

					};
				}

			}));
		}
		return executeAsyncActors(resultHandler, new BatchTask(tasks), actors.toArray(new ActorRef[0]));
		
	}

	/**
	 * run one or more subflows, with result handler
	 * @param subFlows
	 * @param resultHandler
	 * @return
	 */
	public StepBuilder launchSubFlows(
			final Collection<FlowSession> subFlows, 
			final StepCallbackHandler resultHandler) 
	{
		List<FlowTask> tasks = new ArrayList<FlowTask>();
		for (FlowSession subFlow : subFlows) {
			tasks.add(new FlowTask(subFlow, new ExecuteOption(0, 0)));
		}
		final List<ActorRef> workers = new ArrayList<ActorRef>();
		for (final FlowTask task : tasks) {
			
			workers.add(ActorUtil.createActor(new UntypedActorFactory() {
				
				private static final long serialVersionUID = 1L;

				@Override
				public Actor create() throws Exception {
					return new AsyncTaskWorker<FlowTask>(task) {

						@Override
						public TaskResult processRequestResult(FlowTask task, TaskResult result) {
							return result;
						}

						@Override
						public Actor createRequestWorker(FlowTask task) {
							return new ExecutableTaskWorker<FlowTask>(task);
						}
						
					};
				}
			}));
		}
		FlowStepExecution execution = new FlowStepExecution(StepTransition.CALLBACK, tasks) {
			@Override
			public StepTransition execute() throws FlowExecutionException {
				StepTransition trans = super.execute();
				ActorRef batchWorker = ActorUtil.createActor(new UntypedActorFactory() {
					
					private static final long serialVersionUID = 1L;

					@Override
					public Actor create() throws Exception {
						return new BatchTaskWorker(new BatchTask(), workers.toArray(new ActorRef[0]), resultHandler);
					}
					
				});
				final FiniteDuration timeout = Duration.create(10, TimeUnit.MINUTES);
				ask(batchWorker, IWorker.WorkerMessageType.PROCESS_REQUEST, new Timeout(timeout));
				return trans;
			}
		};
		this.execute(execution);
		if (resultHandler != null) {
			this.onResult(resultHandler);
		}
		return this;
	}
}
