package org.lightj.task;


public class ExecuteOption {
	
	/** default timeout in 24 hours */
	private static long DEFAULT_TIMEOUT = 24 * 60 * 60 * 1000L;

	/** timeout delta from now, 0 - infinite */
	private final long timeoutInMs;
	
	/** initial delay in millisecond, 0 - right away */
	private final long initialDelayMs;
	
	/** execution retry on failure */
	private final int maxRetry;
	
	/** retry delay */
	private final long retryDelayMs;
	
	/** timeout at */
	private final long timeoutAt;
	
	public ExecuteOption() {
		this(0,0,0,0);
	}
		
	public ExecuteOption(long initialDelayMs, long timeoutInMs, int execRetry, long retryDelayMs) 
	{
		this.initialDelayMs = initialDelayMs;
		this.timeoutInMs = timeoutInMs;
		this.maxRetry = execRetry;
		this.retryDelayMs = retryDelayMs;
		this.timeoutAt = System.currentTimeMillis() + (timeoutInMs > 0 ? timeoutInMs : DEFAULT_TIMEOUT);
	}

	public ExecuteOption(long initialDelayMs, long timeoutInMs) 
	{
		this(initialDelayMs, timeoutInMs, 0, 0);
	}

	public long getTimeoutInMs() {
		return timeoutInMs;
	}
	public long getTimeOutAt() {
		return timeoutAt;
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
	public boolean hasTimeout() {
		return timeoutInMs > 0;
	}
}
