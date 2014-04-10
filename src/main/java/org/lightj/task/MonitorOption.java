package org.lightj.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;



/**
 * how the task monitoring is done
 * @author biyu
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public final class MonitorOption extends ExecuteOption {
	
	public static final long DEFAULT_MONITOR_TIMEOUT = 24 * 60 * 60;
	public static final long DEFAULT_MONITOR_INTERVAL = 10;
	
	/** how often to run, in millisecond */
	private long intervalSec;
	
	public MonitorOption() {
		this(0, DEFAULT_MONITOR_INTERVAL, DEFAULT_MONITOR_TIMEOUT, 0, 0);
	}
	
	public MonitorOption(long intervalSec, long timeOutSec) {
		this(0, intervalSec, timeOutSec, 0, 0);
	}

	public MonitorOption(long initDelaySec, long intervalSec, long timeOutSec, int maxRetry, long retryDelaySec) {
		super(initDelaySec, timeOutSec, maxRetry, retryDelaySec);
		this.intervalSec = intervalSec;
	}

	public long getIntervalSec() {
		return intervalSec;
	}
	public MonitorOption setIntervalSec(long intervalSec) {
		this.intervalSec = intervalSec;
		return this;
	}

}
