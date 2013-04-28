package org.lightj.session.step;

import org.lightj.session.FlowResult;
import org.lightj.session.FlowState;
import org.lightj.util.StringUtil;

/**
 * A transition from one flow step to another, this represents the arch in flow chart from
 * one state to another
 * 
 * possible combinations and their meaning:
 * 
 * actionstatus, nextstep, resultstatus, msg	: meaning
 * 1. running, nextstep, *, * 	: run next step
 * 2. callback, null, *, * 		: park flow, waiting for callback
 * 3. null, null, *, *			: don't change flow state, just change result or do logging
 * 4. completed/canceled/skipped, *, *, *		: stop flow, close session
 * 5. waiting/retry/suspended, nextstep, *, *		: park flow at a step, waiting for external action
 *  
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
public final class StepTransition implements Comparable<StepTransition> {
	
	public static final StepTransition CALLBACK	=	new StepTransition().inState(FlowState.Callback);
	public static final StepTransition NOOP = new StepTransition();
	
	/**
	 * flow activity state
	 */
	private FlowState actionStatus;
	
	/**
	 * flow result state
	 */
	private FlowResult resultStatus;
	
	/**
	 * next flow step to transition to
	 */
	private String nextStep;
	
	/**
	 * message for the flow execution
	 */
	private String msg;
	
	/**
	 * detail
	 */
	private String detail;

	/** a new log instance */
	public static StepTransition newLog(String msg, String detail) {
		return new StepTransition().log(msg, detail);
	}
	
	/** run to a step */
	public static StepTransition runToStep(String stepName) {
		return new StepTransition().toStep(stepName).inState(FlowState.Running);
	}

	/** run to a step */
	public static StepTransition runToStep(Enum stepName) {
		return new StepTransition().toStep(stepName).inState(FlowState.Running);
	}
	
	/**
	 * park in state
	 * @param state
	 * @param result
	 * @param msg
	 * @return
	 */
	public static StepTransition parkInState(FlowState state, FlowResult result, String msg) {
		return new StepTransition().inState(state).withResult(result).withMsg(msg);
	}

	public StepTransition() {}
	
	/**
	 * construct a FlowStepTransition given the destination step
	 * flow state is default to {@link FlowState#Running}
	 * @param nextStep
	 */
	public StepTransition(String nextStep) {
		this(FlowState.Running, nextStep, null, null);
	}
	
	/**
	 * constructor
	 * @param nextStep
	 */
	public StepTransition(Enum nextStep) {
		this(FlowState.Running, nextStep.name(), null, null);
	}

	/**
	 * construct a FlowStepTransition given a flow state and next step it goes to
	 * @param actionStatus
	 * @param nextStep
	 */
	public StepTransition(FlowState actionStatus, String nextStep) {
		this(actionStatus, nextStep, null, null);
	}
	
	/**
	 * construct a FlowStepTransition given a flow state and next step it goes to
	 * @param actionStatus
	 * @param nextStep
	 */
	public StepTransition(FlowState actionStatus, Enum nextStep) {
		this(actionStatus, nextStep.name(), null, null);
	}

	/**
	 * construct a {@link StepExecution} given a flow state, next step, result state and message
	 * @param actionStatus
	 * @param nextStep
	 * @param resultStatus
	 * @param msg
	 */
	public StepTransition(FlowState actionStatus, String nextStep, FlowResult resultStatus, String msg) {
		this.actionStatus = actionStatus;
		this.nextStep = nextStep;
		this.resultStatus = resultStatus;
		this.msg = msg;
	}
	
	public StepTransition inState(FlowState state) {
		this.actionStatus = state;
		return this;
	}

	public StepTransition toStep(String stepName) {
		this.nextStep = stepName;
		this.actionStatus = FlowState.Running;
		return this;
	}

	public StepTransition toStep(Enum stepName) {
		this.nextStep = stepName.name();
		this.actionStatus = FlowState.Running;
		return this;
	}

	public StepTransition withResult(FlowResult result) {
		this.resultStatus = result;
		return this;
	}

	public StepTransition log(String msg, String detail) {
		this.msg = msg;
		this.detail = detail;
		return this;
	}

	public FlowState getActionStatus() {
		return actionStatus;
	}

	public String getNextStep() {
		return nextStep;
	}

	public String getMsg() {
		return msg;
	}
	
	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	public StepTransition withMsg(String msg) {
		this.msg = msg;
		return this;
	}

	public FlowResult getResultStatus() {
		return resultStatus;
	}
	
	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public boolean isEdge() {
		return ((actionStatus==FlowState.Running && nextStep != null) || 
				(actionStatus!=null && actionStatus.isComplete()));
	}

	@Override
	public int compareTo(StepTransition o) {
		return (o != null && actionStatus == o.actionStatus 
				&& resultStatus == o.resultStatus 
				&& StringUtil.equalIgnoreCase(this.nextStep, o.nextStep)) ? 0 : 1;
	}
	
}
