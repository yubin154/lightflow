package org.lightj.task;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * options for task execution, initial delay, timeout, maxRetry, retryDelay
 * 
 * @author binyu
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ExecuteOption {
	
	/** default timeout in 24 hours */
	private static long DEFAULT_TIMEOUT = 24 * 60 * 60 * 1000L;

	/** timeout delta from now, 0 - infinite */
	private long timeoutInMs;
	
	/** initial delay in millisecond, 0 - right away */
	private long initialDelayMs;
	
	/** execution retry on failure */
	private int maxRetry;
	
	/** retry delay */
	private long retryDelayMs;
	
	public ExecuteOption() {
		this(0,0,0,0);
	}
		
	public ExecuteOption(long initialDelayMs, long timeoutInMs, int execRetry, long retryDelayMs) 
	{
		this.initialDelayMs = initialDelayMs;
		this.timeoutInMs = timeoutInMs;
		this.maxRetry = execRetry;
		this.retryDelayMs = retryDelayMs;
	}

	public ExecuteOption(long initialDelayMs, long timeoutInMs) 
	{
		this(initialDelayMs, timeoutInMs, 0, 0);
	}

	public long getTimeoutInMs() {
		return timeoutInMs;
	}
	@JsonIgnore
	public long getTimeOutAt() {
		return System.currentTimeMillis() + (timeoutInMs > 0 ? timeoutInMs : DEFAULT_TIMEOUT);
	}
	public long getInitialDelayMs() {
		return initialDelayMs;
	}
	public int getMaxRetry() {
		return maxRetry;
	}
	public long getRetryDelayMs() {
		return retryDelayMs;
	}
	@JsonIgnore
	public boolean hasTimeout() {
		return timeoutInMs > 0;
	}
	public void setTimeoutInMs(long timeoutInMs) {
		this.timeoutInMs = timeoutInMs;
	}
	public void setInitialDelayMs(long initialDelayMs) {
		this.initialDelayMs = initialDelayMs;
	}
	public void setMaxRetry(int maxRetry) {
		this.maxRetry = maxRetry;
	}
	public void setRetryDelayMs(long retryDelayMs) {
		this.retryDelayMs = retryDelayMs;
	}
}
