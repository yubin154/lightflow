package org.lightj.task;

import org.lightj.session.FlowContext;

import akka.actor.ActorRef;

public abstract class ExecutableTask<T extends FlowContext> extends Task<T> {

	private ActorRef executingActor;
	
	public ExecutableTask() {
		super();
	}

	public ExecutableTask(ExecuteOption executeOptions) {
		super(executeOptions);
	}
	
	public ExecutableTask(ExecuteOption executeOptions, MonitorOption monitorOption) {
		super(executeOptions, monitorOption);
	}

	public TaskResult execute(ActorRef executingActor) throws TaskExecutionException {
		this.executingActor = executingActor;
		return execute();
	}
	
	public void reply(TaskResult res) {
		executingActor.tell(res, null);
	}
	protected ActorRef getExecutingActor() {
		return executingActor;
	}
	protected void setExecutingActor(ActorRef executingActor) {
		this.executingActor = executingActor;
	}

	public abstract TaskResult execute() throws TaskExecutionException;

}
