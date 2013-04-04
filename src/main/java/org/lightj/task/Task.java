/*
 * Created on Feb 23, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.task;

import java.util.UUID;

import org.lightj.session.FlowContext;
import org.lightj.util.Log4jProxy;

/**
 * @author biyu
 */
public abstract class Task<T extends FlowContext> {
	
	/** logger */
	static Log4jProxy logger = Log4jProxy.getLogger(Task.class);

	/** task id */
	private final String taskId = "Task|" + UUID.randomUUID().toString();
	
	/** execution option */
	private final ExecuteOption execOptions;
	
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
	
	/////////////////// getters and setters /////////////////
	public String getTaskId() {
		return taskId;
	}
	public ExecuteOption getExecOptions() {
		return execOptions;
	}
	public String getExtTaskUuid() {
		return extTaskUuid;
	}
	public void setExtTaskUuid(String extTaskUuid) {
		this.extTaskUuid = extTaskUuid;
	}

	public abstract String getTaskDetail();

	/** create an immutable TaskResult */
	public final TaskResult createTaskResult(TaskResultEnum status, String msg) {
		return new TaskResult(this, status, msg);
	}
	
	/** create an immutable error result */
	public final TaskResult createErrorResult(TaskResultEnum status, String msg, String stackTrace) {
		return new TaskResult(this, status, msg, stackTrace);
	}

	/** create an immutable error result */
	public final TaskResult createErrorResult(Object realResult, TaskResultEnum status, String msg, String stackTrace) {
		return new TaskResult(this, realResult, status, msg, stackTrace);
	}

	/** create an immutable TaskResult */
	public final TaskResult createTaskResult(Object realResult, TaskResultEnum status, String msg) {
		return new TaskResult(this, realResult, status, msg);
	}
}
