package org.lightj.example.session;

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
@SuppressWarnings("rawtypes")
@FlowProperties(typeId="HelloWorld", desc="hello world", interruptible=false,clustered=true)
public class HelloWorldFlow extends FlowSession<HelloWorldFlowContext> {
	
	public static enum steps {
		@FlowStepProperties(stepWeight=0)
		start,
		@FlowStepProperties(stepWeight=1, onSuccess="asyncTaskStep", onException="asyncTaskStep")
		syncTaskStep,
		@FlowStepProperties(stepWeight=2)
		asyncTaskStep,
		@FlowStepProperties(stepWeight=3)
		sessionJoinStep,
		@FlowStepProperties(stepWeight=1)
		delayStep,
		@FlowStepProperties(stepWeight=1)
		retryStep,
		@FlowStepProperties(stepWeight=1)
		timeoutStep,
		@FlowStepProperties(stepWeight=1)
		asyncPollStep,
		@FlowStepProperties(stepWeight=1)
		actorBatchStep,
		@FlowStepProperties(stepWeight=1)
		testFailureStep,
		@FlowStepProperties(stepWeight=1)
		stop,
		@FlowStepProperties(stepWeight=0, isErrorStep=true)
		error,
	}

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

	@Override
	public Enum getFirstStepEnum() {
		return steps.start;
	}

	/**
	 * start step, synchronous
	 * demonstrate how to execute a synchronous step
	 * @return
	 */
	public IFlowStep start() {
		
		Enum nextStep = getParentId()>0 ? steps.stop : (sessionContext.isInjectFailure() ? steps.testFailureStep : steps.syncTaskStep);
		return new StepBuilder().runTo(nextStep).getFlowStep();
	}
	
	/**
	 * synchronous execution
	 * @return
	 */
	public IFlowStep syncTaskStep() {
		return new StepImpl();
	}
	
	/**
	 * step to execute a task by task runner
	 * demonstrate how to execute a runtask asynchronous step
	 * @return
	 */
	public IFlowStep asyncTaskStep() 
	{
		return helloWorldAsyncTaskStep;
	}
	
	/**
	 * step to launch child session(s) and join on return
	 * demonstrate how to execute a session join asynchronous task
	 * @return
	 */
	public IFlowStep sessionJoinStep() throws Exception {
		return helloWorldSessionJoinStep;
	}
	
	/**
	 * delay step
	 * @return
	 */
	public IFlowStep delayStep() 
	{
		return helloWorldDelayStep;
	}

	/**
	 * retry step
	 * @return
	 */
	public IFlowStep retryStep() 
	{
		return helloWorldRetryStep;
	}
	
	/**
	 * step with timeout
	 * @return
	 */
	public IFlowStep timeoutStep() {
		return helloWorldTimeoutStep;
	}

	/**
	 * step with actor and poll
	 * @return
	 */
	public IFlowStep asyncPollStep() {
		return helloWorldAsyncPollStep;
	}
	
	/**
	 * run batch actors
	 * @return
	 */
	public IFlowStep actorBatchStep() {
		return helloWorldActorBatchStep;
	}
	
	/**
	 * inject failure
	 * @return
	 */
	public IFlowStep testFailureStep() {
		return helloWorldTestFailureStep;
	}
	
	/**
	 * error complete step
	 * @return
	 */
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
	
	/**
	 * normal complete step
	 * @return
	 */
	public IFlowStep stop() {

		StepTransition trans = new StepTransition().inState(FlowState.Completed).withResult(FlowResult.Success);
		return new StepBuilder().parkInState(trans).getFlowStep();

	}
	
}
