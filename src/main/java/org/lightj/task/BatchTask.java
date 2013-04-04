package org.lightj.task;

import org.lightj.session.FlowContext;

@SuppressWarnings("rawtypes")
public class BatchTask extends Task<FlowContext> {
	
	private final Task[] tasks;
	public Task[] getTasks() {
		return tasks;
	}

	public BatchTask(Task...tasks) {
		this.tasks = tasks;
	}

	@Override
	public String getTaskDetail() {
		return "batching tasks";
	}

}
