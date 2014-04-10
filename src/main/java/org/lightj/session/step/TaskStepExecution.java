package org.lightj.session.step;

import java.util.ArrayList;
import java.util.List;

import org.lightj.session.FlowContext;
import org.lightj.session.exception.FlowExecutionException;
import org.lightj.task.BatchOption;
import org.lightj.task.BatchTask;
import org.lightj.task.BatchTaskWorker;
import org.lightj.task.ExecutableTask;
import org.lightj.task.ExecuteOption;
import org.lightj.task.GroupTask;
import org.lightj.task.NoopTask;
import org.lightj.task.TaskModule;
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
public abstract class TaskStepExecution<T extends FlowContext> extends SimpleStepExecution<T> {
	
	/** batch option */
	private BatchOption batchOption;
	
	/** additional IAroundExecution logic */
	private IAroundExecution<T> extraExec;
			
	/** constructor */
	public TaskStepExecution(
			BatchOption batchOption) 
	{
		this(batchOption, null);
	}
	

	/** constructor */
	public TaskStepExecution(
			BatchOption batchOption, 
			IAroundExecution<T> extraExec) 
	{
		super(StepTransition.CALLBACK);
		this.batchOption = batchOption;
		this.extraExec = extraExec;
	}
	
	public BatchOption getBatchOption() {
		return batchOption;
	}

	public void setBatchOption(BatchOption batchOption) {
		this.batchOption = batchOption;
	}


	@Override
	public StepTransition execute() throws FlowExecutionException {
		
		if (extraExec != null) {
			extraExec.preExecute(this.sessionContext);
		}
		
		List<ExecutableTask> realTasks = getRealTasks();
		
		if (realTasks.isEmpty()) {
			realTasks.add(new NoopTask(new ExecuteOption().setInitDelaySec(1)));
		}
		
		final StepCallbackHandler chandler = this.flowStep.getResultHandler();
		
		for (ExecutableTask task : realTasks) {
			// inject the context
			task.setFlowContext(sessionContext);
		}
		
		final BatchTask batchTask = new BatchTask(batchOption, realTasks.toArray(new ExecutableTask[0]));
		fire(batchTask, chandler);
		
		if (extraExec != null) {
			extraExec.postExecute(this.sessionContext);
		}
		
		return super.execute();
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
	private List<ExecutableTask> getRealTasks() {
		List<ExecutableTask> realTasks = new ArrayList<ExecutableTask>();
		List<ExecutableTask> initialTasks = getInitialTasks();
		for (ExecutableTask task : initialTasks) {
			task.setFlowContext(sessionContext);
			if (task instanceof GroupTask) {
				realTasks.addAll(((GroupTask) task).getTasks());
			}
			else {
				realTasks.add(task);
			}
		}
		return realTasks;
	}
	
	/** return initially user passed in tasks */
	public abstract List<ExecutableTask> getInitialTasks();


}
