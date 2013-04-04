package org.lightj.example.session;

import org.lightj.session.CtxProp;
import org.lightj.session.FlowContext;

/**
 * Session context
 * @author biyu
 *
 */
public class HelloWorldFlowContext extends FlowContext {
	
	private String goodHost = "http://www.ebay.com";
	private String badHost = "http://asdfewerasdfk";
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
	public String getGoodHost() {
		return goodHost;
	}
	public void setGoodHost(String goodHost) {
		this.goodHost = goodHost;
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

}