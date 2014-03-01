package org.lightj.example.session.helloworld;

import org.lightj.session.CtxProp;
import org.lightj.session.FlowContext;

/**
 * Session context
 * @author biyu
 *
 */
public class HelloWorldFlowContext extends FlowContext {
	
	// private context
	private String[] goodHosts;
	private String badHost = "http://asdfewerasdfk";
	private boolean injectFailure = false;
	private boolean controlledFailure = true;
	private boolean pauseOnError = false;
	
	// persisted context
	@CtxProp
	private int batchCount;
	@CtxProp
	private int taskCount;
	@CtxProp
	private int splitCount;
	@CtxProp
	private int retryCount;
	@CtxProp
	private int timeoutCount;
	@CtxProp
	private int errorStepCount;
	public int getTaskCount() {
		return taskCount;
	}
	public void setTaskCount(int taskCount) {
		this.taskCount = taskCount;
	}
	public synchronized void incTaskCount() {
		taskCount++;
	}
	public int getSplitCount() {
		return splitCount;
	}
	public void setSplitCount(int splitCount) {
		this.splitCount = splitCount;
	}
	public synchronized void incSplitCount() {
		splitCount++;
	}
	public int getRetryCount() {
		return retryCount;
	}
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}
	public synchronized void incRetryCount() {
		retryCount++;
	}
	public int getTimeoutCount() {
		return timeoutCount;
	}
	public void setTimeoutCount(int timeoutCount) {
		this.timeoutCount = timeoutCount;
	}
	public synchronized void incTimeoutCount() {
		timeoutCount++;
	}
	public String[] getGoodHosts() {
		return goodHosts;
	}
	public void setGoodHosts(String... goodHosts) {
		this.goodHosts = goodHosts;
	}
	public String getBadHost() {
		return badHost;
	}
	public void setBadHost(String badHost) {
		this.badHost = badHost;
	}
	public int getBatchCount() {
		return batchCount;
	}
	public void setBatchCount(int batchCount) {
		this.batchCount = batchCount;
	}
	public synchronized void incBatchCount() {
		this.batchCount++;
	}
	public boolean isInjectFailure() {
		return injectFailure;
	}
	public void setInjectFailure(boolean injectFailure) {
		this.injectFailure = injectFailure;
	}
	public int getErrorStepCount() {
		return errorStepCount;
	}
	public void setErrorStepCount(int errorStepCount) {
		this.errorStepCount = errorStepCount;
	}
	public synchronized void incErrorStepCount() {
		this.errorStepCount++;
	}
	public boolean isControlledFailure() {
		return controlledFailure;
	}
	public void setControlledFailure(boolean controlledFailure) {
		this.controlledFailure = controlledFailure;
	}
	public boolean isPauseOnError() {
		return pauseOnError;
	}
	public void setPauseOnError(boolean pauseOnError) {
		this.pauseOnError = pauseOnError;
	}

}