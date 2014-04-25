package org.lightj.session.step;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.lightj.session.FlowContext;
import org.lightj.session.FlowEvent;
import org.lightj.session.FlowResult;
import org.lightj.session.exception.FlowExecutionException;
import org.lightj.task.BatchTask;
import org.lightj.task.ITaskEventHandler;
import org.lightj.task.ITaskListener;
import org.lightj.task.Task;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;
import org.lightj.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An execution aspect of a flow step to handle callback from task worker actor system
 * 
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
public final class StepCallbackHandler<T extends FlowContext> extends StepExecution<T> implements ITaskListener {
	
	/** logger */
	static Logger logger = LoggerFactory.getLogger(StepCallbackHandler.class.getName());

	/** all results, task id to result map */
	private ConcurrentMap<String, TaskResult> results = 
			new ConcurrentHashMap<String, TaskResult>();
	
	/** map a result status to an edge in flow state machine (where flow goes next) */
	private HashMap<TaskResultEnum, StepExecution> mapOnResults = 
			new HashMap<TaskResultEnum, StepExecution>();
	
	/** number of tasks to track */
	private int numOfTasks;
	private AtomicInteger numOfTaskResults = new AtomicInteger(0);
	
	/** additional handling logic */
	private ITaskEventHandler<T> delegateHandler;
	
	/**
	 * constructor with no default transition
	 * @param transition
	 */
	public StepCallbackHandler() {
		super(null);
	}

	/**
	 * constructor with default transition
	 * @param transition
	 */
	public StepCallbackHandler(String runTo) {
		super(StepTransition.runToStep(runTo));
	}

	/**
	 * construct a result handler with default transition
	 * @param transition
	 */
	public StepCallbackHandler(StepTransition transition) {
		super(transition);
	}

	public ITaskEventHandler<T> getDelegateHandler() {
		return delegateHandler;
	}

	public StepCallbackHandler setDelegateHandler(ITaskEventHandler<T> delegateHandler) {
		this.delegateHandler = delegateHandler;
		return this;
	}

	/**
	 * result handle task completed event
	 * one task is allowed to have only one result
	 */
	public int handleTaskResult(Task task, TaskResult result) 
	{
		// if this is the last result expected
		int resultRemaining = (task instanceof BatchTask) ? 0 : (numOfTasks - numOfTaskResults.incrementAndGet()); 
		
		try {
			// handle result
			if (task != null && result != null) {
				results.put(task.getTaskId(), result);
				sessionContext.saveTaskResult(flowStep.getStepId(), task, result);
			}
			publishStepEvent(FlowEvent.stepOngoing, StepTransition.newLog(
					String.format("%s: %s", result.getStatus(), result.getMsg()), 
					StringUtil.getStackTrace(result.getStackTrace())));
			if (delegateHandler != null) {
				delegateHandler.executeOnResult(sessionContext, task, result);
			}
		} catch (Throwable t) {
			this.flowStep.resume(t);
		}
		
		// all task completed
		if (resultRemaining == 0 && task != null) {
			try {
				StepTransition trans = executeOnCompleted(task);
				if (trans != null && trans.isEdge()) {
					this.flowStep.resume(trans);
				}
			} catch (Throwable t) {
				this.flowStep.resume(t);
			}
		}
		
		return resultRemaining;

	}

	/**
	 * result handle task submitted event
	 */
	public final void taskSubmitted(Task task) {
		if (task != null) {
			sessionContext.addTask(flowStep.getStepId(), task);
			try {
				this.publishStepEvent(FlowEvent.stepOngoing, StepTransition.newLog(task.getExtTaskUuid(), null));
				if (delegateHandler != null) {
					delegateHandler.executeOnSubmitted(sessionContext, task);
				}
			} catch (Throwable t) {
				this.flowStep.resume(t);
			}
		}
	}

	/**
	 * result handle task created event
	 */
	public final void taskCreated(Task task) {
		if (task != null) {
			sessionContext.addTask(flowStep.getStepId(), task);
			try {
				if (delegateHandler != null) {
					delegateHandler.executeOnCreated(sessionContext, task);
				}
			} catch (Throwable t) {
				this.flowStep.resume(t);
			}
		}
	}

	@Override
	public final StepTransition execute() throws FlowExecutionException {
		// noop
		return null;
	}

	/**
	 * do the work when task completed
	 * @param result
	 * @return
	 * @throws FlowExecutionException
	 */
	public synchronized StepTransition executeOnCompleted(Task task)
			throws FlowExecutionException 
	{
		TaskResultEnum status = null;
		// run additional handling logic
		if (delegateHandler != null) {
			status = delegateHandler.executeOnCompleted(sessionContext, results);
		}
		
		if (status == TaskResultEnum.Running) {
			// there are more tasks fired, reset myself for set of new task results
			return StepTransition.CALLBACK;
		}
		else if (status == null) {
			status = aggregateResults(results);
		}
		return mapStatus2Transition(status);
	}
	
	/**
	 * map status to result
	 * @param status
	 * @return
	 */
	public StepTransition mapStatus2Transition(TaskResultEnum status) {
		StepTransition trans = null;
		if (mapOnResults.containsKey(status)) {
			trans = mapOnResults.get(status).execute();
		}
		if (trans != null) {
			return trans;
		} 
		else {
			return defResult;
		}
	}
	
	/**
	 * aggregate and find the most severe result among all
	 * @param results
	 * @return
	 */
	public TaskResultEnum aggregateResults(Map<String, TaskResult> results) {
		TaskResult curRst = null;
		for (Entry<String, TaskResult> entry : results.entrySet()) {
			TaskResult result = entry.getValue();
			if ((curRst == null || result.isMoreSevere(curRst))) {
				curRst = result;
			}
		}
		return curRst.getStatus();
	}
	
	/**
	 * add default mapping if null
	 * @param def
	 */
	@SuppressWarnings("unchecked")
	public void setResultMapIfNull(StepCallbackHandler def) {
		if (mapOnResults.isEmpty()) {
			mapOnResults.putAll(def.mapOnResults);
		}
	}
	
	/**
	 * register a status with result(s)
	 * @param status
	 * @param result
	 */
	public StepCallbackHandler mapResultTo(StepTransition result, TaskResultEnum... statuses) {
		for (TaskResultEnum status : statuses) {
			mapOnResults.put(status, new TransitionWrapper(result));
		}
		return this;
	}

	/**
	 * register a status with result(s)
	 * @param status
	 * @param result
	 */
	public StepCallbackHandler mapResultTo(String stepName, TaskResultEnum... statuses) {
		for (TaskResultEnum status : statuses) {
			mapOnResults.put(status, new TransitionWrapper(StepTransition.runToStep(stepName)));
		}
		return this;
	}

	/**
	 * register step with results
	 * @param stepOnSuccess
	 * @param stepOnElse
	 * @return
	 */
	public StepCallbackHandler mapResult(String stepOnSuccess, String stepOnElse) {
		this.mapResultTo(StepTransition.runToStep(stepOnSuccess), TaskResultEnum.Success);
		this.mapResultTo(StepTransition.runToStep(stepOnElse), TaskResultEnum.Failed, TaskResultEnum.Timeout, TaskResultEnum.Canceled);
		return this;
	}
	
	/**
	 * convenient method to create handler
	 * @param stepOnSuccess
	 * @param stepOnElse
	 * @return
	 */
	public static StepCallbackHandler onResult(String stepOnSuccess, String stepOnElse) {
		StepCallbackHandler handler = new StepCallbackHandler(StepTransition.runToStep(stepOnElse).withResult(FlowResult.Failed));
		handler.mapResultTo(StepTransition.runToStep(stepOnSuccess), TaskResultEnum.Success);
		return handler;
	}
	
	@SuppressWarnings("unchecked")
	public void merge(StepCallbackHandler another) {
		this.mapOnResults.putAll(another.mapOnResults);
		if (another.defResult != null) {
			this.defResult = another.defResult;
		}
	}

	@Override
	public void setExpectedResultCount(int numOfTasks) {
		this.numOfTasks = numOfTasks;
	}
	
	/**
	 * reset this callback listener internal data structure for a new set of tasks
	 */
	public synchronized void reset() {
		results.clear();
		numOfTasks = 0;
		numOfTaskResults.set(0);
	}
	
	/**
	 * simple wrapper for the step transition
	 * @author biyu
	 *
	 */
	private static class TransitionWrapper extends SimpleStepExecution {

		public TransitionWrapper(StepTransition transition) {
			super(transition);
		}
		
	}

}
