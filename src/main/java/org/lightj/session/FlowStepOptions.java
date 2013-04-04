package org.lightj.session;

public class FlowStepOptions {
	
	/**
	 * step description
	 */
	private String desciption;

	/**
	 * relative weight of the step, used to calculate flow progress
	 */
	private int stepWeight = 1;
	
	/**
	 * setting this property to true will force flow driver
	 * to persist logs of a flow step execution to session step log
	 * at end of a flow step execution
	 */
	private boolean loggingEnabled = true;
	
	/**
	 * time out after
	 */
	private int timeoutMs = 0;
	
	public int getTimeoutMs() {
		return timeoutMs;
	}

	public void setTimeoutMs(int timeoutMs) {
		this.timeoutMs = timeoutMs;
	}

	public int getStepWeight() {
		return stepWeight;
	}

	public boolean loggingEnabled() {
		return loggingEnabled;
	}

	public String getDesciption() {
		return desciption;
	}

	public void setPredefinedProperties(String desc, int stepWeight, boolean logging, int timeoutMillisec) {
		this.desciption = desc;
		this.stepWeight = stepWeight;
		this.loggingEnabled = logging;
		if (this.timeoutMs <= 0) {
			this.timeoutMs = Math.max(0, timeoutMillisec);
		}
	}
	
}
