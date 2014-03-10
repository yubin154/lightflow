package org.lightj.session;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.lightj.session.dal.ISessionData;
import org.lightj.session.dal.SessionDataFactory;
import org.lightj.session.eventlistener.FlowRecoverEventListener;
import org.lightj.session.eventlistener.FlowSaver;
import org.lightj.session.eventlistener.FlowStatsTracker;
import org.lightj.session.eventlistener.FlowTimer;
import org.lightj.session.exception.FlowExecutionException;
import org.lightj.session.exception.FlowSaveException;
import org.lightj.session.exception.FlowValidationException;
import org.lightj.session.exception.StateChangeException;
import org.lightj.session.step.StepLog;
import org.lightj.util.AnnotationDefaults;
import org.lightj.util.DateUtil;
import org.lightj.util.NetUtil;
import org.lightj.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * base flow session class
 * 
 * @author binyu
 *
 * @param <T>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class FlowSession<T extends FlowContext> implements IFlowControl
{

	/** logger */
	private static Logger logger = LoggerFactory.getLogger(FlowSession.class);

	/** date format used in default flow key */
	private static String date_fmt = "yyyy-MM-dd";
	
	/** flow type */
	protected FlowType _type;

	/** session manager */
	protected ISessionData sessionDo;

	/** session context */
	protected T sessionContext;

	/** flow driver */
	protected FlowDriver driver;
	
	/** run time flow event listener */
	protected List<IFlowEventListener> flowEventListeners = new ArrayList<IFlowEventListener>();
	
	/** ordered step properties */
	protected final LinkedHashMap<String, FlowStepProperties> stepProperties;
	protected final List<StepPropTuple> orderedStepProperties;
	
	public LinkedHashMap<String, FlowStepProperties> getStepProperties() {
		return stepProperties;
	}
	public List<StepPropTuple> getOrderedStepProperties() {
		return orderedStepProperties;
	}
	public String getStepByOffset(String current, int offset) {
		for (int idx = 0; idx < orderedStepProperties.size(); idx++) {
			if (orderedStepProperties.get(idx).name.equalsIgnoreCase(current)) {
				int nIdx = idx + offset;
				if (nIdx < 0 || nIdx >= orderedStepProperties.size()) {
					return null;
				}
				return orderedStepProperties.get(nIdx).name;
			}
		}
		throw new FlowExecutionException(String.format("no step of %s is found", current));
	}
	public String getErrorStep() {
		for (Entry<String, FlowStepProperties> entry : stepProperties.entrySet()) {
			if (entry.getValue().isErrorStep()) {
				return entry.getKey();
			}
		}
		return null;
	}
	public String getFirstStep() {
		return orderedStepProperties.get(0).name;
	}
	
	
	/**
	 * Constructor
	 * 
	 * @param firstStep
	 * @param type
	 */
	public FlowSession() {
		sessionDo = SessionDataFactory.getInstance().getDataManager().newInstance();
		sessionDo.setCreationDate(new Date());
		sessionDo.setFlowState(FlowState.Pending);
		
		// populate flowsteps
		this.stepProperties = new LinkedHashMap<String, FlowStepProperties>();
		this.orderedStepProperties = new ArrayList<StepPropTuple>();
		for (Method stepMethod : this.getClass().getMethods()) {
			FlowStepProperties sp = stepMethod.getAnnotation(FlowStepProperties.class);
			if (sp != null) {
				orderedStepProperties.add(new StepPropTuple(stepMethod.getName(), sp));
			}
		}
		Collections.sort(orderedStepProperties, new Comparator<StepPropTuple>() {

			@Override
			public int compare(StepPropTuple o1, StepPropTuple o2) {
				return o1.prop.isFirstStep() ? -1 : o1.prop.isErrorStep() ? 1 : o1.prop.stepIdx() - o2.prop.stepIdx();
			}
			
		});
		for (StepPropTuple tuple : orderedStepProperties) {
			this.stepProperties.put(tuple.name, tuple.prop);
		}

		sessionDo.setCurrentAction(this.getFirstStep());
		setRequester(StringUtil.genUuid());
		setTarget(StringUtil.genUuid());
		
		// add the system default listener, no choice of removing
		flowEventListeners.add(new FlowSaver());
		flowEventListeners.add(new FlowTimer());
		flowEventListeners.add(new FlowStatsTracker());
//		flowEventListeners.add(new FlowRecoverEventListener());

		postConstruct();
	}
	
	/**
	 * post construct a flow
	 * @param session
	 */
	private final void postConstruct() {
		try {
			FlowType ft = FlowSessionFactory.getInstance().fromFlowClass(this.getClass());
			setFlowType(ft);
			sessionContext = (T) ft.getCtxKlass().newInstance();
		} catch (IllegalAccessException e) {
			throw new FlowValidationException(e);
		} catch (InstantiationException e) {
			throw new FlowValidationException(e);
		}
	}
	
	////////////////// proxy to managerDO ////////////////////
	
	public ISessionData getSessionData() {
		return sessionDo;
	}
	protected void setSessionData(ISessionData sessionDo) {
		this.sessionDo = sessionDo;
		sessionContext.setSessionId(sessionDo.getFlowId());
		sessionContext.setFlowKey(getKey());
	}
	
	/**
	 * display result status if session is completed, otherwise action status label
	 * 
	 * @return
	 */
	public String getStatusDesc() {
		return getState() == FlowState.Completed ? 
				getResult().getUserStatus() : getState().getLabel();
	}

	public final long getId() {
		return sessionDo.getFlowId();
	}
	public final boolean isSaved() {
		return getId() > 0;
	}

	public void setRequester(String requester) {
		sessionDo.setRequesterKey(requester);
	}
	/** Load requester object from request locatable string */
	public String getRequester() {
		return sessionDo.getRequesterKey();
	}
	
	public Date getCreationDate() {
		return sessionDo.getCreationDate();
	}
	public Date getEndDate() {
		return sessionDo.getEndDate();
	}
	
	public void setTarget(String target) {
		sessionDo.setTargetKey(target);
	}
	public String getTarget() {
		return sessionDo.getTargetKey();
	}

	void setFlowType(FlowType ft) {
		this._type = ft;
		sessionDo.setType(ft.value());
		sessionDo.setFlowKey(generateFlowKey());
	}
	public FlowType getFlowType() {
		return _type;
	}
	public String getTypeDesc() {
		return getFlowType().desc();
	}

	public FlowState getState() {
		return sessionDo.getFlowState();
	}
	protected void setState(FlowState actionStatus) {
		sessionDo.setFlowState(actionStatus);
	}

	public String getCurrentAction() {
		return sessionDo.getCurrentAction();
	}
	public void setCurrentAction(String currentAction) {
		sessionDo.setCurrentAction(currentAction);
	}
	
	public String getNextAction() {
		return sessionDo.getNextAction();
	}
	protected void setNextAction(String nextAction) {
		sessionDo.setNextAction(nextAction);
	}
	
	public FlowResult getResult() {
		return sessionDo.getFlowResult();
	}
	protected void setResult(FlowResult resultStatus) {
		sessionDo.setFlowResult(resultStatus);
	}
	
	public Date getLastModified() {
		return sessionDo.getLastModified();
	}
	public String getRunBy() {
		return sessionDo.getRunBy();
	}
	protected void setRunBy(String runBy) {
		sessionDo.setRunBy(runBy);
	}
	
	public String getStatus() {
		return sessionDo.getStatus();
	}
	protected void setStatus(String status) {
		sessionDo.setStatus(status);
	}
	
	public long getParentId() {
		return this.sessionDo.getParentId();
	}
	public void setParentId(long id) {
		this.sessionDo.setParentId(id);
	}

	/////////////////////// Flow Control ///////////////////////

	public T getSessionContext() {
		return sessionContext;
	}

	/**
	 * here is a chance to load extra stuffs after the session is loaded
	 *
	 */
	public void loadExtra() {
	}

	/**
	 * here is chance to do extra stuffs before the session is saved
	 *  this in future will be a place to do MMI check to make sure
	 *  1. entitlement
	 *  2. locking
	 *  3. runtime governing policy
	 *  4. proper locking if everything passed
	 *  
	 * @param isUpdate
	 * @throws FlowSaveException
	 */
	protected void beforeSave(boolean isUpdate)	throws FlowSaveException {
	}
	
	/**
	 * here is a chance to do extra stuffs after session is saved
	 * @param isUpdate
	 * @throws FlowSaveException
	 */
	protected void afterSave(boolean isUpdate) throws FlowSaveException {
		if (!isUpdate) {
			if (sessionContext.getSessionId() <= 0) {
				sessionContext.setSessionId(this.getId());
				sessionContext.setFlowKey(this.getKey());
			}
		}
	}

	/**
	 * validate a state transition
	 * 1. change from stopped state to non-stopped state is exception
	 * 2. change from running state to running state is exception
	 * 3. change from stopped state to stopped state is warning, no exception
	 * 
	 * @param fromState
	 * @param toState
	 * @throws StateChangeException
	 */
	protected void validateStateTransition(FlowState fromState,
			FlowState toState) throws StateChangeException {
		if (fromState.isComplete() && !(toState.isComplete())) {
			throw new StateChangeException(
					"Can't transition from complete state to noncomplete state, "
							+ "FromState=" + fromState.getLabel() 
							+ ", ToState=" + toState.getLabel());
		} 
		else if (fromState.isRunning() && toState.isRunning()) {
			throw new StateChangeException(
					"The session is already running "
							+ "FromState=" + fromState.getLabel() 
							+ ", ToState=" + toState.getLabel());
		}
		else if (fromState.isComplete() && toState.isComplete()) {
			logger.warn(
					"Try to transition from complete state to complete state, "
							+ "FromState=" + fromState.getLabel()
							+ ", ToState=" + toState.getLabel());
		}
	}
	
	///////////////////// Locatable Interface /////////////////////

	/**
	 * SessionManagerFactory uses this to instantiate session manager, don't
	 * override!
	 * 
	 */
	public final String getKey() {
		return sessionDo.getFlowKey();
	}

	// //////////////////// IFlowControl Interface //////////////////////
	/**
	 * run flow, start a flow from the current step, the following session can be started safely:
	 * 1. the session has not been run yet (runby field is empty), or is specified cluster safe in session property {@link FlowProperties#interruptible()}
	 * 2. the session is not already in a running {@link FlowState#isRunning()} or complete {@link FlowState#isComplete()} state
	 */
	public final synchronized void runFlow() throws StateChangeException {
		try {
			validate();
		} catch (FlowSaveException e1) {
			throw new StateChangeException(e1.getMessage());
		}
		if (this.isSaved()) {
			FlowSession sessionFromCache = FlowSessionFactory.getInstance().getSessionByKeyFromCache(this.getKey());
			// not in cache
			if (sessionFromCache == null) {
				FlowSessionFactory.getInstance().addToCache(this);
			}
			// cached object is different, use cached object instead
			else if (sessionFromCache != this) {
				sessionFromCache.runFlow();
				return;
			}
		}
		else {
			// session has to be persisted before it can be runned, so different set of exceptions
			// ConcurrentComponentInteractionException can be handled properly
			throw new StateChangeException("Session has to be persisted before run");
		}
		// validate state transition
		validateStateTransition(getState(), FlowState.Running);
		// a started session, all only cluster safe session or the VM runs the session initially
		if (this.getRunBy() != null && !StringUtil.equalIgnoreCase(NetUtil.getMyHostName(), this.getRunBy())) {
			if (!getFlowProperties().interruptible()) {
				throw new StateChangeException("The session is not run by this VM " + this.getRunBy() + 
				", or cannot change state safely by another node in the cluster");
			}
		}
		// create driver if not created already
		if (driver == null) {
			createFlowDriver();
		}
		// we persist the session ALWAYS when we run it
		try {
			FlowSessionFactory.getInstance().save(this);
		} catch (FlowSaveException e) {
			throw new StateChangeException(e);
		}
		FlowModule.getExecutorService().submit(driver);
	}
	
	/**
	 * create a new flow driver for this session
	 * @return
	 */
	FlowDriver createFlowDriver() {
		driver = new FlowDriver(this);
		return driver;
	}

	/**
	 * pause a flow
	 */
	public final synchronized void pauseFlow(FlowResult resultStatus, String message)
			throws StateChangeException {
		stopFlow(FlowState.Paused, resultStatus, message);
	}

	/**
	 * stop a flow, stop a flow, this could mean either stop the flow completely, or put it in waiting state
	 * 
	 * I. stop the flow completely, the following flows can be stopped safely
	 * 	1. A flow that hasn't been stopped already, a stopped flow can still be stopped without giving {@link StateChangeException}
	 * 
	 * II. put the flow in a waiting state, the following flows can be changed safely
	 *  1. A flow that is running from this VM, or has a cluster safe flow property {@link FlowProperties#interruptible()}
	 *  2. A flow taht hasn't been stopped already, try to change a stopped flow to waiting state will throw {@link StateChangeException}
	 */
	public final synchronized void stopFlow(FlowState actionStatus, FlowResult resultStatus, String message) 
		throws StateChangeException
	{
		// actionstatus can't be null
		if (actionStatus == null) {
			throw new StateChangeException("action status cannot be empty");
		}
		// actionstatus can't be a running state
		if (actionStatus.isRunning()) {
			throw new StateChangeException("Cannot run a flow by calling stopFlow " + actionStatus.getLabel());
		}
		// validate state transition
		validateStateTransition(getState(), actionStatus);
		// destination is waiting state, only allow current VM (runby field) or cluster safe session to be changed
		if (actionStatus.isWaiting()  
				&& !StringUtil.equalIgnoreCase(NetUtil.getMyHostName(), this.getRunBy())
				&& !getFlowProperties().interruptible()) {
			throw new StateChangeException("The session is not run by this VM " + this.getRunBy() + 
				", or cannot change state safely by another node in the cluster");
		}
		// make sure we are getting the correct session object from factory cache if run from the same VM
		if (this.isSaved()) {
			FlowSession sessionFromCache = FlowSessionFactory.getInstance().getSessionByKeyFromCache(this.getKey());
			// cached object is different, use cached object instead
			if (sessionFromCache != null && sessionFromCache != this) {
				sessionFromCache.stopFlow(actionStatus, resultStatus, message);
				return;
			}
		}
		try {
			this.setState(actionStatus);
			FlowResult currRs = getResult();
			boolean needSaveRs = (resultStatus != null && 
					(currRs == null || resultStatus.logLevel().intValue() >= currRs.logLevel().intValue()));

			if (needSaveRs) {
				this.setResult(resultStatus);
				if (message != null) {
					this.sessionDo.setStatus(message);
				}
			}
			if (actionStatus.isComplete()) {
				sessionDo.setEndDate(new Date());
				// wipe out next actin field
				this.setNextAction(null);
			}
			// we persist the session ALWAYS when we "stop" it
			try {
				FlowSessionFactory.getInstance().save(this);
			} catch (FlowSaveException e) {
				throw new StateChangeException(e);
			}
		} finally {
			if (actionStatus.isComplete()) {
				cleanup();
			}
			final FlowEvent event = actionStatus.isComplete() ? FlowEvent.stop : FlowEvent.pause;
			if (driver != null) {
				driver.handleFlowEvent(event, message);
			}
		}
	}
	
	/**
	 * kill a flow, only used by the framework, stop a flow silently, w/o {@link StateChangeException}
	 * @param actionStatus
	 * @param resultStatus
	 * @param message
	 */
	final synchronized public void killFlow(FlowState actionStatus, FlowResult resultStatus, String message) {
		// actionstatus can't be null
		if (actionStatus == null) {
			throw new IllegalArgumentException("Action status cannot be empty");
		}
		// actionstatus can't be a non stopped state
		if (!actionStatus.isComplete()) throw new IllegalArgumentException("Cannot kill a flow by pass in a non-stopped state " + actionStatus.getLabel());
		// make sure we are getting the correct session object from factory cache if run from the same VM
		if (this.isSaved()) {
			FlowSession sessionFromCache = FlowSessionFactory.getInstance().getSessionByKeyFromCache(this.getKey());
			// cached object is different, use cached object instead
			if (sessionFromCache != null && sessionFromCache != this) {
				sessionFromCache.killFlow(actionStatus, resultStatus, message);
				return;
			}
		}
		try {
			this.setState(actionStatus);
			if (actionStatus.isComplete()) {
				sessionDo.setEndDate(new Date());
				// wipe out next action field
				this.setNextAction(null);
			}
			FlowResult currRs = getResult();
			boolean needSaveRs = (resultStatus != null && 
					(currRs == null || resultStatus.logLevel().intValue() >= currRs.logLevel().intValue()));

			if (needSaveRs) {
				this.setResult(resultStatus);
				if (message != null) {
					this.sessionDo.setStatus(message);
				}
			}
			// we persist the session ALWAYS when we "stop" it
			FlowSessionFactory.getInstance().update(this);
		}
		finally {
			cleanup();
			if (driver != null) {
				driver.handleFlowEvent(FlowEvent.stop, message);
			}
		}
	}
	
	/**
	 * validate a session is in a runnable state
	 * @throws FlowSaveException 
	 */
	protected final void validate() throws FlowSaveException {
		if (sessionDo == null) throw new FlowSaveException("Session not initialized");
		if (sessionDo.getRequesterKey() == null) throw new FlowSaveException("Session requester cannot be empty");
		if (sessionDo.getTargetKey() == null) throw new FlowSaveException("Session target cannot be empty");
	}

	/**
	 * clean up after flow 
	 */
	protected void cleanup() {
		// wipe out next step
		setNextAction(null);
		// stop all non-completed child sessions if any
		Object q = SessionDataFactory.getInstance().getDataManager().queryIncompleteChildFlows(this.getId());
		for (FlowSession child : FlowSessionFactory.getInstance().getSessionsByQuery(q)) {
			child.killFlow(this.getState(), this.getResult(), "Parent was stopped");
		}
		// remove it from factory cache
		FlowSessionFactory.getInstance().removeSessionFromCache(this.getKey());
	}

	/////////////////// IRuntimeFlowProperties interface ////////////////////
	
	
	/**
	 * get flow property annotation for this session
	 * 
	 * @return
	 */
	public FlowProperties getFlowProperties() {
		FlowProperties fp = this.getClass().getAnnotation(FlowProperties.class);
		return fp == null ? AnnotationDefaults.of(FlowProperties.class) : fp;
	}
	
	/** get flow event listeners */
	public java.util.List<IFlowEventListener> getFlowEventListeners() {
		return flowEventListeners;
	}

	/** add runtime event listener */
	public void addEventListener(IFlowEventListener listener) {
		flowEventListeners.add(listener);
	}
	
	/** remove a listener of a class type */
	public synchronized void removeEventListenerOfType(Class listenerKlass) {
		for (Iterator<IFlowEventListener> iter = flowEventListeners.iterator(); iter.hasNext(); ) {
			IFlowEventListener l = iter.next();
			if (l.getClass() == listenerKlass) {
				iter.remove();
			}
		}
	}
	
	/**
	 * recover a crashed session from a running state 
	 *
	 */
	public void recoverFromCrash() {
		String msg = null;
		try {
			// first cancel all non complete child flows
			Object q = SessionDataFactory.getInstance().getDataManager().queryActiveChildFlows(this.getId());
			List<FlowSession> children = FlowSessionFactory.getInstance().getSessionsByQuery(q);
			for (FlowSession child : children) {
				child.killFlow(FlowState.Canceled, FlowResult.Failed, "Parent recover from an unexpected stop");
			}
		} catch (Throwable t) {
			logger.error("cancel child sessions failed", t);
		}

		FlowEvent evt = null;
		try {
			driver = this.createFlowDriver();
			this.addEventListener(new FlowRecoverEventListener());
			
			if (this.getFlowProperties().clustered()) {
				evt = FlowEvent.recover;
				this.setState(FlowState.Paused);
				msg = "Session recovered from an unexpected stop";
				this.getSessionData().setStatus(msg);
				this.runFlow();
			}
			else {
				evt = FlowEvent.recoverNot;
				msg = "Non-crashsafe session not recoverable from an unexpected stop";
			}
		} catch (Throwable t) {
			logger.error("Failed to recover session from an unexpected stop ", t);
			msg = String.format("Failed to recover session from an unexpected stop: %s", t.getMessage());
			evt = FlowEvent.recoverFailure;
			driver.handleFlowEvent(FlowEvent.recoverFailure, t.getMessage());
		} finally {
			driver.handleFlowEvent(evt, msg);
		}
	}
	
	/** percent complete */
	public int getPercentComplete() {
		if (this.getEndDate() != null) return 100;
		return getSessionContext().getPctComplete();
	}
	
	/** execution logs */
	public LinkedHashMap<String, StepLog> getExecutionLogs() {
		return sessionContext.getExecutionLogs();
	}
	
	/**
	 * generate default flow key, override in subclass for something different
	 * @return
	 */
	protected String generateFlowKey() {
		return String.format("%s|%s|%s", DateUtil.format(new Date(), date_fmt), _type.value(), UUID.randomUUID().toString());
	}
	
	/**
	 * flow info
	 * @return
	 */
	public FlowInfo getFlowInfo() {
		FlowInfo flowInfo = new FlowInfo();
		flowInfo.setFlowKey(getKey());
		flowInfo.setCreateDate(getCreationDate());
		flowInfo.setCurrentStep(getCurrentAction());
		flowInfo.setEndDate(getEndDate());
		flowInfo.setExecutionLogs(sessionContext.getExecutionLogs());
		flowInfo.setFlowResult(getResult().getUserStatus());
		flowInfo.setFlowState(getState().getLabel());
		flowInfo.setFlowStatus(getStatus());
		flowInfo.setFlowType(getFlowType().value());
		flowInfo.setProgress(String.format("%s", getPercentComplete()));
		flowInfo.setRequester(getRequester());
		flowInfo.setTarget(getTarget());
		flowInfo.setFlowContext(sessionContext.getSearchableContext());
		return flowInfo;
	}
	
	/**
	 * save flow
	 * @throws FlowSaveException
	 */
	public void save() throws FlowSaveException {
		FlowSessionFactory.getInstance().save(this);
	}
	
	/**
	 * step prop
	 * @author binyu
	 *
	 */
	public static class StepPropTuple {
		public final String name;
		public final FlowStepProperties prop;
		StepPropTuple(String name, FlowStepProperties prop) {
			this.name = name;
			this.prop = prop;
		}
	}
}
