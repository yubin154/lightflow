package org.lightj.session;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.lightj.session.step.StepLog;

public class FlowInfo {
	
	private String flowKey;
	private String flowType;
	private String requester;
	private String target;
	private String currentStep;
	private String flowState;
	private String flowResult;
	private String flowStatus;
	private Date createDate;
	private Date endDate;
	private String progress;
	
	private HashMap<String, String> flowContext;
	
	private LinkedHashMap<String, StepLog> executionLogs;

	public String getFlowKey() {
		return flowKey;
	}

	public void setFlowKey(String flowKey) {
		this.flowKey = flowKey;
	}

	public String getFlowType() {
		return flowType;
	}

	public void setFlowType(String flowType) {
		this.flowType = flowType;
	}

	public String getRequester() {
		return requester;
	}

	public void setRequester(String requester) {
		this.requester = requester;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getCurrentStep() {
		return currentStep;
	}

	public void setCurrentStep(String currentStep) {
		this.currentStep = currentStep;
	}

	public String getFlowState() {
		return flowState;
	}

	public void setFlowState(String flowState) {
		this.flowState = flowState;
	}

	public String getFlowResult() {
		return flowResult;
	}

	public void setFlowResult(String flowResult) {
		this.flowResult = flowResult;
	}

	public String getFlowStatus() {
		return flowStatus;
	}

	public void setFlowStatus(String flowStatus) {
		this.flowStatus = flowStatus;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getProgress() {
		return progress;
	}

	public void setProgress(String progress) {
		this.progress = progress;
	}
	
	public HashMap<String, String> getFlowContext() {
		return flowContext;
	}

	public void setFlowContext(HashMap<String, String> flowContext) {
		this.flowContext = flowContext;
	}

	public LinkedHashMap<String, StepLog> getExecutionLogs() {
		return executionLogs;
	}

	public void setExecutionLogs(LinkedHashMap<String, StepLog> executionLogs) {
		this.executionLogs = executionLogs;
	}


}
