package org.lightj.example.session.simplehttpflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.lightj.example.session.simplehttpflow.HttpTaskUtil.HttpTaskWrapper;
import org.lightj.session.CtxProp;
import org.lightj.session.CtxProp.CtxDbType;
import org.lightj.session.CtxProp.CtxSaveType;
import org.lightj.session.FlowContext;
import org.lightj.task.ExecutableTask;

@SuppressWarnings("rawtypes")
public class SimpleHttpFlowContext extends FlowContext {
	
	// some input as string, that gets persisted on value change
	@CtxProp(dbType=CtxDbType.VARCHAR, saveType=CtxSaveType.AutoSave)
	private int taskIndex=0;
	
	@CtxProp(dbType=CtxDbType.BLOB, saveType=CtxSaveType.AutoSave)
	private List<HttpTaskWrapper> httpTasks = new ArrayList<HttpTaskWrapper>();
	
	private Collection<ExecutableTask> currentTasks = new ArrayList<ExecutableTask>();
	
	public List<HttpTaskWrapper> getHttpTasks() {
		return httpTasks;
	}
	public void setHttpTasks(List<HttpTaskWrapper> httpTasks) {
		this.httpTasks = httpTasks;
	}
	public void addHttpTask(HttpTaskWrapper...httpTasks) {
		this.httpTasks.addAll(Arrays.asList(httpTasks));
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
	public HttpTaskWrapper getCurrentRequest() {
		return taskIndex<httpTasks.size() ? httpTasks.get(taskIndex) : null;
	}
	public Collection<ExecutableTask> getCurrentTasks() {
		return currentTasks;
	}
	public void setCurrentTasks(Collection<ExecutableTask> currentTasks) {
		this.currentTasks = currentTasks;
	}
	public void addCurrentTask(ExecutableTask...tasks) {
		this.currentTasks.addAll(Arrays.asList(tasks));
	}
}
