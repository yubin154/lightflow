package org.lightj.example.session;

import org.lightj.session.FlowDefinition;
import org.lightj.session.FlowProperties;
import org.lightj.session.FlowResult;
import org.lightj.session.FlowSession;
import org.lightj.session.FlowState;
import org.lightj.session.FlowStepProperties;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.SimpleStepExecution;
import org.lightj.session.step.StepBuilder;
import org.lightj.session.step.StepExecution;
import org.lightj.session.step.StepImpl;
import org.lightj.session.step.StepTransition;


/**
 * a hello world session to demonstrate flow session usage
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
@FlowDefinition(typeId="HelloWorld", desc="hello world", group="TEST")
@FlowProperties(interruptible=false,clustered=true,errorStep="error")
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
		actorStep,
		@FlowStepProperties(stepWeight=1)
		actorPollStep,
		@FlowStepProperties(stepWeight=1)
		actorBatchStep,
		@FlowStepProperties(stepWeight=1)
		stop,
		@FlowStepProperties(stepWeight=0)
		error,
	}

	/**
	 * demonstrate how to do DI for individual step, see {@link HelloWorldFlowStepsImpl}
	 */
	private IFlowStep asyncTaskStep;
	private IFlowStep sessionJoinStep;
	private IFlowStep delayStep;
	private IFlowStep retryStep;
	private IFlowStep timeoutStep;
	private IFlowStep actorStep;
	private IFlowStep actorPollStep;
	private IFlowStep actorBatchStep;

	public IFlowStep getActorPollStep() {
		return actorPollStep;
	}
	public void setActorPollStep(IFlowStep actorPollStep) {
		this.actorPollStep = actorPollStep;
	}
	public IFlowStep getActorStep() {
		return actorStep;
	}
	public void setActorStep(IFlowStep actorStep) {
		this.actorStep = actorStep;
	}
	public IFlowStep getTimeoutStep() {
		return timeoutStep;
	}
	public void setTimeoutStep(IFlowStep timeoutStep) {
		this.timeoutStep = timeoutStep;
	}
	public IFlowStep getRetryStep() {
		return retryStep;
	}
	public void setRetryStep(IFlowStep retryStep) {
		this.retryStep = retryStep;
	}
	public IFlowStep getDelayStep() {
		return delayStep;
	}
	public void setDelayStep(IFlowStep delayStep) {
		this.delayStep = delayStep;
	}
	public IFlowStep getAsyncTaskStep() {
		return asyncTaskStep;
	}
	public void setAsyncTaskStep(IFlowStep asyncTaskStep) {
		this.asyncTaskStep = asyncTaskStep;
	}
	public IFlowStep getSessionJoinStep() {
		return sessionJoinStep;
	}
	public void setSessionJoinStep(IFlowStep sessionJoinStep) {
		this.sessionJoinStep = sessionJoinStep;
	}
	public IFlowStep getActorBatchStep() {
		return actorBatchStep;
	}
	public void setActorBatchStep(IFlowStep actorBatchStep) {
		this.actorBatchStep = actorBatchStep;
	}

	public HelloWorldFlow() {
		super();
	}

	@Override
	protected Enum getFirstStepEnum() {
		return steps.start;
	}

	/**
	 * start step, synchronous
	 * demonstrate how to execute a synchronous step
	 * @return
	 */
	public IFlowStep start() {
		StepExecution execution = new SimpleStepExecution(new StepTransition(getParentId()>0 ? steps.stop : steps.syncTaskStep));
		return new StepBuilder().execute(execution).getFlowStep();
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
		return asyncTaskStep;
	}
	
	/**
	 * step to launch child session(s) and join on return
	 * demonstrate how to execute a session join asynchronous task
	 * @return
	 */
	public IFlowStep sessionJoinStep() throws Exception {
		return sessionJoinStep;
	}
	
	/**
	 * delay step
	 * @return
	 */
	public IFlowStep delayStep() 
	{
		return delayStep;
	}

	/**
	 * retry step
	 * @return
	 */
	public IFlowStep retryStep() 
	{
		return retryStep;
	}
	
	/**
	 * step with timeout
	 * @return
	 */
	public IFlowStep timeoutStep() {
		return timeoutStep;
	}

	/**
	 * step with actor
	 * @return
	 */
	public IFlowStep actorStep() {
		return actorStep;
	}
	
	/**
	 * step with actor and poll
	 * @return
	 */
	public IFlowStep actorPollStep() {
		return actorPollStep;
	}
	
	/**
	 * run batch actors
	 * @return
	 */
	public IFlowStep actorBatchStep() {
		return actorBatchStep;
	}
	
	/**
	 * error complete step
	 * @return
	 */
	public IFlowStep error() {
		StepExecution execution = new SimpleStepExecution(
				new StepTransition().inState(FlowState.Completed).withResult(FlowResult.Failed));

		return new StepBuilder().execute(execution).getFlowStep();
	}
	
	/**
	 * normal complete step
	 * @return
	 */
	public IFlowStep stop() {
		StepExecution execution = new SimpleStepExecution(
				new StepTransition().inState(FlowState.Completed).withResult(FlowResult.Success));

		return new StepBuilder().execute(execution).getFlowStep();
	}
	
}
