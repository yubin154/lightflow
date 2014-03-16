/*
 * Created on Feb 23, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.task;

import java.util.UUID;

import org.lightj.session.FlowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * base task
 * 
 * @author biyu
 */
public abstract class Task<T extends FlowContext> {
	
	/** logger */
	static Logger logger = LoggerFactory.getLogger(Task.class);

	/** task id */
	private final String taskId;
	
	/** execution option */
	protected ExecuteOption execOptions;
	
	/** monitor option */
	protected MonitorOption monitorOption;

	/** external task uuid */
	protected String extTaskUuid;
	
	/** session context */
	protected T context;
	
	public Task() {
		this(new ExecuteOption());
	}
	
	/**
	 * construct a task with exeucte option
	 * @param name
	 */
	public Task(ExecuteOption executeOptions) {
		this(executeOptions, null);
	}
	
	/**
	 * 
	 * @param executeOptions
	 * @param monitorOption
	 */
	public Task(ExecuteOption executeOptions, MonitorOption monitorOption) {
		this.taskId = String.format("%s|%s", this.getClass().getSimpleName(), UUID.randomUUID().toString());
		this.execOptions = executeOptions;
		this.monitorOption = monitorOption;
	}

	/////////////////// getters and setters /////////////////
	public String getTaskId() {
		return taskId;
	}
	public ExecuteOption getExecOptions() {
		return execOptions;
	}
	protected void setExecOptions(ExecuteOption execOptions) {
		this.execOptions = execOptions;
	}
	public MonitorOption getMonitorOption() {
		return monitorOption;
	}
	public String getExtTaskUuid() {
		return extTaskUuid;
	}
	public void setExtTaskUuid(String extTaskUuid) {
		this.extTaskUuid = extTaskUuid;
	}
	public boolean needPolling() {
		return monitorOption != null;
	}
	
	public T getContext() {
		return context;
	}
	public void setContext(T context) {
		this.context = context;
	}


	/** create an immutable success result */
	public final TaskResult succeeded() {
		return new TaskResult(this, TaskResultEnum.Success, null);
	}
	
	/** create an immutable TaskResult */
	public final TaskResult hasResult(TaskResultEnum status, String msg) {
		return new TaskResult(this, status, msg);
	}
	
	/** create an immutable error result */
	public final TaskResult failed(String msg, Throwable stackTrace) {
		return new TaskResult(this, TaskResultEnum.Failed, msg, stackTrace);
	}

	/** create an immutable error result */
	public final TaskResult failed(TaskResultEnum status, String msg, Throwable stackTrace) {
		return new TaskResult(this, status, msg, stackTrace);
	}

	/** create an immutable error result */
	public final TaskResult failed(Object realResult, TaskResultEnum status, String msg, Throwable stackTrace) {
		return new TaskResult(this, realResult, status, msg, stackTrace);
	}

	/** create an immutable TaskResult */
	public final TaskResult hasResult(Object realResult, TaskResultEnum status, String msg) {
		return new TaskResult(this, realResult, status, msg);
	}

}
