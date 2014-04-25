package org.lightj.session.step;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lightj.session.FlowContext;
import org.lightj.session.exception.FlowExecutionException;
import org.lightj.task.BatchOption;
import org.lightj.task.BatchTask;
import org.lightj.task.BatchTaskWorker;
import org.lightj.task.ExecutableTask;
import org.lightj.task.ExecuteOption;
import org.lightj.task.GroupTask;
import org.lightj.task.ITaskEventHandler;
import org.lightj.task.NoopTask;
import org.lightj.task.Task;
import org.lightj.task.TaskModule;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;
import org.lightj.task.WorkerMessage;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

/**
 * executing task(s) in the flowstep, through actor system
 * 
 * @author binyu
 *
 * @param <T>
 */
@SuppressWarnings({ "rawtypes"})
public class TaskFactoryStepExecution<T extends FlowContext> extends StepExecution<T> {
	
	/** user provide taskFactory */
	private IFlowContextTaskFactory<T> taskFactory;
	
	/** sequence, for sequentially executing multiple task per step */
	private int sequence = 0;
	
	/** constructor */
	public TaskFactoryStepExecution(IFlowContextTaskFactory<T> taskFactory) 
	{
		this.taskFactory = taskFactory;
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public StepTransition execute() throws FlowExecutionException {

		TaskInFlow<T> taskInFlow = null; 
		
		taskInFlow = taskFactory.createTaskInFlow(sessionContext, sequence++);
		
		if (taskInFlow != null) {
			
			// has task to run, run it
			List<ExecutableTask> realTasks = new ArrayList<ExecutableTask>();
			if (taskInFlow.tasks != null) {
				realTasks.addAll(getRealTasks(taskInFlow.tasks));
			}
			if (realTasks.isEmpty()) {
				realTasks.add(new NoopTask(new ExecuteOption().setInitDelaySec(1)));
			}
			for (ExecutableTask task : realTasks) {
				// inject the context
				task.setFlowContext(sessionContext);
			}

			final BatchTask batchTask = new BatchTask(taskInFlow.batchOption,
					realTasks.toArray(new ExecutableTask[0]));

			final StepCallbackHandler chandler = this.flowStep.getResultHandler();
			TaskEventHandlerWrapper handler = new TaskEventHandlerWrapper(taskInFlow.taskEventHandler);
			chandler.setDelegateHandler(handler);

			// reset callback listeners if not the first time this Step is run
			if (sequence > 1) {
				this.flowStep.getResultHandler().reset();
			}
			
			fire(batchTask, chandler);
			
			// always wait for callback
			return StepTransition.CALLBACK;
		}
		else {
			// no more task to run, go to next step
			return this.flowStep.getResultHandler().mapStatus2Transition(null);
		}

	}
	
	/**
	 * fire tasks to actor system
	 * @param batchTask
	 * @param chandler
	 */
	private void fire(final BatchTask batchTask, final StepCallbackHandler chandler) {
		
		final UntypedActorFactory actorFactory = batchTask.getTasks()[0].needPolling() 
				? TaskModule.getAsyncPollWorkerFactory() : TaskModule.getAsyncWorkerFactory();
		
		ActorRef batchWorker = TaskModule.getActorSystem().actorOf(
				new Props(new UntypedActorFactory() {
		
			private static final long serialVersionUID = 1L;

			@Override
			public Actor create() throws Exception {
				return new BatchTaskWorker(batchTask, actorFactory, chandler);
			}
		}));
		
		batchWorker.tell(WorkerMessage.Type.PROCESS_REQUEST, null);
	}
	
	/**
	 * possibly fan out one group task to multiple real tasks
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<ExecutableTask> getRealTasks(ExecutableTask... initialTasks) {
		List<ExecutableTask> realTasks = new ArrayList<ExecutableTask>();
		for (ExecutableTask initialTask : initialTasks) {
			initialTask.setFlowContext(sessionContext);
			if (initialTask instanceof GroupTask) {
				realTasks.addAll(((GroupTask) initialTask).getTasks());
			}
			else {
				realTasks.add(initialTask);
			}
		}
		return realTasks;
	}
	
	/**
	 * base on flow context, create task to be executed in flow
	 * @author biyu
	 *
	 * @param <T>
	 */
	public interface IFlowContextTaskFactory<T extends FlowContext> {
		
		/**
		 * build task with flow context, and sequence number
		 * @param context
		 * @param sequence
		 * @return
		 */
		public TaskInFlow<T> createTaskInFlow(T context, int sequence);
		
	}
	
	/**
	 * everything needed to run this task from within a flow
	 * @author biyu
	 *
	 * @param <T>
	 */
	public static class TaskInFlow<T extends FlowContext> 
	{
		public ExecutableTask[] tasks;
		public BatchOption batchOption;
		public ITaskEventHandler taskEventHandler;
		public TaskInFlow() {}
		public TaskInFlow(BatchOption batchOption, ITaskEventHandler handler, ExecutableTask... tasks) {
			this.tasks = tasks;
			this.batchOption = batchOption;
			this.taskEventHandler = handler;
		}
	}
	
	/**
	 * intercept callbacks to run more tasks from factory
	 * @author biyu
	 *
	 */
	@SuppressWarnings("unchecked")
	class TaskEventHandlerWrapper implements ITaskEventHandler 
	{
		ITaskEventHandler delegate;
		public TaskEventHandlerWrapper(ITaskEventHandler delegate) 
		{
			this.delegate = delegate;
		}
		
		@Override
		public void executeOnCreated(FlowContext ctx, Task task) {
			if (delegate != null) {
				delegate.executeOnCreated(ctx, task);
			}
		}

		@Override
		public void executeOnSubmitted(FlowContext ctx, Task task) {
			if (delegate != null) {
				delegate.executeOnSubmitted(ctx, task);
			}
		}

		@Override
		public void executeOnResult(FlowContext ctx, Task task,
				TaskResult result) {
			if (delegate != null) {
				delegate.executeOnResult(ctx, task, result);
			}
		}

		@Override
		public TaskResultEnum executeOnCompleted(FlowContext ctx, Map results) {
			if (delegate != null) {
				TaskResultEnum result = delegate.executeOnCompleted(ctx, results);
				if (result == TaskResultEnum.Running) {
					// launch more tasks
					TaskFactoryStepExecution.this.execute();
				}
				return result;
			}
			return null;
		}
	}
	


}
