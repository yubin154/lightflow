package org.lightj.task;

public class WorkerMessage {
	
	/** callback types */
	public static enum CallbackType {
		created, submitted, taskresult
	}

	private final CallbackType callbackType;
	private final Task task;
	private final TaskResult result;
	
	public WorkerMessage(CallbackType callbackType, Task task, TaskResult result) {
		this.callbackType = callbackType;
		this.task = task;
		this.result = result;
	}

	public CallbackType getCallbackType() {
		return callbackType;
	}
	public Task getTask() {
		return task;
	}
	public TaskResult getResult() {
		return result;
	}

}
