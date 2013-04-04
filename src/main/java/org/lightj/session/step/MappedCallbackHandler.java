package org.lightj.session.step;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lightj.session.FlowContext;
import org.lightj.session.FlowExecutionException;
import org.lightj.session.FlowResult;
import org.lightj.task.Task;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;


/**
 * A convenient call back handler that maps different callback result to different flow transitions
 * the callback happened on each task result generation, ie. on handleTaskResult, the default behavior of
 * taskCompleted() is noop
 * 
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
public class MappedCallbackHandler<T extends FlowContext> extends StepCallbackHandler<T> {
	
	/**
	 * map a result status to a flow step result
	 */
	protected HashMap<TaskResultEnum, StepExecution> mapOnResults = new HashMap<TaskResultEnum, StepExecution>();
	
	/**
	 * Construct a mapped callback handler with default transition
	 * @param defResult
	 */
	public MappedCallbackHandler(StepTransition defResult) {
		super(defResult);
	}
	
	/**
	 * register a status with a result
	 * @param status
	 * @param result
	 */
	public void mapResultTo(TaskResultEnum status, StepTransition result) {
		mapOnResults.put(status, new TransitionWrapper(result));
	}
	
	/**
	 * register statuses with results
	 * @param mapOnResults
	 */
	public void mapResultsTo(HashMap<TaskResultEnum, StepTransition> mapOnResults) {
		for (Map.Entry<TaskResultEnum, StepTransition> entry : mapOnResults.entrySet()) {
			this.mapOnResults.put(entry.getKey(), new TransitionWrapper(entry.getValue()));
		}
	}
	
	/**
	 * register status with an execution
	 * @param status
	 * @param exec
	 */
	public void mapResultTo(TaskResultEnum status, StepExecution exec) {
		mapOnResults.put(status, exec);
	}

	@Override
	public synchronized StepTransition executeOnCompleted(Task task)
			throws FlowExecutionException 
	{
		StepTransition transition = defResult;
		// if we want to move on when result comes back, find the next step
		TaskResult curRst = null;
		for (Entry<String, TaskResult> entry : results.entrySet()) {
			TaskResult result = entry.getValue();
			TaskResultEnum status = result.getStatus();
			if (mapOnResults.containsKey(status) && (curRst == null || result.isMoreSevere(curRst))) {
				curRst = result;
				transition = mapOnResults.get(status).execute();
				transition.log(result.getMsg(), result.getStackTrace());
			}
		}
		return transition;
	}

	/**
	 * log and change session context
	 */
	@Override
	public StepTransition executeOnSubmitted(Task task) throws FlowExecutionException {
		StepTransition transition = StepTransition.newLog(task.getTaskId(), null);
		return transition;
	}

	/**
	 * logging
	 */
	@Override
	public StepTransition executeOnCreated(Task task) throws FlowExecutionException {
		return StepTransition.NOOP;
	}

	/**
	 * move the flow to the transitions predefined in the result to transition map,
	 * or to default transition if nothing matches
	 */
	@Override
	public synchronized StepTransition executeOnResult(Task task, TaskResult result) throws FlowExecutionException {
		// remember result
		return StepTransition.newLog((result.getStatus() + ": " + result.getMsg()), result.getStackTrace());
	}
	
	public MappedCallbackHandler mapResult(String stepOnSuccess, String stepOnElse) {
		this.mapResultTo(TaskResultEnum.Success, StepTransition.runToStep(stepOnSuccess));
		return this;
	}
	
	public MappedCallbackHandler mapResult(Enum stepOnSuccess, Enum stepOnElse) {
		this.mapResultTo(TaskResultEnum.Success, StepTransition.runToStep(stepOnSuccess));
		return this;
	}

	/**
	 * convenient method to create handler
	 * @param stepOnSuccess
	 * @param stepOnElse
	 * @return
	 */
	public static MappedCallbackHandler onResult(String stepOnSuccess, String stepOnElse) {
		MappedCallbackHandler handler = new MappedCallbackHandler(StepTransition.runToStep(stepOnElse).withResult(FlowResult.Failed));
		handler.mapResultTo(TaskResultEnum.Success, StepTransition.runToStep(stepOnSuccess));
		return handler;
	}
	
	/**
	 * convenient method to create handler
	 * @param stepOnSuccess
	 * @param stepOnElse
	 * @return
	 */
	public static MappedCallbackHandler onResult(Enum stepOnSuccess, Enum stepOnElse) {
		MappedCallbackHandler handler = new MappedCallbackHandler(StepTransition.runToStep(stepOnElse).withResult(FlowResult.Failed));
		handler.mapResultTo(TaskResultEnum.Success, StepTransition.runToStep(stepOnSuccess));
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

}
