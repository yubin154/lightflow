package org.lightj.session.step;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.lightj.session.FlowContext;
import org.lightj.session.FlowResult;
import org.lightj.session.exception.FlowExecutionException;
import org.lightj.task.BatchTask;
import org.lightj.task.ITaskListener;
import org.lightj.task.Task;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;
import org.lightj.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An execution aspect of a flow step to handle asynchronous callback
 * 
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
public class StepCallbackHandler<T extends FlowContext> extends StepExecution<T> implements ITaskListener {
	
	/** logger */
	static Logger logger = LoggerFactory.getLogger(StepCallbackHandler.class.getName());

	/** all results */
	protected ConcurrentMap<String, TaskResult> results = 
			new ConcurrentHashMap<String, TaskResult>();
	
	/** map a result status to a flow step result */
	protected HashMap<TaskResultEnum, StepExecution> mapOnResults = 
			new HashMap<TaskResultEnum, StepExecution>();
	
	/** number of tasks for tracking */
	protected int numOfTasks = 1;
	protected AtomicInteger numOfTaskResults = new AtomicInteger(0);
	
	/**
	 * constructor with no defaul transition
	 * @param transition
	 */
	public StepCallbackHandler() {
		super(null);
	}

	/**
	 * constructor with no defaul transition
	 * @param transition
	 */
	public StepCallbackHandler(String runTo) {
		super(StepTransition.runToStep(runTo));
	}

	/**
	 * constructor with no defaul transition
	 * @param transition
	 */
	public StepCallbackHandler(Enum runTo) {
		super(StepTransition.runToStep(runTo));
	}

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
	public final int handleTaskResult(Task task, TaskResult result) 
	{
		// if this is the last result expected
		int resultRemaining = (task instanceof BatchTask) ? 0 : (numOfTasks - numOfTaskResults.incrementAndGet()); 
		
		try {
			// handle result
			if (task != null && result != null) {
				results.put(task.getTaskId(), result);
				sessionContext.saveTaskResult(flowStep.getStepId(), task, result);
			}
			StepTransition trans = executeOnResult(task, result);
			if (trans != null && trans.isEdge()) {
				this.flowStep.resume(trans);
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
				StepTransition trans = executeOnSubmitted(task);
				if (trans != null && trans.isEdge()) {
					this.flowStep.resume(trans);
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
				StepTransition trans = executeOnCreated(task);
				if (trans != null && trans.isEdge()) {
					this.flowStep.resume(trans);
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
	 * do the work when task submitted
	 * @param task
	 * @return
	 * @throws FlowExecutionException
	 */
	public StepTransition executeOnSubmitted(Task task) throws FlowExecutionException {
		StepTransition transition = StepTransition.newLog(task.getTaskId(), null);
		return transition;
	}

	/**
	 * do the work when task is created
	 * @param task
	 * @return
	 * @throws FlowExecutionException
	 */
	public StepTransition executeOnCreated(Task task) throws FlowExecutionException {
		return StepTransition.NOOP;
	}

	/**
	 * do the work when task generates some result
	 * move the flow to the transitions predefined in the result to transition map,
	 * or to default transition if nothing matches
	 * @param result
	 * @return
	 * @throws FlowExecutionException
	 */
	public synchronized StepTransition executeOnResult(Task task, TaskResult result) throws FlowExecutionException {
		// remember result
		handleResult(task, result);
		return StepTransition.newLog((result.getStatus() + ": " + result.getMsg()), StringUtil.getStackTrace(result.getStackTrace()));
	}

	/**
	 * custom logic to handle result
	 * @param task
	 * @param result
	 * @throws FlowExecutionException
	 */
	protected void handleResult(Task task, TaskResult result) throws FlowExecutionException {
		// override me
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
		return processResults(results);
	}
	
	/**
	 * process all results from callbacks and determine where to go next
	 * override this for custom logic
	 */
	public StepTransition processResults(Map<String, TaskResult> results) {
		StepTransition transition = null;
		TaskResult curRst = null;
		for (Entry<String, TaskResult> entry : results.entrySet()) {
			TaskResult result = entry.getValue();
			TaskResultEnum status = result.getStatus();
			if ((curRst == null || result.isMoreSevere(curRst))) {
				curRst = result;
				if (mapOnResults.containsKey(status)) {
					transition = mapOnResults.get(status).execute();
					transition.log(result.getMsg(), StringUtil.getStackTrace(result.getStackTrace()));
				}
			}
		}
		if (transition != null) {
			return transition;
		} else if (curRst != null && curRst.getStatus().isAnyError()) {
			throw new FlowExecutionException(curRst.getMsg(), curRst.getStackTrace());
		} else {
			return defResult;
		}
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
	 * register a status with result(s)
	 * @param status
	 * @param result
	 */
	public StepCallbackHandler mapResultTo(Enum stepName, TaskResultEnum... statuses) {
		for (TaskResultEnum status : statuses) {
			mapOnResults.put(status, new TransitionWrapper(StepTransition.runToStep(stepName)));
		}
		return this;
	}
	
	
	@SuppressWarnings("unchecked")
	public void merge(StepCallbackHandler another) {
		this.mapOnResults.putAll(another.mapOnResults);
		if (another.defResult != null) {
			this.defResult = another.defResult;
		}
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
	 * register step with results
	 * @param stepOnSuccess
	 * @param stepOnElse
	 * @return
	 */
	public StepCallbackHandler mapResult(Enum stepOnSuccess, Enum stepOnElse) {
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
	public static StepCallbackHandler onResult(Enum stepOnSuccess, Enum stepOnElse) {
		StepCallbackHandler handler = new StepCallbackHandler(StepTransition.runToStep(stepOnElse).withResult(FlowResult.Failed));
		handler.mapResultTo(StepTransition.runToStep(stepOnSuccess), TaskResultEnum.Success);
		return handler;
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

	@Override
	public void setExpectedResultCount(int numOfTasks) {
		this.numOfTasks = numOfTasks;
	}
	
}
