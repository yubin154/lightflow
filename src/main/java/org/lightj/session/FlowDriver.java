package org.lightj.session;

import java.io.InvalidObjectException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import org.lightj.Constants;
import org.lightj.dal.DataAccessException;
import org.lightj.locking.LockException;
import org.lightj.session.dal.SessionDataFactory;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.MappedCallbackHandler;
import org.lightj.session.step.SimpleStepExecution;
import org.lightj.session.step.StepCallbackHandler;
import org.lightj.session.step.StepErrorHandler;
import org.lightj.session.step.StepExecution;
import org.lightj.session.step.StepImpl;
import org.lightj.session.step.StepStatistics;
import org.lightj.session.step.StepTransition;
import org.lightj.util.AnnotationDefaults;
import org.lightj.util.Log4jProxy;
import org.lightj.util.NetUtil;
import org.lightj.util.StringUtil;


/**
 * Driver that drives a {@link FlowSession}
 *
 * @author biyu
 *
 */
@SuppressWarnings({"rawtypes"})
public class FlowDriver implements Runnable, IQueueTask {

	/** logger */
	static final Log4jProxy logger = Log4jProxy.getInstance(FlowDriver.class);
	
	/**
	 * the session it is going to drive
	 */
	private FlowSession session;

	/**
	 * session class
	 */
	private Class<? extends FlowSession> type;

	/**
	 * current step
	 */
	private IFlowStep currentFlowStep;

	/**
	 * flow event listener
	 */
	private HashMap<Class, IFlowEventListener> eventListeners;

	/**
	 * constructor
	 * @param session
	 * @param step
	 */
	public FlowDriver(FlowSession session) {
		this.session = session;
		this.type = session.getClass();
		this.eventListeners = new HashMap<Class, IFlowEventListener>();
	}
	
	/**
	 * what's the current flow step
	 * @return
	 */
	public IFlowStep getCurrentFlowStep() {
		return currentFlowStep;
	}
	
	/**
	 * build flowstep implementation for the current step
	 * @param step
	 * @return
	 * @throws InvalidObjectException
	 */
	public IFlowStep buildStep(String step) throws FlowExecutionException {
        // getting information about the step
		try {
			Method method = type.getMethod(step, Constants.NO_PARAMETER_TYPES);
			// find out annotations about the method
			FlowStepProperties currentExecutionProperties = session.getFirstStepEnum().getDeclaringClass().getField(step).getAnnotation(FlowStepProperties.class);
			if (currentExecutionProperties == null) {
				currentExecutionProperties = AnnotationDefaults.of(FlowStepProperties.class);
			}			
			// execute the method, it will build the step to be runned
	        Object flowStep = method.invoke(session, Constants.NO_PARAMETER_VALUES);
	        if(flowStep == null || !(flowStep instanceof IFlowStep)) {
	        	// if we have an answer, it has to be a IFlowStep
	        	throw new FlowExecutionException("Empty or invalid step implementation to execute");
	        }
	        currentFlowStep = (IFlowStep) flowStep;
	        
	        // set default from properties
	        StepExecution exec = null;
	        StepCallbackHandler chandler = null;
	        StepErrorHandler ehandler = null;
	        String onSuccess = currentExecutionProperties.onSuccess();
	        String onElse = currentExecutionProperties.onElse();
	        String onException = currentExecutionProperties.onException();
	        String defErrorStep = session.getFlowProperties().errorStep();
	        
	        if (!StringUtil.isNullOrEmpty(defErrorStep) && session.validateStepByName(defErrorStep)) {
	        	ehandler = StepErrorHandler.onException(defErrorStep);
	        }
	        if (!StringUtil.isNullOrEmpty(onSuccess) && session.validateStepByName(onSuccess)) {
	        	exec = new SimpleStepExecution(onSuccess);
	        }
	        if (!StringUtil.isNullOrEmpty(onSuccess) && !StringUtil.isNullOrEmpty(onElse)
	        		&& session.validateStepByName(onSuccess) && session.validateStepByName(onElse)) {
	        	chandler = MappedCallbackHandler.onResult(onSuccess, onElse);
	        }
	        if (!StringUtil.isNullOrEmpty(onException) && session.validateStepByName(onException)) {
	        	ehandler = StepErrorHandler.onException(onException);
	        }
	        currentFlowStep.setIfNull(exec, ehandler, chandler, ehandler);
	        
	        currentFlowStep.setStepName(step); // step name
	        currentFlowStep.setFlowDriver(this); // this driver
	        currentFlowStep.setSessionContext(session.getSessionContext()); // session context
	        // execution properties from annotation
	        currentFlowStep.getStepOptions().setPredefinedProperties(
	        		currentExecutionProperties.desc(),
	        		currentExecutionProperties.stepWeight(), 
	        		currentExecutionProperties.logging(),
	        		currentExecutionProperties.timeoutMillisec()); 
	        currentFlowStep.setStatistics(new StepStatistics()); // statistics
	        
	        return currentFlowStep;
	        
		} catch (FlowExecutionException e) {
			throw e;
		} catch (Throwable t) {
			throw new FlowExecutionException("Failed to build step " + t.getClass().getName() + " : " + t.getMessage(), t);
		}
	}

	/**
	 * drive a session to as far as possible from beginning
	 */
	public void drive() {
		String myHostName = NetUtil.getMyHostName();
		String runBy = session.getRunBy();
		boolean isStart = (runBy == null);
		boolean isRunByDiff = StringUtil.equalIgnoreCase(myHostName, runBy);
		FlowEvent evt = isStart ? FlowEvent.start : FlowEvent.resume;
		if (isStart || isRunByDiff) {
			session.setRunBy(myHostName);
		}
		session.setState(FlowState.Running);
		session.setResult(FlowResult.InProgress);
		try {
			FlowSessionFactory.getInstance().save(session);
		} catch (FlowSaveException e) {
			logger.error("Error update session", e);
			throw new FlowExecutionException("Error update session", e);
		}
		// lock target(s)
		try {
			session.acquireLock();
		} catch (LockException e1) {
			handleError(e1);
		}
		// initialize flow stats
		session.getStats();
		handleFlowEvent(evt);
		try {
			drive(new StepTransition()
					.toStep(session.getCurrentAction())
					.inState(session.getState()));
		} catch (StateChangeException e) {
			handleError(e);
		}
	}

	/**
	 * this happens when the callback comes back,
	 * where we need to resume the session from parking state
	 *
	 */
	public void callback() {
		// take the current step and execute its callback handler
		StepTransition transition = null;
		try {
			transition = currentFlowStep.onResult();
			drive(transition);
		}
		catch (Throwable t) {
			// we always persist history when failure happens
			handleError(t);
			transition = currentFlowStep.onResultError(t);
		}
	}

	/**
	 * drive this flow with a specific transition, used in synchronous scheduled flow step execution
	 * @param transition
	 */
	public void driveWithTransition(StepTransition transition) {
		try {
			drive(transition);
		}
		catch (Throwable t) {
			// we always persist history when failure happens
			handleError(t);
			transition = currentFlowStep.onResultError(t);
		}
	}

	/**
	 * drive a session to as far as possible from a step
	 * possible values of {@link StepTransition} and their meaning:
	 *
	 * actionstatus, nextstep, resultstatus, msg	: validate value combinations
	 *
	 * 1. running, nextstep, *, * 	: run next step
	 * 2. callback, null, *, * 		: park flow, waiting for callback
	 * 3. null, null, *, *			: don't change flow state, just change result or do logging
	 * 4. completed/canceled/skipped, null, *, *		: stop flow, close session
	 * 5. waiting/retry/suspended, nextstep, *, *		: park flow at a step, waiting for external action
	 * 6. null						: callback
	 *
	 * @param stepStr
	 */
	private synchronized void drive(final StepTransition transition) throws StateChangeException {
		StepTransition t = transition;
		// keep running until we hit a stop/wait
		while (t != null && t.getActionStatus() == FlowState.Running) {
			if (currentFlowStep != null && !StringUtil.equalIgnoreCase(currentFlowStep.getStepName(), t.getNextStep())) {
				// we are exiting the current step, send stepExit event
				currentFlowStep.getStatistics().setEndTime(new Date());
				handleStepEvent(FlowEvent.stepExit, currentFlowStep, t);
			}
			// run the next step
			t = execute(t);
		}
		// just quit if we get a quit marker, means some other VM has taken over the flow,
		// shouldn't have happened anyway because we don't allow a VM to run a flow actively running by a separate VM
		if (t == StepTransition.NOOP) {
			return;
		}
		// null transition maps to callback WfActionStatus.Callback
		if (t == null) {
			t = StepTransition.CALLBACK;
		}
		// next step to run changed
		if (t.getNextStep() != null && !StringUtil.equalIgnoreCase(session.getCurrentAction(), t.getNextStep())) {
			session.setCurrentAction(t.getNextStep());
		}
		if (t.getActionStatus() != null) {
			if (!t.getActionStatus().isRunning()) {
				// we ALWAYS want to persist if the session switched from running to non running
				// event will be dispatched in stopFlow
				handleStepEvent(FlowEvent.stepExit, currentFlowStep, t);
				session.stopFlow(t.getActionStatus(), t.getResultStatus(), (t.getMsg()==null ? "Session " + t.getActionStatus() : t.getMsg()));
			}
			else {
				// when flow is running with no new step (waiting for callback), generate ongoing event
				handleStepEvent(FlowEvent.stepOngoing, currentFlowStep, transition);
				session.setState(t.getActionStatus());
				try {
					FlowSessionFactory.getInstance().saveSession(session);
				} catch (FlowSaveException e) {
					throw new StateChangeException(e);
				}
			}
		}
		else {
			// when flow is running with no new step (waiting for callback), generate ongoing event
			handleStepEvent(FlowEvent.stepOngoing, currentFlowStep, transition);
		}
	}

	/**
	 * execute a step,
	 * 1. reload session from db if property says so
	 * 2. make sure the session is in Running state
	 * 3. make sure the this VM has right to run it (runBy field in session is null or the hostname)
	 * 4. run the step
	 * 5. based on return type and value, run the next step or put the session in Callback state
	 * @param stepEnum
	 */
    private StepTransition execute(StepTransition flowStepTransition) {
    	// if we get in here, the flow state must be running and we have a valid step to execute
    	String currentStepStr = flowStepTransition.getNextStep();

    	// check db state if session property say cluster safe
    	if (session.getFlowProperties().interruptible()) {
			try {
				FlowSession sessionFromDb = FlowSessionFactory.getInstance().createSession(
						SessionDataFactory.getInstance().getDataManager().findById(session.getId()));
				if (sessionFromDb!=null && sessionFromDb.getLastModified() != null && session.getLastModified() != null &&
						sessionFromDb.getLastModified().after(session.getLastModified())) {
					if (!StringUtil.equalIgnoreCase(sessionFromDb.getRunBy(), session.getRunBy())) {
						// someone has already took over the session, just quit
						return StepTransition.NOOP;
					}
					else if (!sessionFromDb.getState().isRunning()) {
		        		// someone has changed the flow state to something else but not yet taken over the session
		    			// use the db values as result
		    			return new StepTransition(sessionFromDb.getState(), sessionFromDb.getCurrentAction(),
		    					sessionFromDb.getResult(), "Flow state changed externally");
		    		}
					// force reload context
					session.getSessionContext().setLoaded(false);
				}
			} catch (DataAccessException e) {

				handleError(e);
				logger.error(null, e);
				return new StepTransition(FlowState.Completed, null, FlowResult.Fatal,
						"Failed to query database for flow info : " + e.getMessage());
			}
    	}
    	else if (session.getState().isComplete()){
    		// session is marked as complete from external, quit
    		return StepTransition.NOOP;
    	}
        
    	// start timer timer for the step
    	Date stepStartTime = new Date();
    	
    	// change related session properties
		// current action
        session.setCurrentAction(currentStepStr);
        // next action
        try {
			session.setNextAction(session.getRelateEnumValue(session.getEnumByName(currentStepStr), 1).name());
		} catch (Throwable t) {
			// we can't access flow's step enum, nothing serious, ignore
		}

		// call the session implementation step to build the IFlowSetp to be executed
    	IFlowStep flowStep = null;
        
    	try {
			 flowStep = buildStep(currentStepStr);
		} catch (FlowExecutionException e) {
			// we can't build a valid step implementation, stop the flow
			StepImpl dummy = new StepImpl();
			dummy.setStepName(currentStepStr);
	        handleStepEvent(FlowEvent.stepBuild, dummy, StepTransition.newLog(e.getMessage(), StringUtil.getStackTrace(e, 2000)));
			logger.error(e.getMessage(), e);
			handleError(e);
			return new StepTransition(FlowState.Completed, null, FlowResult.Fatal, e.getMessage());
		}

    	// change related session properties
        // running state
        session.setState(flowStepTransition.getActionStatus());
        // result state if any
        if (flowStepTransition.getResultStatus() != null) {
        	session.setResult(flowStepTransition.getResultStatus());
        }
        flowStep.getStatistics().setStartTime(stepStartTime);
        
        // notify event listeners step entry
        handleStepEvent(FlowEvent.stepEntry, flowStep, flowStepTransition);
        StepTransition transition = null;
        try {
        	currentFlowStep.setTransitionFrom(flowStepTransition);
			transition = currentFlowStep.execute();
		} catch (Throwable t) {
	        // notify event listeners error
			handleError(t);
			transition = currentFlowStep.onExecutionError(t);
		} finally {
			// when flow is running with no new step (waiting for callback), generate ongoing event
			if (transition == null || !transition.isEdge()) {
				handleStepEvent(FlowEvent.stepOngoing, currentFlowStep, transition);
			}
		}
		return transition;
    }

	/**
	 * add flow event listener
	 * @param listener
	 */
	synchronized void addFlowEventListener(IFlowEventListener listener) {
		eventListeners.put(listener.getClass(), listener);
	}

	/**
	 * remove a listener of a class type
	 * @param listenerKlass
	 */
	synchronized void removeFlowEventListenerOfType(Class listenerKlass) {
		eventListeners.remove(listenerKlass);
	}

	/** notify registered {@link IFlowEventListener} of flow change event */
	public void handleFlowEvent(FlowEvent event) {
		for (Entry<Class, IFlowEventListener> l : eventListeners.entrySet()) {
			try {
				l.getValue().handleFlowEvent(event, session);
			} catch (Throwable t) {
				logger.warn("Flow event handling exception : " + t.getMessage());
			}
		}
	}

	/** notify registered {@link IFlowEventListener} of flow error */
	public void handleError(Throwable t) {
		for (Entry<Class, IFlowEventListener> l : eventListeners.entrySet()) {
			try {
				l.getValue().handleError(t, session);
			} catch (Throwable t1) {
				logger.warn("Flow event handling exception : " + t1.getMessage());
			}
		}
	}

	/** notify registered {@link IFlowEventListener} of flow step event */
	public void handleStepEvent(FlowEvent event, IFlowStep step, StepTransition transition) {
		for (Entry<Class, IFlowEventListener> l : eventListeners.entrySet()) {
			try {
				l.getValue().handleStepEvent(event, session, step, transition);
			} catch (Throwable t) {
				logger.warn("Flow event handling exception : " + t.getMessage());
			}
		}
	}

	//////////////////// Runnable interface ///////////////////
	/**
	 * drive the session in a separate thread
	 */
	public void run() {
		drive();
	}

	@Override
	public String getName() {
		return session.getKey();
	}

	@Override
	public String getGroup() {
		return session.getTaskGroup();
	}

	@Override
	public int getPriority() {
		return session.getPriority();
	}

}
