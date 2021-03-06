package org.lightj.session.step;

import java.util.Arrays;
import java.util.List;

import org.lightj.session.FlowResult;
import org.lightj.session.step.TaskFactoryStepExecution.IFlowContextTaskFactory;
import org.lightj.task.BatchOption;
import org.lightj.task.ExecutableTask;
import org.lightj.task.TaskResultEnum;

/**
 * build a flow step
 * @author binyu
 *
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class StepBuilder {

	/**
	 * flow step concrete impl
	 */
	private StepImpl flowStep = new StepImpl();
	
	/**
	 * get flow step impl
	 * @return
	 */
	public StepImpl getFlowStep() {
		return flowStep;
	}

	/**
	 * execute
	 * @param execution
	 * @return
	 */
	public StepBuilder execute(StepExecution execution) {
		if (execution != null) {
			flowStep.setExecution(execution);
		}
		return this;
	}
	
	/**
	 * run to to step
	 * @param step
	 * @return
	 */
	public StepBuilder runTo(String step) {
		this.execute(new SimpleStepExecution(StepTransition.runToStep(step)));
		return this;
	}

	/**
	 * 
	 * @param trans
	 * @return
	 */
	public StepBuilder parkInState(StepTransition trans) {
		this.execute(new SimpleStepExecution(trans));
		return this;
	}
	
	/**
	 * handle result
	 * @param callbackHandler
	 * @return
	 */
	public StepBuilder onResult(StepCallbackHandler resultHandler) {
		flowStep.setResultHandler(resultHandler);
		return this;
	}

	/**
	 * result handler, go to step when for success/fail result
	 * @param stepOnSuccess
	 * @param stepOnElse
	 * @return
	 */
	public StepBuilder onResult(String stepOnSuccess, String stepOnElse) {
		StepTransition onElse = StepTransition.runToStep(stepOnElse).withResult(FlowResult.Failed);
		StepCallbackHandler handler = new StepCallbackHandler();
		handler.mapResultTo(onElse, TaskResultEnum.values());
		handler.mapResultTo(StepTransition.runToStep(stepOnSuccess), TaskResultEnum.Success);
		flowStep.setOrUpdateResultHandler(handler);
		return this;
	}
	
	/**
	 * result handler, go to step when success, all other will go to error step
	 * @param stepOnSuccess
	 * @return
	 */
	public StepBuilder onSuccess(String stepOnSuccess) {
		StepCallbackHandler handler = new StepCallbackHandler(stepOnSuccess);
		handler.mapResultTo(stepOnSuccess, TaskResultEnum.Success);
		flowStep.setOrUpdateResultHandler(handler);
		return this;
	}
	
	/**
	 * exception handler
	 * @param errorHandler
	 * @return
	 */
	public StepBuilder onException(StepErrorHandler errorHandler) {
		if (errorHandler != null) {
			flowStep.setErrorHandler(errorHandler);
		}
		return this;
	}

	/**
	 * exception handler, go to step
	 * @param step
	 * @return
	 */
	public StepBuilder onException(String step) {
		StepErrorHandler handler = new StepErrorHandler(StepTransition.runToStep(step).withResult(FlowResult.Failed));
		flowStep.setErrorHandler(handler);
		return this;
	}
	
	/**
	 * execute one or more tasks in actor
	 * @param tasks
	 * @return
	 */
	public StepBuilder executeTasks(final ExecutableTask...tasks) {
		return this.executeTasks(null, tasks);
	}
	
	/**
	 * execute one or more tasks in actor, with batch options, and around execution block
	 * @param resultHandler
	 * @param workers
	 * @return
	 */
	public StepBuilder executeTasks(
			final BatchOption batchOption,
			final ExecutableTask...tasks) 
	{
		this.execute(
				
			new TaskStepExecution(batchOption, null) {

				@Override
				public List getInitialTasks() {
					return Arrays.asList(tasks);
				}
				
		});
		
		return this;
	}

	/**
	 * execute tasks from task factory
	 * 
	 * @param taskFactory
	 * @return
	 */
	public StepBuilder executeTasks(final IFlowContextTaskFactory taskFactory) 
	{
		this.execute(new TaskFactoryStepExecution(taskFactory));
		return this;
	}

}
