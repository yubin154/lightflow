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
import org.lightj.dal.Locator;
import org.lightj.dal.Query;
import org.lightj.locking.LockException;
import org.lightj.session.dal.ISessionData;
import org.lightj.session.dal.ISessionMetaData;
import org.lightj.session.dal.SessionDataFactory;
import org.lightj.util.ClassUtils;
import org.lightj.util.DBUtil;
import org.lightj.util.NetUtil;
import org.lightj.util.SpringContextUtil;
import org.lightj.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;

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
	static Logger logger = LoggerFactory.getLogger(FlowSessionFactory.class);
	
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
	private static ConcurrentMap<Class, FlowType> flowClassTypes = new ConcurrentHashMap<Class, FlowType>();
	
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
	 * save the session part of the flow, used when persist from {@link FlowDriver}
	 * @param session
	 * @throws DataAccessException
	 */
	void saveSessionData(FlowSession session) throws FlowSaveException 
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
				logger.error(null, e);
			}
		}
	}
	
	/**
	 * get all active sessions with same type, target, and not direct or indirect parent of a session
	 * @param likeMe
	 * @return
	 */
	private List<ISessionData> getActiveSessionsLike(ISessionData likeMe) {
		Object q = SessionDataFactory.getInstance().getDataManager().newQuery();
		if (q instanceof org.lightj.dal.Query) {
			Query query = (Query) q;
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
		}
		else if (q instanceof org.springframework.data.mongodb.core.query.Query) {
			org.springframework.data.mongodb.core.query.Query query = (org.springframework.data.mongodb.core.query.Query) q;
			Criteria criteria = Criteria.where("type").is(likeMe.getType()).and("endDate").is(null);
			if (likeMe.getParentId() > 0) {
				criteria.and("flowId").ne(likeMe.getParentId());
			}
			// don't count myself
			if (likeMe.getFlowId() > 0) {
				criteria.and("flowId").ne(likeMe.getFlowId());
			}
			// on the same component
			if (!StringUtil.isNullOrEmpty(likeMe.getTargetKey())) {
				criteria.and("targetKey").is(likeMe.getTargetKey());
			}
			query.addCriteria(criteria);
		}
		try {
			return SessionDataFactory.getInstance().getDataManager().search(q);
		} catch (DataAccessException e) {
			logger.error(null, e);
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
				saveSessionData(session);
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
							logger.error(null, e);
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
					logger.error(null, e1);
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
			logger.error(null, e);
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
	 * create a new session with a flow class
	 * @param flowKlazz
	 * @return
	 */
	public <T extends FlowSession> T createSession(Class<T> flowKlazz) {
		return SpringContextUtil.getBean(FlowModule.FLOW_CTX, flowKlazz);
	}
	
	/**
	 * create a session from db
	 * @param sessionDo
	 * @return
	 * @throws NoSuchFlowException 
	 * @throws FlowSaveException 
	 */
	public FlowSession createSession(ISessionData sessionDo) {
		FlowType type = fromFlowTypeId(sessionDo.getType());
		if (type != null) {
			FlowSession session = (FlowSession) SpringContextUtil.getBean(FlowModule.FLOW_CTX, type.getFlowKlass());
			// overwrite session DO 
			session.setSessionData(sessionDo);
			return session;
		}
		throw new NoSuchFlowException("Unknown flowtype " + sessionDo.getType());
	}
	
	/**
	 * @see Locator#findByKey(java.lang.String)
	 */
	public FlowSession findByKey(String key) {
		// try it in cache
		FlowSession session = null;
		session = getSessionByKeyFromCache(key);
		if (session!=null) {
			if (!StringUtil.equalIgnoreCase(NetUtil.getMyHostName(), session.getRunBy())) {
				removeSessionFromCache(key);
			}
			return session;
		}
		try {
			ISessionData sessionDo = SessionDataFactory.getInstance().getDataManager().findByKey(key);
			if (sessionDo == null) throw new NoSuchFlowException("Session with key " + key + " is not found or not understood");
			session = createSession(sessionDo);
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
		Object q = SessionDataFactory.getInstance().getDataManager().newQuery();
		if (q instanceof org.lightj.dal.Query) {
			Query query = (Query) q; 
			query.orderBy(" id desc");
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
		}
		else if (q instanceof org.springframework.data.mongodb.core.query.Query) {
			org.springframework.data.mongodb.core.query.Query query = (org.springframework.data.mongodb.core.query.Query) q;
			Criteria criteria = new Criteria();
			if (wfType != null) {
				criteria.and("type").is(wfType.value());
			}
			if (wfState != null) {
				criteria.and("actionStatus").is(wfState.name());
			}
			if (wfStatus != null) {
				criteria.and("resultStatus").is(wfStatus.name());
			}
			if (targetKey != null) {
				criteria.and("targetKey").is(targetKey);
			}
			query.addCriteria(criteria);
		}		
		return getSessionsByQuery(q);
	}

	/**
	 * search for sessions
	 * NOTE: understand the query that is used. This is due to extreme latency in connecting to the database to grab the meta details. 
	 * @param q this query can not use the session_comp with an alias and will not support self joins on itself unless the driving table is not-aliased 
	 * 
	 * @param fullTraversial
	 * @return
	 */
	public List<FlowSession> getSessionsByQuery(Object q) {
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
			logger.error(null, e);
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
				
				Object q = SessionDataFactory.getInstance().getDataManager().newQuery();
				if (q instanceof org.lightj.dal.Query) {
					((Query) q).and("end_date is null")
					.and("run_by", "=", runBy)
					.and("flow_state in (" + 
							DBUtil.dbC(FlowState.Running.name()) + ',' +
							DBUtil.dbC(FlowState.Callback.name())+ ")")
					.and("parent_id is null");
				}
				else if (q instanceof org.springframework.data.mongodb.core.query.Query) {
					((org.springframework.data.mongodb.core.query.Query) q)
						.addCriteria(Criteria.where("endDate").is(null).and("runBy").is(runBy).and("actionStatus").in(FlowState.Running.name(), FlowState.Callback.name()).and("parentId").is(null));
				}

				// first time run since the VM starts, reclaim any of my session from last crash/reboot
				List<FlowSession> sessionsToRecover = FlowSessionFactory.this.getSessionsByQuery(q);
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
				logger.error("Recover session failed", e);
			} catch (LockException e) {
				logger.error("Recover session failed", e);
			}
		} else {
			logger.warn("Flow framework is not cluster safe, possible concurrent recover operation");
			try {
				operation.execute();
			} catch (Throwable e) {
				logger.error("Recover session failed", e);
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
	 * add a new flow class
	 * @param flowKlass
	 */
	public synchronized void addFlowKlazz(Class<? extends FlowSession> flowKlass) {
		try {
			FlowDefinition type = flowKlass.getAnnotation(FlowDefinition.class);
			if (type == null) throw new IllegalArgumentException("No flow definition annontation found");
			Class<? extends FlowContext> ctxKlass = null;
			Type[]  types = ClassUtils.getGenericType(flowKlass);
			for (Type iType : types) {
				for (Type aType : ((ParameterizedType) iType).getActualTypeArguments()) {
					if (aType instanceof Class) {
						ctxKlass = (Class<? extends FlowContext>) aType;
					}
				}
			}
					
			FlowType ft = fromFlowTypeId(type.typeId());
			if (ft == null) {
//				validateSteps();
				ft = new FlowTypeImpl(type.typeId(), type.desc(), flowKlass, ctxKlass, type.group());
				addFlowTypes(Arrays.asList(new FlowType[] {ft}));
			}
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (FlowValidationException e) {
			throw e;
		} catch (Throwable t) {
			throw new IllegalArgumentException(t);
		}
		
	}
	
	/**
	 * add runtime flow types
	 * @param flowTypes
	 */
	public synchronized void addFlowTypes(Collection<? extends FlowType> flowType) {
		for (FlowType type : flowType) {
//			validateSession(type);
			String typeId = type.value();
			if (flowTypes.containsKey(typeId)) {
				FlowType another = flowTypes.get(typeId);
				if (another.getFlowKlass() != type.getFlowKlass()) {
					throw new IllegalArgumentException("Flow type id " + type.value() + " already registered by other flow " + another.desc());
				}
			}
			flowTypes.putIfAbsent(typeId, type);
			flowClassTypes.putIfAbsent(type.getFlowKlass(), type);
		}
	}
	
	/**
	 * calculate and cache type ids
	 */
//	private Integer[] _typeIds;
//	private Integer[] getTypeIds() {
//		if (_typeIds == null) {
//			_typeIds = flowTypes.keySet().toArray(new Integer[0]);
//		}
//		return _typeIds;
//	}
	
	/**
	 * get flow type from its id
	 * @param flowTypeId
	 * @return
	 */
	FlowType fromFlowTypeId(String flowTypeId) {
		return flowTypes.get(flowTypeId);
	}
	
	/**
	 * get flow type from its class
	 * @param flowClass
	 * @return
	 */
	FlowType fromFlowClass(Class flowClass) {
		return flowClassTypes.get(flowClass);
	}
}
