package org.lightj.session.step;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.lightj.session.FlowContext;
import org.lightj.session.FlowExecutionException;
import org.lightj.session.FlowResult;
import org.lightj.task.Task;
import org.lightj.task.TaskResult;
import org.lightj.util.Log4jProxy;


/**
 * An execution aspect of a flow step to handle asynchronous callback
 * 
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
public abstract class StepCallbackHandler<T extends FlowContext> extends StepExecution<T> {
	
	static Log4jProxy logger = Log4jProxy.getLogger(StepCallbackHandler.class.getName());

	/** callback types */
	public static final int TYPE_CREATED		=	1;
	public static final int TYPE_SUBMITTED		=	2;
	public static final int TYPE_TASKRESULT		=	3;
	public static final int TYPE_COMPLETED		=	4;

	/** all callbacks */
	private ConcurrentLinkedQueue<CallbackWrapper> callbacks = new ConcurrentLinkedQueue<StepCallbackHandler.CallbackWrapper>();
	
	/** all results */
	protected ConcurrentMap<String, TaskResult> results = new ConcurrentHashMap<String, TaskResult>();
	
	/**
	 * construct a result handler with default transition
	 * @param transition
	 */
	public StepCallbackHandler(StepTransition transition) {
		super(transition);
	}
	
	/**
	 * result handle task completed event
	 */
	public final void handleTaskResult(Task task, TaskResult result) {
		if (result != null) {
			callbacks.offer(new CallbackWrapper(TYPE_TASKRESULT, task, result));
			results.put(task.getTaskId(), result);
			sessionContext.saveTaskResult(flowStep.getStepId(), task, result);
		}
		resumeFlowStep();
	}

	/**
	 * result handle task submitted event
	 */
	public final void taskSubmitted(Task task) {
		if (task != null) {
			callbacks.offer(new CallbackWrapper(TYPE_SUBMITTED, task));
			sessionContext.addTask(flowStep.getStepId(), task);
		}
		resumeFlowStep();
	}

	/**
	 * result handle task created event
	 */
	public final void taskCreated(Task task) {
		if (task != null) {
			callbacks.offer(new CallbackWrapper(TYPE_CREATED, task));
			sessionContext.addTask(flowStep.getStepId(), task);
		}
		resumeFlowStep();
	}

	/**
	 * result handle task completion event
	 */
	public final void taskCompleted(Task task) {
		if (task != null) {
			callbacks.offer(new CallbackWrapper(TYPE_COMPLETED, task));
		}
		resumeFlowStep();
	}

	@Override
	public final StepTransition execute() throws FlowExecutionException {
		CallbackWrapper wrapper = null;
		if ((wrapper = callbacks.poll()) != null) {
	 		switch (wrapper.callbackType) {
			case TYPE_SUBMITTED:
				return executeOnSubmitted(wrapper.task);
			case TYPE_TASKRESULT:
				return executeOnResult(wrapper.task, wrapper.result);
			case TYPE_CREATED:
				return executeOnCreated(wrapper.task);
			case TYPE_COMPLETED:
				return executeOnCompleted(wrapper.task);
			default:
				throw new FlowExecutionException("Invalid callback type " + wrapper.callbackType);
			}
		}
		return StepTransition.NOOP;
	}
	
	/** convert {@link ICallableResult} status to {@link FlowResult} */
	protected FlowResult convertStatus(TaskResult result) {
		switch(result.getStatus()) {
		case Success:
			return FlowResult.Success;
		case Failed:
			return FlowResult.Failed;
		case Timeout:
			return FlowResult.Timeout;
		case Canceled:
			return FlowResult.Canceled;
		}
		return FlowResult.Unknown;
	}
	
	/**
	 * callback data wrapper
	 * 
	 * @author biyu
	 *
	 */
	static class CallbackWrapper {
		final int callbackType;
		final Task task;
		final TaskResult result;
		CallbackWrapper(int type, Task task, TaskResult result) {
			this.callbackType = type;
			this.task = task;
			this.result = result;
		}
		CallbackWrapper(int type, Task task) {
			this(type, task, null);
		}
	}

	/**
	 * do the work when task completed
	 * @param result
	 * @return
	 * @throws FlowExecutionException
	 */
	public abstract StepTransition executeOnCompleted(Task task) throws FlowExecutionException;
	
	/**
	 * do the work when task submitted
	 * @param task
	 * @return
	 * @throws FlowExecutionException
	 */
	public abstract StepTransition executeOnSubmitted(Task task) throws FlowExecutionException;
	
	/**
	 * do the work when task is created
	 * @param task
	 * @return
	 * @throws FlowExecutionException
	 */
	public abstract StepTransition executeOnCreated(Task task) throws FlowExecutionException;
	
	/**
	 * do the work when task generates some result
	 * @param result
	 * @return
	 * @throws FlowExecutionException
	 */
	public abstract StepTransition executeOnResult(Task task, TaskResult result) throws FlowExecutionException;
	
}
