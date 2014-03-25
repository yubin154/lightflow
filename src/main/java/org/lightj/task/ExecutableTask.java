package org.lightj.task;

import akka.actor.ActorRef;

/**
 * base class for executable task that implements execute method
 * @author biyu
 *
 * @param <T>
 */
public abstract class ExecutableTask extends Task {

	/** actor ref executing the task */
	private ActorRef executingActor;
	
	/** constructor */
	public ExecutableTask() {
		super();
	}

	/** constructor */
	public ExecutableTask(ExecuteOption executeOptions) {
		super(executeOptions);
	}
	
	/** constructor */
	public ExecutableTask(ExecuteOption executeOptions, MonitorOption monitorOption) {
		super(executeOptions, monitorOption);
	}

	/** execute with call back actor ref */
	public TaskResult execute(ActorRef executingActor) throws TaskExecutionException {
		this.executingActor = executingActor;
		return execute();
	}
	
	/** reply to triggering actor */
	public void reply(TaskResult res) {
		executingActor.tell(res, null);
	}
	
	/** get executing actor ref */
	protected ActorRef getExecutingActor() {
		return executingActor;
	}

	/** set executing actor ref */
	protected void setExecutingActor(ActorRef executingActor) {
		this.executingActor = executingActor;
	}
	/** actual execution, implement in subclass */
	public abstract TaskResult execute() throws TaskExecutionException;

}
