package org.lightj.session;

import java.io.InvalidObjectException;
import java.lang.reflect.Method;

import org.lightj.Constants;
import org.lightj.dal.DataAccessException;
import org.lightj.session.dal.SessionDataFactory;
import org.lightj.session.exception.FlowExecutionException;
import org.lightj.session.exception.FlowSaveException;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.SimpleStepExecution;
import org.lightj.session.step.StepCallbackHandler;
import org.lightj.session.step.StepErrorHandler;
import org.lightj.session.step.StepExecution;
import org.lightj.session.step.StepTransition;
import org.lightj.util.AnnotationDefaults;
import org.lightj.util.NetUtil;
import org.lightj.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Driver that drives a {@link FlowSession}
 *
 * @author biyu
 *
 */
@SuppressWarnings({"rawtypes"})
public class FlowDriver implements Runnable {

	/** logger */
	static final Logger logger = LoggerFactory.getLogger(FlowDriver.class);
	
	/**
	 * the session it is going to drive
	 */
	final private FlowSession session;

	/**
	 * current step
	 */
	private IFlowStep currentFlowStep;

	/**
	 * constructor
	 * @param session
	 * @param step
	 */
	public FlowDriver(FlowSession session) {
		this.session = session;
	}
	
	/**
	 * flow event listeners
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private java.util.List<IFlowEventListener> getFlowEventListeners() {
		return session.getFlowEventListeners();
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
	private IFlowStep buildStep(String step) throws FlowExecutionException {
        // getting information about the step
		try {
			Method method = session.getClass().getMethod(step, Constants.NO_PARAMETER_TYPES);
			// find out annotations about the method
			FlowStepProperties currentExecutionProperties = session.getClass().getMethod(step, Constants.NO_PARAMETER_TYPES).getAnnotation(FlowStepProperties.class);
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
	        String nextStep = session.getStepByOffset(step, 1);
	        StepExecution exec = new SimpleStepExecution<FlowContext>(nextStep);
	        StepCallbackHandler chandler = new StepCallbackHandler(nextStep);
	        StepErrorHandler ehandler = null;
	        String onSuccess = currentExecutionProperties.onSuccess();
	        String onElse = currentExecutionProperties.onElse();
	        String onException = currentExecutionProperties.onException();
	        String defErrorStep = session.getErrorStep();
	        
	        if (!StringUtil.isNullOrEmpty(defErrorStep) && validateStepByName(defErrorStep)) {
	        	ehandler = StepErrorHandler.onException(defErrorStep);
	        	chandler.mapResult(nextStep, defErrorStep);
	        }
	        if (!StringUtil.isNullOrEmpty(onSuccess) && !StringUtil.isNullOrEmpty(onElse)
	        		&& validateStepByName(onSuccess) && validateStepByName(onElse)) {
	        	chandler = StepCallbackHandler.onResult(onSuccess, onElse);
	        }
	        if (!StringUtil.isNullOrEmpty(onException) && validateStepByName(onException)) {
	        	ehandler = StepErrorHandler.onException(onException);
	        }
	        currentFlowStep.setIfNull(exec, ehandler, chandler);
	        
	        currentFlowStep.setStepName(step); // step name
	        currentFlowStep.setFlowDriver(this); // this driver
	        currentFlowStep.setSessionContext(session.getSessionContext()); // session context
	        // execution properties from annotation
	        currentFlowStep.setFlowStepProperties(currentExecutionProperties);
	        
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
		// run the flow
		try {
			handleFlowEvent(evt, null);
			drive(new StepTransition()
					.toStep(session.getCurrentAction())
					.inState(session.getState()));
			
		} catch (Throwable t) {
			handleError(t);
			session.killFlow(FlowState.Completed, FlowResult.Fatal, String.format("fail to start flow %s", t.getMessage()));
		}
	}

	/**
	 * drive this flow with a specific transition, used in async flow step callback
	 * @param transition
	 */
	public void driveWithTransition(StepTransition transition) {
		drive(transition);
	}

	/**
	 * drive this flow with a specific error, used in async flow step callback
	 * @param t
	 */
	public void driveWithError(Throwable t) {

		handleError(t);

		StepTransition transition = null;
		try {
			transition = currentFlowStep.onError(t);
		} catch (Throwable t1) {
			String currentStepStr = currentFlowStep.getStepName();
			if (!isErrorStep(currentStepStr)) {
				transition = currentFlowStep.onError(t);
			}
			else {
				transition = new StepTransition(FlowState.Completed, null, FlowResult.Fatal, "Error handling failed: " + t.getMessage());
			}
		}

		drive(transition);
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
	private synchronized void drive(final StepTransition transition) {
		StepTransition t = transition;
		// keep running until we hit a stop/wait
		while (t != null && t.getActionStatus() == FlowState.Running) {
			if (currentFlowStep != null && !StringUtil.equalIgnoreCase(currentFlowStep.getStepName(), t.getNextStep())) {
				// we are exiting the current step, send stepExit event
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
				FlowSessionFactory.getInstance().update(session);
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
					session.getSessionContext().reload();
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
    	
    	boolean isErroStep = isErrorStep(currentStepStr);
        
    	// change related session properties, only for non-error handling step
    	if (!isErroStep) {
    		// current action
            session.setCurrentAction(currentStepStr);
            // next action
            try {
    			session.setNextAction(session.getStepByOffset(currentStepStr, 1));
    		} catch (Throwable t) {
    			// we can't access flow's step, nothing serious, ignore
    		}
    	}

		// call the session implementation step to build the IFlowSetp to be executed
    	try {
			 buildStep(currentStepStr);
			 handleStepEvent(FlowEvent.stepBuild, currentFlowStep, StepTransition.newLog(String.format("%s built", currentStepStr), null));
		} 
    	catch (FlowExecutionException e) {
			// we can't build a valid step implementation, stop the flow
			logger.error(e.getMessage(), e);
			handleError(e);
			return new StepTransition(FlowState.Completed, null, FlowResult.Fatal, e.getMessage());
		}

    	// change related session properties, for non-error handling step only
    	if (!isErroStep) {
            // running state
            session.setState(flowStepTransition.getActionStatus());
            // result state if any
            if (flowStepTransition.getResultStatus() != null) {
            	session.setResult(flowStepTransition.getResultStatus());
            }
    	}

    	StepTransition transition = null;
        try {
            // notify event listeners step entry
            handleStepEvent(FlowEvent.stepEntry, currentFlowStep, flowStepTransition);

            // execute
            transition = currentFlowStep.execute();
            
		} catch (Throwable t) {
	        // notify event listeners error
			handleError(t);
			if (!isErroStep) {
				transition = currentFlowStep.onError(t);
			}
			else {
				transition = new StepTransition(FlowState.Completed, null, FlowResult.Fatal, "Error handling step failed: " + t.getMessage());
			}
		} finally {
			// when flow is running with no new step (waiting for callback), generate ongoing event
			if (transition == null || !transition.isEdge()) {
				handleStepEvent(FlowEvent.stepOngoing, currentFlowStep, transition);
			}
		}
		return transition;
    }

	/** notify registered {@link IFlowEventListener} of flow change event */
	public void handleFlowEvent(FlowEvent event, String msg) {
		for (IFlowEventListener l : getFlowEventListeners()) {
			try {
				l.handleFlowEvent(event, session, msg);
			} catch (Throwable t) {
				logger.warn("Flow event handling exception : " + t.getMessage());
			}
		}
	}

	/** notify registered {@link IFlowEventListener} of flow error */
	public void handleError(Throwable t) {
		for (IFlowEventListener l : getFlowEventListeners()) {
			try {
				l.handleError(t, session);
			} catch (Throwable t1) {
				logger.warn("Flow event handling exception : " + t1.getMessage());
			}
		}
	}

	/** notify registered {@link IFlowEventListener} of flow step event */
	public void handleStepEvent(FlowEvent event, IFlowStep step, StepTransition transition) {
		for (IFlowEventListener l : getFlowEventListeners()) {
			try {
				l.handleStepEvent(event, session, step, transition);
			} catch (Throwable t) {
				logger.warn("Flow event handling exception : " + t.getMessage());
			}
		}
	}
	
	/**
	 * if a step is an error step
	 * @return
	 */
	protected boolean isErrorStep(String stepName) {
		return StringUtil.equalIgnoreCase(session.getErrorStep(), stepName);
	}
	
	/**
	 * validate a step
	 * @param stepName
	 * @return
	 */
	protected boolean validateStepByName(String stepName) {
		try {
			return session.getStepProperties().containsKey(stepName);
		} catch (Throwable e) {
			return false;
		}
	}

	//////////////////// Runnable interface ///////////////////
	/**
	 * drive the session in a separate thread
	 */
	public void run() {
		drive();
	}

}
