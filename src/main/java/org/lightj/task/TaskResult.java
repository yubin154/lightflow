package org.lightj.task;

import java.io.Serializable;

import org.lightj.util.StringUtil;


/**
 * task results
 * @author biyu
 * 
 */ 
public class TaskResult implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2047959597807009292L;

	/** result status */
	protected final TaskResultEnum status;
	
	/** task */
	protected final Task task;
	
	/** result msg */
	protected final String msg;
	
	/** stack trace */
	protected final Throwable stackTrace;
	
	/** real result object */
	private Object realResult;

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
		this.realResult = realResult;
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
	public <C> C getRealResult() {
		return (C) realResult;
	}

	public void setRealResult(Object realResult) {
		this.realResult = realResult;
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
