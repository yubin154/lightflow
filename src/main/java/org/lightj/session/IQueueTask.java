package org.lightj.session;

/**
 * Runnable task to be submitted into the queue
 * 
 * @author biyu
 *
 */
public interface IQueueTask {
	
	/**
	 * name of the task
	 * @return
	 */
	public String getName();
	
	/**
	 * group of the task, groupname has to be registered first 
	 * by {@link PriorityRoundRobinThreadPoolExecutor#registTaskGroup(String, QueueTaskType)}
	 * @return
	 */
	public String getGroup();
	
	/**
	 * task priority
	 * @return {@link QueueTaskPriority}
	 */
	public int getPriority();
	
}
