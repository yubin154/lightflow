package org.lightj.task;

/**
 * task listener interface
 * 
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
public interface ITaskListener {
	
	/**
	 * task is created and accepted by task runner/engine
	 * @param task
	 */
	public void taskCreated(Task task);
	
	/**
	 * task is submitted through the task runner and we have a id to check against even if the session crashed
	 * @param result
	 */
	public void taskSubmitted(Task task);
	
	/**
	 * called when task results are available
	 * @param result
	 */
	public void handleTaskResult(Task task, TaskResult result);

	/**
	 * task is completed, ok to detach listeners is any
	 * @param result
	 */
	public void taskCompleted(Task result);
	
}
