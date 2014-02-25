package org.lightj.task;

/**
 * batch option
 * @author biyu
 *
 */
public class BatchOption {
	
	public static enum Strategy {
		MAX_CONCURRENT_RATE_SLIDING;
	};
	
	private final int maxConcurrencyCount;
	private final Strategy strategy;
	
	public BatchOption(int maxConcurrencyCount, Strategy strategy) {
		this.maxConcurrencyCount = maxConcurrencyCount;
		this.strategy = strategy;
	}
	public int getMaxConcurrencyCount() {
		return maxConcurrencyCount;
	}
	public Strategy getStrategy() {
		return strategy;
	}

}
