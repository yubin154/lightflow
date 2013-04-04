package org.lightj.session;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.lightj.dal.DataAccessException;
import org.lightj.dal.ITransactional;
import org.lightj.dal.Locatable;
import org.lightj.dal.Locator;
import org.lightj.dal.Query;
import org.lightj.locking.LockException;
import org.lightj.session.dal.ISessionData;
import org.lightj.session.dal.ISessionMetaData;
import org.lightj.session.dal.SessionDataFactory;
import org.lightj.util.ClassUtils;
import org.lightj.util.DBUtil;
import org.lightj.util.Log4jProxy;
import org.lightj.util.NetUtil;
import org.lightj.util.StringUtil;

/**
 * Business object factory for flow session
 * 
 * @author biyu
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class FlowSessionFactory implements Locator<FlowSession> {
	
	/**
	 * logger
	 */
	static Log4jProxy cat = Log4jProxy.getInstance(FlowSessionFactory.class);
	
	/**
	 * a cache for all the actively running session managers in this VM
	 */
	private static ConcurrentMap<String, FlowSession> smCache = new ConcurrentHashMap<String, FlowSession>();
	
	/** 
	 * a lock to synchronize new session creation 
	 */
	private final ReentrantLock creationLock = new ReentrantLock(); 

	/**
	 * a cache of all type value to {@link FlowType}
	 */
	private static ConcurrentMap<String, FlowType> flowTypes = new ConcurrentHashMap<String, FlowType>();
	
	/**
	 * singleton
	 */
	private static FlowSessionFactory me = null;
	
	/**
	 * get singleton instance
	 * @return
	 */
	public static synchronized final FlowSessionFactory getInstance() {
		if (me == null) {
			me = new FlowSessionFactory();
		}
		return me;
	}
	
	/**
	 * private constructor
	 *
	 */
	private FlowSessionFactory() {}

	/**
	 * validate all flow sessions, its construction and its steps
	 * @throws FlowValidationException
	 */
	public void validateSession(FlowType flowType) throws FlowValidationException {
		Class<? extends FlowSession> flowKlass = flowType.getFlowKlass();
		if (flowKlass != null) {
			try {
				// make sure flow can be constructed
				FlowSession session = flowKlass.newInstance();
				// make sure flow context can be constructed 
				flowType.getCtxKlass().newInstance();
				// validate steps
				session.validateSteps();
			} 
			catch (Throwable t) {
				throw new FlowValidationException(t);
			}
		}
		else {
			throw new FlowValidationException("Flow type doesn't associate with flow class " + flowType.value());
		}
	}

	/**
	 * save the session part of the flow, used when persist from {@link FlowDriver}
	 * @param session
	 * @throws DataAccessException
	 */
	public void saveSession(FlowSession session) throws FlowSaveException 
	{
		ISessionData managerDO = session.getSessionData();
		managerDO.setLastModified(new Date());
		try {
			SessionDataFactory.getInstance().getDataManager().save(managerDO);
		} catch (DataAccessException e) {
			throw new FlowSaveException(e);
		}
	}
	
	/**
	 * save session meta, used when persist from {@link FlowDriver}
	 * @param manager
	 */
	public void saveMeta(FlowSession manager) {
		FlowContext ctx = manager.getSessionContext();
		for (ISessionMetaData managerMeta : ctx.getDirtyMetas()) {
			if (managerMeta.getFlowId() <= 0) managerMeta.setFlowId(ctx.getSessionId());
			try {
				SessionDataFactory.getInstance().getMetaDataManager().save(managerMeta);
				managerMeta.setDirty(false);
			} catch (DataAccessException e) {
				cat.error(null, e);
			}
		}
	}
	
	/**
	 * get all active sessions with same type, target, and not direct or indirect parent of a session
	 * @param likeMe
	 * @return
	 */
	private List<ISessionData> getActiveSessionsLike(ISessionData likeMe) {
		Query query = new Query();
		// only care about the same type
		query.and("flow_type", "=", likeMe.getType());
		// don't count my parent
		if (likeMe.getParentId() > 0) {
			query.and("flow_id", "!=", likeMe.getParentId());
		}
		// don't count myself
		if (likeMe.getFlowId() > 0) {
			query.and("flow_id", "!=", likeMe.getFlowId());
		}
		// on the same component
		if (!StringUtil.isNullOrEmpty(likeMe.getTargetKey())) {
			query.and("target", "=", likeMe.getTargetKey());
		}
		// open session
		query.and("END_DATE IS NULL");
		try {
			return SessionDataFactory.getInstance().getDataManager().search(query);
		} catch (DataAccessException e) {
			cat.error(null, e);
			return Collections.emptyList();
		}

	}
	
	/**
	 * save everything, session manager, meta
	 * used when persist the session from {@link IFlowControl} interface
	 * @param session
	 * @throws Exception
	 */
	public void save(FlowSession session) throws FlowSaveException {
		synchronized (session) 
		{
			boolean isUpdate = (session.isSaved());
			// we serialize session creation 
			if (!isUpdate) {
				creationLock.lock();
			}
			try {
				// before save
				session.beforeSave(isUpdate);
				// save session
				saveSession(session);
				// optimistic locking
				if(session.getFlowProperties().lockTarget() && !isUpdate && session.getEndDate() == null) 
				{
					List<ISessionData> activeSession = getActiveSessionsLike(session.getSessionData()); 
					int activeSessionSize = activeSession.size();
					//ensure that the ID here matches the ID of the only active session, this will be optimistic locking.
					if (activeSessionSize > 0){
						//remove the current session as some session has won already.
						try {
							SessionDataFactory.getInstance().getDataManager().delete(session.getSessionData());
						} catch (DataAccessException e) {
							cat.error(null, e);
						}
						long existingSessionId = activeSession.get(0).getFlowId();
						FlowExistException e = new FlowExistException("Unable to create flow on "
								+ session.getKeyOfTarget() + ". Active flow exists flowid : " + existingSessionId);
						e.setExistSessionId(Long.toString(existingSessionId));
						throw e;
					}
				}
			}
			finally {
				if (!isUpdate) {
					creationLock.unlock();
				}
			}
			
			try {
				// after save
				session.afterSave(isUpdate);
			} catch (FlowSaveException e) {
				try {
					SessionDataFactory.getInstance().getDataManager().delete(session.getSessionData());
				} catch (DataAccessException e1) {
					cat.error(null, e1);
				}
				throw e;
			}
			// lazy save meta
			saveMeta(session);
			// Bin on 7/26/10 : add it to cache if this VM saves the session itself, chances are if it saved the session, it will run/use the session soon 
			if (!isUpdate) {
				// everything is fine, now add it to sm cache
				smCache.putIfAbsent(session.getKey(), session);
			}
		}
	}
	
	/**
	 * update a session, = saveSupressException
	 * @param session
	 */
	public void update(FlowSession session) {
		try {
			save(session);
		} catch (FlowSaveException e) {
			// shouldn't have never reach here
			cat.error(null, e);
		}
	}
	
	/**
	 * get session from cache, null if not present in cache
	 * @param id
	 * @return
	 */
	FlowSession getSessionByKeyFromCache(String key) {
		return smCache.get(key);
	}
	
	/**
	 * add a session to cache
	 * @param session
	 */
	synchronized void addToCache(FlowSession session) {
		smCache.putIfAbsent(session.getKey(), session);
	}
	
	/**
	 * 
	 * @param flowKlass
	 * @param target
	 * @param requester
	 * @return
	 * @throws FlowSaveException 
	 */
	public <A extends FlowSession> A createSession(Class<A> flowKlass, Locatable target, Locatable requester)
	{
		FlowDefinition type = flowKlass.getAnnotation(FlowDefinition.class);
		
		if (type == null) throw new IllegalArgumentException("No flow definition annontation found");
		
		try {
			Class<? extends FlowContext> ctxKlass = null;
			Type[]  types = ClassUtils.getGenericType(flowKlass);
			for (Type iType : types) {
				for (Type aType : ((ParameterizedType) iType).getActualTypeArguments()) {
					if (aType instanceof Class) {
						ctxKlass = (Class<? extends FlowContext>) aType;
					}
				}
			}
					
			FlowType ft = null;
			if (!flowTypes.containsKey(type.typeId())) 
			{
				ft = new FlowTypeImpl(type.typeId(), type.desc(), flowKlass, ctxKlass, type.group());
				addFlowTypes(Arrays.asList(new FlowType[] {ft}));
			}
			else {
				ft = flowTypes.get(type.typeId());
				if (ft.getFlowKlass() != flowKlass) {
					throw new IllegalAccessException("Duplicate flow type id " + type.typeId() + ", " 
							+ ft.getFlowKlass().getName() + " already registered with the id");
				}
			}

			A session = flowKlass.newInstance();
			session.setRequester(requester);
			session.setTarget(target);
			session.setFlowType(ft);
			session.sessionContext = ctxKlass.newInstance();
			return session;
		} catch (Throwable t) {
			// we shouldn't have reached here if we do the validation up front
			cat.error(null, t);
			throw new IllegalArgumentException(t);
		}
	}

	/**
	 * create a session from db
	 * @param sessionDo
	 * @return
	 * @throws FlowSaveException 
	 */
	public FlowSession createSession(ISessionData sessionDo) {
		FlowType type = fromFlowTypeId(sessionDo.getType());
		if (type != null) {
			FlowSession session = createSession(type.getFlowKlass(), null, null);
			// overwrite session DO 
			session.setSessionData(sessionDo);
			return session;
		}
		return null;
	}
	
	/**
	 * @see Locator#findByKey(java.lang.String)
	 */
	public FlowSession findByKey(String key) throws NoSuchFlowException {
		// try it in cache
		FlowSession s = getSessionByKeyFromCache(key);
		if (s!=null) {
			if (!StringUtil.equalIgnoreCase(NetUtil.getMyHostName(), s.getRunBy())) {
				removeSessionFromCache(key);
			}
			return s;
		}
		// cache misses, resort to lower level cache in DAO layer
		try {
			ISessionData sessionDo = SessionDataFactory.getInstance().getDataManager().findByKey(key);
			FlowSession session = createSession(sessionDo);
			if (session == null) throw new NoSuchFlowException("Session with key " + key + " is not found or not understood");
			session.loadExtra();
			return session;
		} catch (DataAccessException e) {
			throw new NoSuchFlowException(e.getMessage());
		}
	}

	/**
	 * search for sessions
	 * @param wfType
	 * @param wfState
	 * @param wfStatus
	 * @param targetKey
	 * @return
	 */
	public List<FlowSession> getSessions(FlowType wfType, FlowState wfState, FlowResult wfStatus, String targetKey) 
	{
		Query query = new Query().orderBy(" id desc");
		if (wfType != null) {
			query.and("flow_type", "=", wfType.value());
		}
		if (wfState != null) {
			query.and("flow_state", "=", wfState.name());
		}
		if (wfStatus != null) {
			query.and("flow_result", "=", wfStatus.name());
		}
		if (targetKey != null) {
			query.and("target", "=", targetKey);
		}
		return getSessionsByQuery(query);
	}

	/**
	 * search for sessions
	 * NOTE: understand the query that is used. This is due to extreme latency in connecting to the database to grab the meta details. 
	 * @param q this query can not use the session_comp with an alias and will not support self joins on itself unless the driving table is not-aliased 
	 * 
	 * @param fullTraversial
	 * @return
	 */
	public List<FlowSession> getSessionsByQuery(Query q) {
		Integer[] typeIds = getTypeIds();
		if (typeIds != null && typeIds.length > 0) {
			q.and("flow_type in (" + StringUtil.join(typeIds, ",") + ")");
		}
		List<FlowSession> sessions = new ArrayList<FlowSession>();
		try {
			List<ISessionData> sessionDos  = SessionDataFactory.getInstance().getDataManager().search(q);
			for (ISessionData sessionDo : sessionDos){
				FlowSession session = null;
				if (smCache.containsKey(sessionDo.getFlowKey())) {
					// exist in cache
					session = getSessionByKeyFromCache(sessionDo.getFlowKey());
				}
				else {
					session = createSession(sessionDo);
					if (session == null) {
						continue;
					}
					session.loadExtra();
				}
				sessions.add(session);
			} 		
		}
		catch (DataAccessException e) {
			cat.error(null, e);
		}
 		return sessions;
 	}
	
	/**
	 * remove a session from cache, when the session is completed
	 * @param key
	 */
	public synchronized void removeSessionFromCache(String key) {
		smCache.remove(key);
	}
	
	/**
	 * recover sessions from runBy
	 * @param lockManager
	 * @param runBy
	 */
	private void recoverSession(final String runBy) {
		
		ITransactional operation = new ITransactional() {

			public void execute() throws Throwable {

				// first time run since the VM starts, reclaim any of my session from last crash/reboot
				List<FlowSession> sessionsToRecover = FlowSessionFactory.this.getSessionsByQuery(
						new Query().and("end_date is null")
							.and("run_by", "=", runBy)
							.and("flow_state in (" + 
									DBUtil.dbC(FlowState.Running.name()) + ',' +
									DBUtil.dbC(FlowState.Callback.name())+ ")")
							.and("parent_id is null")
				);
				for (FlowSession session : sessionsToRecover) {
					if (session.getFlowProperties().clustered()) {
						session.recoverFromCrash();
					}
				}
				
			}};
			
		if (FlowModule.isClusterEnabled()) {
			try {
				FlowModule.getLockManager().synchronizeObject(FlowSessionFactory.class.getName(), operation);
			} catch (RuntimeException e) {
				cat.error("Recover session failed", e);
			} catch (LockException e) {
				cat.error("Recover session failed", e);
			}
		} else {
			cat.warn("Flow framework is not cluster safe, possible concurrent recover operation");
			try {
				operation.execute();
			} catch (Throwable e) {
				cat.error("Recover session failed", e);
			}
		}

	}
	
	/**
	 * recover my session from last shutdown
	 */
	synchronized void recoverMySession() {
		recoverSession(NetUtil.getMyHostName());
	}
	
	/**
	 * recover crashed session by a host
	 *
	 */
	public synchronized void recoverCrashedSession(final String runBy) {

		// recover needs global locking
		if (FlowModule.isClusterEnabled()) {
			recoverSession(runBy);
		}
	}
	
	/**
	 * add runtime flow types
	 * @param flowTypes
	 */
	public void addFlowTypes(Collection<? extends FlowType> flowType) {
		for (FlowType type : flowType) {
			validateSession(type);
			String typeId = type.value();
			if (flowTypes.containsKey(typeId)) {
				FlowType another = flowTypes.get(typeId);
				if (another.getFlowKlass() != type.getFlowKlass()) {
					throw new IllegalArgumentException("Flow type id " + type.value() + " already registered by other flow " + another.desc());
				}
			}
			flowTypes.put(typeId, type);
		}
	}
	
	/**
	 * calculate and cache type ids
	 */
	private Integer[] _typeIds;
	private Integer[] getTypeIds() {
		if (_typeIds == null) {
			_typeIds = flowTypes.keySet().toArray(new Integer[0]);
		}
		return _typeIds;
	}
	
	/**
	 * get flow type from its id
	 * @param flowTypeId
	 * @return
	 */
	FlowType fromFlowTypeId(String flowTypeId) {
		return flowTypes.get(flowTypeId);
	}

}
