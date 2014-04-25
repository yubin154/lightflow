package org.lightj.task;

/**
 * task listener interface, handling callbacks from actor system to non actor system
 * 
 * @author biyu
 *
 */
public interface ITaskListener {
	
	/**
	 * total number of real tasks will be executed
	 * 
	 * @param numOfResults
	 */
	public void setExpectedResultCount(int numOfResults);
	
	/**
	 * task is created and accepted by task worker
	 * 
	 * @param task
	 */
	public void taskCreated(Task task);
	
	/**
	 * task is submitted by the task worker
	 * 
	 * @param result
	 */
	public void taskSubmitted(Task task);
	

	/**
	 * called when task results are available
	 * @param task
	 * @param result
	 * @return how many more results are expected still, when 0 remaining, task worker system will self-terminate
	 */
	public int handleTaskResult(Task task, TaskResult result);
	
}
