package org.lightj.task;

/**
 * Abstraction of polling for an synchronous task
 * @author biyu
 *
 */
public abstract class AsyncPollMonitor {
	
	/** monitor option */
	private final MonitorOption monitorOption;

	/** constructor */
	public AsyncPollMonitor(MonitorOption monitorOption) {
		this.monitorOption = monitorOption;
	}

	/** get monitor option */
	public MonitorOption getMonitorOption() {
		return monitorOption;
	}

	/**
	 * create poll task from request result
	 * @param reqTask
	 * @param reqResult
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public abstract ExecutableTask createPollTask(ExecutableTask task, TaskResult reqResult);

	/**
	 * process poll result
	 * @param result
	 * @return null indicates system should continue polling
	 */
	public abstract TaskResult processPollResult(TaskResult result);

}
