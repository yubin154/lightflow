package org.lightj.task;

import java.util.ArrayList;
import java.util.List;

import org.lightj.session.FlowContext;
import org.lightj.session.exception.FlowExecutionException;
import org.lightj.session.step.IAroundExecution;
import org.lightj.session.step.SimpleStepExecution;
import org.lightj.session.step.StepCallbackHandler;
import org.lightj.session.step.StepTransition;

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
@SuppressWarnings({ "rawtypes", "unchecked" })
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
	
	@Override
	public StepTransition execute() throws FlowExecutionException {
		
		if (extraExec != null) {
			extraExec.preExecute(this.sessionContext);
		}
		
		List<ExecutableTask> realTasks = getRealTasks();
		
		if (realTasks.isEmpty()) {
			realTasks.add(new NoopTask(new ExecuteOption(1000, 0, 0, 0)));
		}
		
		final StepCallbackHandler chandler = this.flowStep.getResultHandler();
		
		for (ExecutableTask task : realTasks) {
			// inject the context
			task.setContext(sessionContext);
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
		
		batchWorker.tell(IWorker.WorkerMessageType.REPROCESS_REQUEST, null);
	}
	
	/**
	 * possibly fan out one group task to multiple real tasks
	 * @return
	 */
	private List<ExecutableTask> getRealTasks() {
		List<ExecutableTask> realTasks = new ArrayList<ExecutableTask>();
		List<ExecutableTask> initialTasks = getInitialTasks();
		for (ExecutableTask task : initialTasks) {
			task.setContext(sessionContext);
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
