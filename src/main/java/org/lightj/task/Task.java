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
	private final String taskId = "Task|" + UUID.randomUUID().toString();
	
	/** execution option */
	private ExecuteOption execOptions;
	
	/** monitor option */
	private MonitorOption monitorOption;

	/** external task uuid */
	protected String extTaskUuid;
	
	protected T context;
	
	public T getContext() {
		return context;
	}
	public void setContext(T context) {
		this.context = context;
	}

	public Task() {
		this(new ExecuteOption());
	}
	
	/**
	 * construct a task with a specific name
	 * @param name
	 */
	public Task(ExecuteOption executeOptions) {
		this.execOptions = executeOptions;
	}
	
	public Task(ExecuteOption executeOptions, MonitorOption monitorOption) {
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

	/** create an immutable TaskResult */
	public final TaskResult createTaskResult(TaskResultEnum status, String msg) {
		return new TaskResult(this, status, msg);
	}
	
	/** create an immutable error result */
	public final TaskResult createErrorResult(TaskResultEnum status, String msg, Throwable stackTrace) {
		return new TaskResult(this, status, msg, stackTrace);
	}

	/** create an immutable error result */
	public final TaskResult createErrorResult(Object realResult, TaskResultEnum status, String msg, Throwable stackTrace) {
		return new TaskResult(this, realResult, status, msg, stackTrace);
	}

	/** create an immutable TaskResult */
	public final TaskResult createTaskResult(Object realResult, TaskResultEnum status, String msg) {
		return new TaskResult(this, realResult, status, msg);
	}

}
