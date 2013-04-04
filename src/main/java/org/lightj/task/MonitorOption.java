package org.lightj.task;



/**
 * how the task monitoring is done
 * @author biyu
 *
 */
public final class MonitorOption extends ExecuteOption {
	
	/** how often to run, in millisecond */
	private final long monitorIntervalMs;
	
	public MonitorOption(long monitorIntervalMs) {
		this(0, monitorIntervalMs, 0, 0, 0);
	}

	public MonitorOption(long initialDelayMs, long monitorIntervalMs, long timeoutInMs) {
		this(initialDelayMs, monitorIntervalMs, timeoutInMs, 0, 0);
	}

	public MonitorOption(long initialDelayMs, long monitorIntervalMs, long timeoutInMs, int maxRetry, long retryDelayMs) {
		super(initialDelayMs, timeoutInMs, maxRetry, retryDelayMs);
		this.monitorIntervalMs = monitorIntervalMs;
	}

	public long getMonitorIntervalMs() {
		return monitorIntervalMs;
	}
	public long getScheduleNextRunAt() {
		return System.currentTimeMillis() + this.monitorIntervalMs;
	}

}
