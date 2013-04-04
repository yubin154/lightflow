package org.lightj.task;

import org.lightj.session.FlowContext;

import akka.actor.ActorRef;

public abstract class ExecutableTask<T extends FlowContext> extends Task<T> {

	public ExecutableTask() {
		super();
	}

	public ExecutableTask(ExecuteOption executeOptions) {
		super(executeOptions);
	}
	
	public abstract TaskResult execute(ActorRef exeuctingActor);

}
