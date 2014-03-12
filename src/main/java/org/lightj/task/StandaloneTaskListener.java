package org.lightj.task;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.lightj.session.exception.FlowExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"rawtypes", "unchecked"})
public class StandaloneTaskListener implements ITaskListener {

	/** logger */
	static Logger logger = LoggerFactory.getLogger(StandaloneTaskListener.class.getName());

	/** all results */
	protected ConcurrentMap<String, TaskResult> results = 
			new ConcurrentHashMap<String, TaskResult>();
	
	/** number of tasks for tracking */
	protected AtomicInteger numOfTasks = new AtomicInteger(0);
	protected AtomicInteger numOfTaskResults = new AtomicInteger(0);
	
	/** custom handling logic */
	protected ITaskEventHandler delegateHandler;
	
	/**
	 * constructor with no defaul transition
	 * @param transition
	 */
	public StandaloneTaskListener() {
	}

	public ITaskEventHandler getDelegateHandler() {
		return delegateHandler;
	}

	public void setDelegateHandler(ITaskEventHandler delegateHandler) {
		this.delegateHandler = delegateHandler;
	}

	/**
	 * result handle task completed event
	 */
	public final int handleTaskResult(Task task, TaskResult result) 
	{
		// if this is the last result expected
		int resultRemaining = (task instanceof BatchTask) ? 0 : (numOfTasks.get() - numOfTaskResults.incrementAndGet()); 
		
		try {
			// handle result
			if (task != null && result != null) {
				results.put(task.getTaskId(), result);
			}
			executeOnResult(task, result);
		} catch (Throwable t) {
			logger.error(t.getMessage());
		}

		// all task completed
		if (resultRemaining == 0 && task != null) {
			try {
				executeOnCompleted(task);
			} catch (Throwable t) {
				logger.error(t.getMessage());
			}
		}
		
		// all task completed
		return resultRemaining;

	}

	/**
	 * result handle task submitted event
	 */
	public final void taskSubmitted(Task task) {
		if (task != null) {
			try {
				executeOnSubmitted(task);
			} catch (Throwable t) {
				logger.error(t.getMessage());
			}
		}
	}

	/**
	 * result handle task created event
	 */
	public final void taskCreated(Task task) {
		if (task != null) {
			try {
				executeOnCreated(task);
			} catch (Throwable t) {
				logger.error(t.getMessage());
			}
		}
	}

	/**
	 * do the work when task submitted
	 * @param task
	 * @return
	 * @throws FlowExecutionException
	 */
	public void executeOnSubmitted(Task task) throws FlowExecutionException {
		if (delegateHandler != null) {
			delegateHandler.executeOnSubmitted(null, task);
		}
	}

	/**
	 * do the work when task is created
	 * @param task
	 * @return
	 * @throws FlowExecutionException
	 */
	public void executeOnCreated(Task task) throws FlowExecutionException {
		if (delegateHandler != null) {
			delegateHandler.executeOnCreated(null, task);
		}
	}

	/**
	 * do the work when task generates some result
	 * move the flow to the transitions predefined in the result to transition map,
	 * or to default transition if nothing matches
	 * @param result
	 * @return
	 * @throws FlowExecutionException
	 */
	public synchronized void executeOnResult(Task task, TaskResult result) throws FlowExecutionException {
		if (delegateHandler != null) {
			delegateHandler.executeOnResult(null, task, result);
		}
	}

	/**
	 * do the work when task completed
	 * @param result
	 * @return
	 * @throws FlowExecutionException
	 */
	public synchronized void executeOnCompleted(Task task)
			throws FlowExecutionException 
	{
		if (delegateHandler != null) {
			delegateHandler.executeOnCompleted(null, results);
		}
	}
	
	@Override
	public void setExpectedResultCount(int numOfTasks) {
		this.numOfTasks.addAndGet(numOfTasks);
	}
	
}
