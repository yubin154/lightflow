package org.lightj.task;

import org.lightj.session.FlowContext;

@SuppressWarnings("rawtypes")
public class BatchTask extends Task<FlowContext> {
	
	private final Task[] tasks;
	private final BatchOption batchOption;
	public Task[] getTasks() {
		return tasks;
	}

	public BatchTask(Task...tasks) {
		this.tasks = tasks;
		this.batchOption = null;
	}

	public BatchTask(BatchOption batchOption, Task...tasks) {
		this.tasks = tasks;
		this.batchOption = batchOption;
	}

	public String toString() {
		return "batching tasks";
	}
	
	public BatchOption getBatchOption() {
		return batchOption;
	}

	/**
	 * set all sub tasks with flow context
	 */
	@SuppressWarnings("unchecked")
	public void setContext(FlowContext context) {
		for (Task task : tasks) {
			task.setContext(context);
		}
	}

}
