package org.lightj.session.step;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;

import org.lightj.task.Task;
import org.lightj.task.TaskResult;
import org.lightj.util.StringUtil;

/**
 * step log
 * @author binyu
 *
 */
@SuppressWarnings("rawtypes")
public class StepLog implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7176653459501523665L;
	
	private String stepId;
	private String stepName;
	private Date startTime;
	private Date endTime;
	private String stackTrace;
	private LinkedHashMap<String, TaskLog> tasks = new LinkedHashMap<String, StepLog.TaskLog>();
	
	public StepLog() {}
	
	public StepLog(String stepId,
			String stepName) {
		this.stepId = stepId;
		this.stepName = stepName;
		this.startTime = new Date();
	}
	public void addTask(Task task) {
		if (tasks.containsKey(task.getTaskId())) {
			TaskLog ti = tasks.get(task.getTaskId());
			ti.extTaskUuid = task.getExtTaskUuid();
			ti.taskInfo = task.toString();
		}
		else {
			tasks.put(task.getTaskId(), new TaskLog(task.toString(), task.getExtTaskUuid()));
		}
	}
	public void updateTaskResult(Task task, TaskResult taskResult) {
		if (tasks.containsKey(task.getTaskId())) {
			TaskLog ti = tasks.get(task.getTaskId());
			ti.result = taskResult.getStatus().name();
			ti.msg = taskResult.getMsg();
			ti.stackTrace = StringUtil.getStackTrace(taskResult.getStackTrace());
		}
	}
	public void updateStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}
	public void setComplete() {
		this.endTime = new Date();
	}
	
	public String getStepId() {
		return stepId;
	}
	public void setStepId(String stepId) {
		this.stepId = stepId;
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
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public Date getEndTime() {
		return endTime;
	}
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	public LinkedHashMap<String, TaskLog> getTasks() {
		return tasks;
	}
	public void setTasks(LinkedHashMap<String, TaskLog> tasks) {
		this.tasks = tasks;
	}

	/**
	 * task info
	 * @author biyu
	 *
	 */
	public static class TaskLog {
		String taskInfo;
		String extTaskUuid;
		String result;
		String msg;
		String stackTrace;
		
		public TaskLog() {}
		public TaskLog(String taskInfo, String extTaskUuid) {
			this.taskInfo = taskInfo;
			this.extTaskUuid = extTaskUuid;
		}
		public String getTaskInfo() {
			return taskInfo;
		}
		public void setTaskInfo(String taskInfo) {
			this.taskInfo = taskInfo;
		}
		public String getExtTaskUuid() {
			return extTaskUuid;
		}
		public void setExtTaskUuid(String extTaskUuid) {
			this.extTaskUuid = extTaskUuid;
		}
		public String getResult() {
			return result;
		}
		public void setResult(String result) {
			this.result = result;
		}
		public String getMsg() {
			return msg;
		}
		public void setMsg(String msg) {
			this.msg = msg;
		}
		public String getStackTrace() {
			return stackTrace;
		}
		public void setStackTrace(String stackTrace) {
			this.stackTrace = stackTrace;
		}
		
	}

}
