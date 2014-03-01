package org.lightj.example.session.simplehttpflow;

import java.io.Serializable;

import org.lightj.session.CtxProp;
import org.lightj.session.CtxProp.CtxDbType;
import org.lightj.session.CtxProp.CtxSaveType;
import org.lightj.session.FlowContext;

public class SimpleHttpFlowContext extends FlowContext {
	
	// some input as string, that gets persisted on value change
	@CtxProp(dbType=CtxDbType.VARCHAR, saveType=CtxSaveType.AutoSave)
	private int taskIndex;
	
	// a complex context parameter persisted as json string in blob, saved for every step transition 
	@CtxProp(dbType=CtxDbType.BLOB, saveType=CtxSaveType.AutoSave)
	private Serializable someObject;

	public Serializable getSomeObject() {
		return someObject;
	}
	public void setSomeObject(Serializable someObject) {
		this.someObject = someObject;
	}
	public int getTaskIndex() {
		return taskIndex;
	}
	public void setTaskIndex(int taskIndex) {
		this.taskIndex = taskIndex;
	}

}
