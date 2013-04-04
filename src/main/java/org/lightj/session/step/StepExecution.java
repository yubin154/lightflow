package org.lightj.session.step;

import org.lightj.session.FlowContext;
import org.lightj.session.FlowExecutionException;
import org.lightj.task.ITaskListener;
import org.lightj.task.Task;
import org.lightj.task.TaskResult;


/**
 * An execution aspect of a flow step, whether it is a execution, or callback handling,
 * or error handling
 * 
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
public abstract class StepExecution<T extends FlowContext> implements ITaskListener {
	
	/**
	 * default flow transition
	 */
	protected StepTransition defResult;
	
	/**
	 * a task listener that handles asynchronous callback
	 */
	protected ITaskListener delegateTaskListener;
	
	/**
	 * parent step that hosts this execution
	 */
	protected IFlowStep flowStep;
	
	/**
	 * a locatable key that can be used to reinstantiate the execution later
	 */
	protected String key;
	
	/**
	 * session context
	 */
	protected T sessionContext;
	
	/**
	 * constructor
	 *
	 */
	StepExecution() {}
	
	/**
	 * construct a new flow step execution with a default transition 
	 * @param transition
	 */
	public StepExecution(StepTransition transition) {
		this.defResult = transition;
	}
	
	/**
	 * do the work
	 * @return
	 * @throws FlowExecutionException
	 */
	public abstract StepTransition execute() throws FlowExecutionException;

	/**
	 * get default transition
	 * @return
	 */
	public StepTransition getDefResult() {
		return defResult;
	}

	/**
	 * set default transition
	 * @param defResult
	 */
	public void setDefResult(StepTransition defResult) {
		this.defResult = defResult;
	}
	
	/**
	 * set delegate task listener
	 * @param delegateTaskListener
	 */
	protected void setDelegateTaskListener(ITaskListener delegateTaskListener) {
		this.delegateTaskListener = delegateTaskListener;
	}

	/**
	 * set parent flowstep that hosts this execution, called from {@link StepImpl}
	 * @param flowStep
	 */
	public void setFlowStep(IFlowStep flowStep) {
		this.flowStep = flowStep;
	}

	/**
	 * set session context
	 * @param sessionContext
	 */
	public void setSessionContext(T sessionContext) {
		this.sessionContext = sessionContext;
	}
	
	/**
	 * resume flow step on callback
	 */
	public final void resumeFlowStep() {
		this.flowStep.resume();
	}
	
	////////////////// ITaskListener interface /////////////////////
	
	/**
	 * called when task is completed
	 * override in subclass with concrete implementation
	 * @param result
	 */
	public void handleTaskResult(Task task, TaskResult result) {
		delegateTaskListener.handleTaskResult(task, result);
	}

	/**
	 * task is submitted through the task runner and we have a id to check against even if the session crashed
	 * @param rstEnum
	 */
	public void taskSubmitted(Task task) {
		delegateTaskListener.taskSubmitted(task);
	}

	/**
	 * task is created
	 */
	public void taskCreated(Task task) {
		delegateTaskListener.taskCreated(task);
	}
	
	/**
	 * task is completed
	 */
	public void taskCompleted(Task task) {
		delegateTaskListener.taskCompleted(task);
	}
	
}
