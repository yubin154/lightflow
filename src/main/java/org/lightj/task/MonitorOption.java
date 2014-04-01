package org.lightj.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;



/**
 * how the task monitoring is done
 * @author biyu
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public final class MonitorOption extends ExecuteOption {
	
	public static final long DEFAULT_MONITOR_TIMEOUT = 24 * 60 * 60 * 1000L;
	public static final long DEFAULT_MONITOR_INTERVAL = 10 * 1000L;
	
	/** how often to run, in millisecond */
	private long monitorIntervalMs;
	
	public MonitorOption() {
		this(0, DEFAULT_MONITOR_INTERVAL, DEFAULT_MONITOR_TIMEOUT, 0, 0);
	}
	
	public MonitorOption(long monitorIntervalMs) {
		this(0, monitorIntervalMs, DEFAULT_MONITOR_TIMEOUT, 0, 0);
	}

	public MonitorOption(long monitorIntervalMs, long timeoutInMs) {
		this(0, monitorIntervalMs, timeoutInMs, 0, 0);
	}

	public MonitorOption(long initialDelayMs, long monitorIntervalMs, long timeoutInMs, int maxRetry, long retryDelayMs) {
		super(initialDelayMs, timeoutInMs, maxRetry, retryDelayMs);
		this.monitorIntervalMs = monitorIntervalMs;
	}

	public long getMonitorIntervalMs() {
		return monitorIntervalMs;
	}
	public void setMonitorIntervalMs(long monitorIntervalMs) {
		this.monitorIntervalMs = monitorIntervalMs;
	}
	@JsonIgnore
	public long getScheduleNextRunAt() {
		return System.currentTimeMillis() + this.monitorIntervalMs;
	}

}
