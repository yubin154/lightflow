package org.lightj.example.session;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.lightj.session.CtxProp;
import org.lightj.session.CtxProp.CtxDbType;
import org.lightj.session.FlowContext;

public class DummyFlowContext extends FlowContext {
	
	@CtxProp
	private int param1;
	@CtxProp
	private String param2;
	@CtxProp
	private Date param3;
	@CtxProp(dbType=CtxDbType.BLOB)
	private HashMap<String, String> param4;
	@CtxProp(dbType=CtxDbType.BLOB)
	private ArrayList<String> param5;
	public int getParam1() {
		return param1;
	}
	public void setParam1(int param1) {
		this.param1 = param1;
	}
	public String getParam2() {
		return param2;
	}
	public void setParam2(String param2) {
		this.param2 = param2;
	}
	public Date getParam3() {
		return param3;
	}
	public void setParam3(Date param3) {
		this.param3 = param3;
	}
	public HashMap<String, String> getParam4() {
		return param4;
	}
	public void setParam4(HashMap<String, String> param4) {
		this.param4 = param4;
	}
	public ArrayList<String> getParam5() {
		return param5;
	}
	public void setParam5(ArrayList<String> param5) {
		this.param5 = param5;
	}
	
}
