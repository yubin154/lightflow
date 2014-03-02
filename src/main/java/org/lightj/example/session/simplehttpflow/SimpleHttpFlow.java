package org.lightj.example.session.simplehttpflow;

import org.lightj.session.FlowProperties;
import org.lightj.session.FlowSession;
import org.lightj.session.FlowStepProperties;
import org.lightj.session.step.IFlowStep;
import org.springframework.beans.factory.annotation.Autowired;

@FlowProperties(typeId="SimpleHttp", desc="run a serials of http tasks", clustered=false, interruptible=false, timeoutInSec=0)
public class SimpleHttpFlow extends FlowSession<SimpleHttpFlowContext> {

	//////////////// step implementation /////////////////
	
	@Autowired(required=true)
	private IFlowStep buildHttpTasksStep;
	@Autowired(required=true)
	private IFlowStep runHttpTasksStep;
	@Autowired(required=true)
	private IFlowStep handleErrorStep;
	
	// method with the same name as in flow step enum, framework will use reflection to run each step
	@FlowStepProperties(stepWeight=1, isFirstStep=true, stepIdx=1, onSuccess="runHttpTasks", onElse="handleError")
	public IFlowStep buildHttpTasks() {
		return buildHttpTasksStep;
	}	
	@FlowStepProperties(stepWeight=1, stepIdx=2, onSuccess="buildHttpTasks", onElse="handleError")
	public IFlowStep runHttpTasks() {
		return runHttpTasksStep;
	}
	@FlowStepProperties(stepWeight=0, isErrorStep=true, stepIdx=100)
	public IFlowStep handleError() {
		return handleErrorStep;
	}
	
}
