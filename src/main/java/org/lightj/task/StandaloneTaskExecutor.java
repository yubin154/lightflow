package org.lightj.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

/**
 * standalone task executor util
 * 
 * @author biyu
 *
 */
public class StandaloneTaskExecutor {
	
	/** batch option */
	private BatchOption batchOption;
	/** task listener */
	private ITaskListener taskListener;
	/** tasks to be executed */
	private ExecutableTask[] tasks;
	
	/**
	 * constructor
	 * @param batchOption
	 * @param taskListener
	 * @param tasks
	 */
	public StandaloneTaskExecutor(
			BatchOption batchOption,
			ITaskListener taskListener,
			ExecutableTask... tasks) {
		this.batchOption = batchOption;
		this.taskListener = taskListener;
		this.tasks = tasks;
	}

	/**
	 * execute tasks
	 */
	public void execute() {
		
		List<ExecutableTask> realTasks = getRealTasks();
		
		if (realTasks.isEmpty()) {
			realTasks.add(new NoopTask(new ExecuteOption().setInitDelaySec(1)));
		}
		
		final BatchTask batchTask = new BatchTask(batchOption, realTasks.toArray(new ExecutableTask[0]));
		fire(batchTask, taskListener);
		
	}
	
	/**
	 * fire tasks to actor system
	 * @param batchTask
	 * @param chandler
	 */
	private void fire(final BatchTask batchTask, final ITaskListener chandler) {
		
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<ExecutableTask> getRealTasks() {
		List<ExecutableTask> realTasks = new ArrayList<ExecutableTask>();
		List<ExecutableTask> initialTasks = Arrays.asList(tasks);
		for (ExecutableTask task : initialTasks) {
			if (task instanceof GroupTask) {
				realTasks.addAll(((GroupTask) task).getTasks());
			}
			else {
				realTasks.add(task);
			}
		}
		return realTasks;
	}
	


}
