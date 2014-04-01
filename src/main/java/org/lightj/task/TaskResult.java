package org.lightj.task;

import java.io.Serializable;
import java.util.HashMap;

import org.lightj.util.StringUtil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


/**
 * task results
 * @author biyu
 * 
 */
@JsonInclude(Include.NON_NULL)
public class TaskResult implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2047959597807009292L;

	/** result status */
	protected final TaskResultEnum status;
	
	/** reference to the task */
	protected final Task task;
	
	/** result msg */
	protected final String msg;
	
	/** stack trace */
	protected final Throwable stackTrace;
	
	/** result details as map */
	private final HashMap<String, String> details = new HashMap<String, String>();
	
	/** real result object */
	private Object rawResult;

	/**
	 * constructor
	 * @param task
	 * @param status
	 * @param msg
	 */
	public TaskResult(Task task, TaskResultEnum status, String msg) {
		this(task, null, status, msg, null);
	}

	/**
	 * constructor
	 * @param task
	 * @param realResult
	 * @param status
	 * @param msg
	 */
	public TaskResult(Task task, Object realResult, TaskResultEnum status, String msg) {
		this(task, realResult, status, msg, null);
	}

	/**
	 * constructor
	 * @param task
	 * @param status
	 * @param msg
	 * @param stackTrace
	 */
	public TaskResult(Task task, TaskResultEnum status, String msg, Throwable stackTrace) {
		this(task, null, status, msg, stackTrace);
	}
	
	/**
	 * constructor
	 * @param task
	 * @param status
	 * @param msg
	 * @param stackTrace
	 */
	public TaskResult(Task task, Object realResult, TaskResultEnum status, String msg, Throwable stackTrace) {
		this.rawResult = realResult;
		this.task = task;
		this.status = status;
		this.msg = msg;
		this.stackTrace = stackTrace;
	}

	public String getMsg() {
		return msg;
	}

	public TaskResultEnum getStatus() {
		return status;
	}

	public Task getTask() {
		return task;
	}

	public Throwable getStackTrace() {
		return stackTrace;
	}

	@SuppressWarnings("unchecked")
	public <C> C getRawResult() {
		return (C) rawResult;
	}

	public void setRawResult(Object rawResult) {
		this.rawResult = rawResult;
	}
	
	public void addResultDetail(String name, String value) {
		this.details.put(name, value);
	}

	public HashMap<String, String> getDetails() {
		return details;
	}

	public boolean isComplete() {
		return status.isComplete();
	}

	/** if one if more severe than another, status override */
	public boolean isMoreSevere(TaskResult another) {
		return this.status.getSeverity() > another.status.getSeverity();
	}
	
	static final int STACKTRACE_LEN = 1000;
	public String toString() {
		return String.format("%s,%s,%s", status, msg, StringUtil.getStackTrace(stackTrace, STACKTRACE_LEN));
	}

}
