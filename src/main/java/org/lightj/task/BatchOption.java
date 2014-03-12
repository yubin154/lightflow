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
	
	private final int concurrentRate;
	private final Strategy strategy;
	
	public BatchOption(int concurrentRate, Strategy strategy) {
		this.concurrentRate = concurrentRate;
		this.strategy = strategy;
	}
	public int getConcurrentRate() {
		return concurrentRate;
	}
	public Strategy getStrategy() {
		return strategy;
	}

}
