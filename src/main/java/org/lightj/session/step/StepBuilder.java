package org.lightj.session.step;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.lightj.session.FlowContext;
import org.lightj.session.FlowModule;
import org.lightj.session.FlowResult;
import org.lightj.session.exception.FlowExecutionException;
import org.lightj.task.AsyncPollTaskWorker;
import org.lightj.task.AsyncTaskWorker;
import org.lightj.task.BatchOption;
import org.lightj.task.BatchTask;
import org.lightj.task.BatchTaskWorker;
import org.lightj.task.ExecutableTask;
import org.lightj.task.ExecuteOption;
import org.lightj.task.IWorker;
import org.lightj.task.NoopTask;
import org.lightj.task.TaskResultEnum;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

@SuppressWarnings({"rawtypes","unchecked"})
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
		this.execute(new SimpleStepExecution(StepTransition.runToStep(step)));
		return this;
	}

	/**
	 * 
	 * @param trans
	 * @return
	 */
	public StepBuilder parkInState(StepTransition trans) {
		this.execute(new SimpleStepExecution(trans));
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
		handler.mapResultTo(stepOnSuccess, TaskResultEnum.Success);
		flowStep.setOrUpdateResultHandler(handler);
		return this;
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
	 * pick up tasks from context and execute
	 * @param contextName
	 * @param batchOption
	 * @return
	 */
	public StepBuilder executeTasksFromContext(
			final String contextName, 
			final BatchOption batchOption, 
			final IAroundExecution extraExec) 
	{
		this.execute(
				
				new SimpleStepExecution(StepTransition.CALLBACK) {
				
					@Override
					public StepTransition execute() throws FlowExecutionException {
						
						if (extraExec != null) {
							extraExec.preExecute(this.sessionContext);
						}
						
						Object val = this.sessionContext.getValueByName(contextName);
						ArrayList<ExecutableTask> ctasks = new ArrayList<ExecutableTask>();
						if (val instanceof ExecutableTask) {
							ctasks.add((ExecutableTask) val);
						}
						else if (val instanceof ExecutableTask[]) {
							ctasks.addAll(Arrays.asList((ExecutableTask[]) val));
						}
						else if (val instanceof Collection<?>) {
							ctasks.addAll((Collection<ExecutableTask>)val);
						}
						if (ctasks.isEmpty()) {
							ctasks.add(new NoopTask(new ExecuteOption(1000, 0, 0, 0)));
						}
						final FlowContext mycontext = this.sessionContext;
						final StepCallbackHandler chandler = this.flowStep.getResultHandler();
						for (ExecutableTask task : ctasks) {
							// inject the context
							task.setContext(mycontext);
						}
						final BatchTask batchTask = new BatchTask(batchOption, ctasks.toArray(new ExecutableTask[0]));
						fire(batchTask, chandler);
						
						if (extraExec != null) {
							extraExec.postExecute(this.sessionContext);
						}
						
						return super.execute();
					}
					
					private void fire(final BatchTask batchTask, final StepCallbackHandler chandler) {
						
						final UntypedActorFactory actorFactory = batchTask.getTasks()[0].needPolling() ? createAsyncPollActorFactory() : createAsyncActorFactory();
						
						ActorRef batchWorker = FlowModule.getActorSystem().actorOf(
								new Props(new UntypedActorFactory() {
						
							private static final long serialVersionUID = 1L;

							@Override
							public Actor create() throws Exception {
								return new BatchTaskWorker(batchTask, actorFactory, chandler);
							}
						}));
						batchWorker.tell(IWorker.WorkerMessageType.REPROCESS_REQUEST, null);
					}
		});
		
		return this;
	}
	
	/**
	 * execute one or more tasks in actor, with result handler
	 * @param tasks
	 * @return
	 */
	public StepBuilder executeTasks(final ExecutableTask...tasks) {
		return this.executeTasks(null, null, tasks);
	}
	
	/**
	 * execute one or more tasks in actor, with result handler
	 * @param resultHandler
	 * @param workers
	 * @return
	 */
	public StepBuilder executeTasks(
			final BatchOption batchOption,
			final IAroundExecution extraExec,
			final ExecutableTask...tasks) 
	{
		this.execute(
				
			new SimpleStepExecution(StepTransition.CALLBACK) {
				
				@Override
				public StepTransition execute() throws FlowExecutionException {

					if (extraExec != null) {
						extraExec.preExecute(this.sessionContext);
					}
					ExecutableTask[] ntsaks = (tasks.length == 0 ? 
							new ExecutableTask[] {new NoopTask(new ExecuteOption(1000,0,0,0))} :
								tasks);
					final FlowContext mycontext = this.sessionContext;
					final StepCallbackHandler chandler = this.flowStep.getResultHandler();
					for (ExecutableTask task : ntsaks) {
						// inject the context
						task.setContext(mycontext);
					}
					final BatchTask batchTask = new BatchTask(batchOption, ntsaks);
					fire(batchTask, chandler);
					
					if (extraExec != null) {
						extraExec.postExecute(this.sessionContext);
					}
					
					return super.execute();
				}
				
				private void fire(final BatchTask batchTask, final StepCallbackHandler chandler) {
					
					final UntypedActorFactory actorFactory = batchTask.getTasks()[0].needPolling() ? createAsyncPollActorFactory() : createAsyncActorFactory();
					
					ActorRef batchWorker = FlowModule.getActorSystem().actorOf(
							new Props(new UntypedActorFactory() {
					
						private static final long serialVersionUID = 1L;

						@Override
						public Actor create() throws Exception {
							return new BatchTaskWorker(batchTask, actorFactory, chandler);
						}
					}));
					batchWorker.tell(IWorker.WorkerMessageType.REPROCESS_REQUEST, null);
				}
		});
		
		return this;
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
