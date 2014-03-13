package org.lightj.task;

import java.util.Map;

import org.lightj.session.FlowContext;

/**
 * custom logic to handle task events within a step
 * @author binyu
 *
 */
@SuppressWarnings("rawtypes")
public interface ITaskEventHandler<T extends FlowContext> {

	/**
	 * task is created and accepted by task runner/engine
	 * @param task
	 */
	public void executeOnCreated(T ctx, Task task);
	
	/**
	 * task is submitted through the task runner and we have a id to check against even if the session crashed
	 * @param result
	 */
	public void executeOnSubmitted(T ctx, Task task);
	
	/**
	 * called when task results are available
	 * @param result
	 */
	public void executeOnResult(T ctx, Task task, TaskResult result);


	/**
	 * task completed, have access to all task result 
	 * @param result
	 */
	public TaskResultEnum executeOnCompleted(T ctx, Map<String, TaskResult> results);
}
