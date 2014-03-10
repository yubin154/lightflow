package org.lightj.session.step;

import java.io.Serializable;

/**
 * step error log
 * @author binyu
 *
 */
public class StepErrorLog implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5298872282369692660L;
	private String stepName;
	private String stackTrace;
	public StepErrorLog() {}
	public StepErrorLog(String stepName, String stackTrace) {
		this.stepName = stepName;
		this.stackTrace = stackTrace;
	}
	public String getStepName() {
		return stepName;
	}
	public void setStepName(String stepName) {
		this.stepName = stepName;
	}
	public String getStackTrace() {
		return stackTrace;
	}
	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}
	
}
