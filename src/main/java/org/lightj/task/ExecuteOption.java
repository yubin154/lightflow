package org.lightj.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * options for task execution, initial delay, timeout, maxRetry, retryDelay
 * 
 * @author binyu
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ExecuteOption {
	
	/** timeout delta from now, 0 - infinite */
	private long timeOutSec;
	
	/** initial delay in millisecond, 0 - right away */
	private long initDelaySec;
	
	/** execution retry on failure */
	private int maxRetry;
	
	/** retry delay */
	private long retryDelaySec;
	
	/** default constructor */
	public ExecuteOption() {
	}
	
	/** full constructor */
	public ExecuteOption(long initDelaySec, long timeOutSec, int maxRetry, long retryDelaySec) 
	{
		this.initDelaySec = initDelaySec;
		this.timeOutSec = timeOutSec;
		this.maxRetry = maxRetry;
		this.retryDelaySec = retryDelaySec;
	}

	public long getTimeOutSec() {
		return timeOutSec;
	}
	public long getInitDelaySec() {
		return initDelaySec;
	}
	public int getMaxRetry() {
		return maxRetry;
	}
	public long getRetryDelaySec() {
		return retryDelaySec;
	}
	public ExecuteOption setTimeOutSec(long timeOutSec) {
		this.timeOutSec = timeOutSec;
		return this;
	}
	public ExecuteOption setInitDelaySec(long initDelaySec) {
		this.initDelaySec = initDelaySec;
		return this;
	}
	public ExecuteOption setMaxRetry(int maxRetry) {
		this.maxRetry = maxRetry;
		return this;
	}
	public ExecuteOption setRetryDelaySec(long retryDelaySec) {
		this.retryDelaySec = retryDelaySec;
		return this;
	}
}
