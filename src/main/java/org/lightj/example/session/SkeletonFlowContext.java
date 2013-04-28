package org.lightj.example.session;

import java.io.Serializable;

import org.lightj.session.CtxProp;
import org.lightj.session.CtxProp.CtxDbType;
import org.lightj.session.CtxProp.CtxSaveType;
import org.lightj.session.FlowContext;

public class SkeletonFlowContext extends FlowContext {
	
	// some input as string, that gets persisted on value change
	@CtxProp(dbType=CtxDbType.VARCHAR, saveType=CtxSaveType.SaveOnChange)
	private String someInput;
	
	// a complex context parameter persisted as json string in blob, saved for every step transition 
	@CtxProp(dbType=CtxDbType.BLOB, saveType=CtxSaveType.AutoSave)
	private Serializable someObject;

	public String getSomeInput() {
		return someInput;
	}
	public void setSomeInput(String someInput) {
		this.someInput = someInput;
	}
	public Serializable getSomeObject() {
		return someObject;
	}
	public void setSomeObject(Serializable someObject) {
		this.someObject = someObject;
	}

}
