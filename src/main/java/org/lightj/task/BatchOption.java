package org.lightj.task;

public class BatchOption {
	
	private final int maxConcurrencyCount;
	public BatchOption(int maxConcurrencyCount) {
		this.maxConcurrencyCount = maxConcurrencyCount;
	}
	public int getMaxConcurrencyCount() {
		return maxConcurrencyCount;
	}

}
