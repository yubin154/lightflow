package org.lightj.task;

/**
 * batch option
 * @author biyu
 *
 */
public class BatchOption {
	
	private final int maxConcurrencyCount;
	public BatchOption(int maxConcurrencyCount) {
		this.maxConcurrencyCount = maxConcurrencyCount;
	}
	public int getMaxConcurrencyCount() {
		return maxConcurrencyCount;
	}

}
