package org.lightj.example.session.simplehttpflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lightj.example.task.HttpTaskRequest;
import org.lightj.session.CtxProp;
import org.lightj.session.CtxProp.CtxDbType;
import org.lightj.session.CtxProp.CtxSaveType;
import org.lightj.session.FlowContext;
import org.lightj.task.ExecutableTask;

public class SimpleHttpFlowContext extends FlowContext {
	
	// some input as string, that gets persisted on value change
	@CtxProp(dbType=CtxDbType.VARCHAR, saveType=CtxSaveType.AutoSave)
	private int taskIndex=0;
	
	@CtxProp(dbType=CtxDbType.BLOB, saveType=CtxSaveType.AutoSave)
	private List<HttpTaskRequest> userRequests = new ArrayList<HttpTaskRequest>();
	
	private ExecutableTask currentTask;
	
	public List<HttpTaskRequest> getUserRequests() {
		return userRequests;
	}
	public void setUserRequests(List<HttpTaskRequest> userRequests) {
		this.userRequests = userRequests;
	}
	public void addUserRequests(HttpTaskRequest...userRequests) {
		this.userRequests.addAll(Arrays.asList(userRequests));
	}
	public int getTaskIndex() {
		return taskIndex;
	}
	public void setTaskIndex(int taskIndex) {
		this.taskIndex = taskIndex;
	}
	public void incTaskIndex() {
		this.taskIndex++;
	}
	public void incTaskIndexIfNotZero() {
		if (this.taskIndex != 0) {
			this.taskIndex++;
		}
	}
	public HttpTaskRequest getCurrentRequest() {
		return taskIndex<userRequests.size() ? userRequests.get(taskIndex) : null;
	}
	
	public ExecutableTask getCurrentTask() {
		return currentTask;
	}
	public void setCurrentTask(ExecutableTask currentTask) {
		this.currentTask = currentTask;
	}
	
}
