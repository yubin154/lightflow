package org.lightj.example.session.helloworld;

import org.lightj.session.FlowProperties;
import org.lightj.session.FlowResult;
import org.lightj.session.FlowSession;
import org.lightj.session.FlowState;
import org.lightj.session.FlowStepProperties;
import org.lightj.session.exception.FlowExecutionException;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.SimpleStepExecution;
import org.lightj.session.step.StepBuilder;
import org.lightj.session.step.StepImpl;
import org.lightj.session.step.StepTransition;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * a hello world session to demonstrate flow session usage
 * @author biyu
 *
 */
@FlowProperties(typeId="HelloWorld", desc="hello world", interruptible=false,clustered=true)
public class HelloWorldFlow extends FlowSession<HelloWorldFlowContext> {
	
	/**
	 * demonstrate how to do DI for individual step, see {@link HelloWorldFlowFactory}
	 */
	@Autowired(required=true)
	private IFlowStep helloWorldAsyncTaskStep;
	@Autowired(required=true)
	private IFlowStep helloWorldSessionJoinStep;
	@Autowired(required=true)
	private IFlowStep helloWorldDelayStep;
	@Autowired(required=true)
	private IFlowStep helloWorldRetryStep;
	@Autowired(required=true)
	private IFlowStep helloWorldTimeoutStep;
	@Autowired(required=true)
	private IFlowStep helloWorldAsyncPollStep;
	@Autowired(required=true)
	private IFlowStep helloWorldActorBatchStep;
	@Autowired(required=true)
	private IFlowStep helloWorldTestFailureStep;

	/**
	 * start step, synchronous
	 * demonstrate how to execute a synchronous step
	 * @return
	 */
	@FlowStepProperties(stepWeight=0, isFirstStep=true, stepIdx=1)
	public IFlowStep start() {
		
		String nextStep = getParentId()>0 ? "stop" : (sessionContext.isInjectFailure() ? "testFailureStep" : "syncTaskStep");
		return new StepBuilder().runTo(nextStep).getFlowStep();
	}
	
	/**
	 * synchronous execution
	 * @return
	 */
	@FlowStepProperties(stepWeight=1, onSuccess="asyncTaskStep", onException="asyncTaskStep", stepIdx=2)
	public IFlowStep syncTaskStep() {
		return new StepImpl();
	}
	
	/**
	 * step to execute a task by task runner
	 * demonstrate how to execute a runtask asynchronous step
	 * @return
	 */
	@FlowStepProperties(stepWeight=2, stepIdx=3)
	public IFlowStep asyncTaskStep() 
	{
		return helloWorldAsyncTaskStep;
	}
	
	/**
	 * step to launch child session(s) and join on return
	 * demonstrate how to execute a session join asynchronous task
	 * @return
	 */
	@FlowStepProperties(stepWeight=3, stepIdx=4)
	public IFlowStep sessionJoinStep() throws Exception {
		return helloWorldSessionJoinStep;
	}
	
	/**
	 * delay step
	 * @return
	 */
	@FlowStepProperties(stepWeight=1, stepIdx=5)
	public IFlowStep delayStep() 
	{
		return helloWorldDelayStep;
	}

	/**
	 * retry step
	 * @return
	 */
	@FlowStepProperties(stepWeight=1, stepIdx=6)
	public IFlowStep retryStep() 
	{
		return helloWorldRetryStep;
	}
	
	/**
	 * step with timeout
	 * @return
	 */
	@FlowStepProperties(stepWeight=1, stepIdx=7)
	public IFlowStep timeoutStep() {
		return helloWorldTimeoutStep;
	}

	/**
	 * step with actor and poll
	 * @return
	 */
	@FlowStepProperties(stepWeight=1, stepIdx=8)
	public IFlowStep asyncPollStep() {
		return helloWorldAsyncPollStep;
	}
	
	/**
	 * run batch actors
	 * @return
	 */
	@FlowStepProperties(stepWeight=1, stepIdx=9)
	public IFlowStep actorBatchStep() {
		return helloWorldActorBatchStep;
	}
	
	/**
	 * inject failure
	 * @return
	 */
	@FlowStepProperties(stepWeight=1, stepIdx=10)
	public IFlowStep testFailureStep() {
		return helloWorldTestFailureStep;
	}
	
	/**
	 * normal complete step
	 * @return
	 */
	@FlowStepProperties(stepWeight=1, stepIdx=11)
	public IFlowStep stop() {

		StepTransition trans = new StepTransition().inState(FlowState.Completed).withResult(FlowResult.Success);
		return new StepBuilder().parkInState(trans).getFlowStep();

	}
	
	/**
	 * error complete step
	 * @return
	 */
	@FlowStepProperties(stepWeight=0, isErrorStep=true, stepIdx=100)
	public IFlowStep error() {

		StepTransition trans = new StepTransition().inState(FlowState.Completed).withResult(FlowResult.Failed);
		return new StepBuilder().execute(new SimpleStepExecution<HelloWorldFlowContext>(trans) {
			@Override
			public StepTransition execute() throws FlowExecutionException {
				sessionContext.incErrorStepCount();
				return sessionContext.isPauseOnError() ? StepTransition.parkInState(FlowState.Paused, FlowResult.Failed, "pause on error") : defResult;
			}
		}).getFlowStep();

	}
	
}
