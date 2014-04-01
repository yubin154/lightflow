/*
 * Created on Feb 23, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.lightj.session.FlowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * base task
 * 
 * @author biyu
 */
public abstract class Task {
	
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
	
	/** local context */
	private Map<String, Object> context = new HashMap<String, Object>();
	
	/** optional global context */
	private IGlobalContext globalContext;
	
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
	
	public Map<String, Object> getContext() {
		return context;
	}
	public void addContext(String name, Object value) {
		this.context.put(name, value);
	}
	@SuppressWarnings("unchecked")
	public <T> T getContextValue(String name) {
		return (T) context.get(name);
	}
	static final String FLOW_CTX_NAME = "FLOW_CTX";
	public void setFlowContext(FlowContext flowContext) {
		this.context.put(FLOW_CTX_NAME, flowContext);
	}
	@SuppressWarnings("unchecked")
	public <T extends FlowContext> T getFlowContext() {
		return (T) context.get(FLOW_CTX_NAME);
	}

	public IGlobalContext getGlobalContext() {
		return globalContext;
	}
	public void setGlobalContext(IGlobalContext globalContext) {
		this.globalContext = globalContext;
	}
	public boolean hasGlobalContext() {
		return this.globalContext != null;
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
