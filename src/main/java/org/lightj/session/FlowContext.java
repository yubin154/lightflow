package org.lightj.session;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.lightj.Constants;
import org.lightj.dal.DataAccessException;
import org.lightj.session.CtxProp.CtxDbType;
import org.lightj.session.CtxProp.CtxSaveType;
import org.lightj.session.dal.ISessionMetaData;
import org.lightj.session.dal.SessionDataFactory;
import org.lightj.session.exception.FlowContextException;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepErrorLog;
import org.lightj.session.step.StepLog;
import org.lightj.session.step.StepLog.TaskLog;
import org.lightj.task.Task;
import org.lightj.task.TaskResult;
import org.lightj.util.JsonUtil;
import org.lightj.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * session manager context, store context for a workflow session
 * support property of String type and Blob type in a name value pair format
 * 
 * @author biyu
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class FlowContext {
	
	static final Logger logger = LoggerFactory.getLogger(FlowContext.class);

	/**
	 * property hash map
	 */
	private LinkedHashMap<String, ISessionMetaData> context = new LinkedHashMap<String, ISessionMetaData>();
	
	/**
	 * field name to ctxprop map
	 */
	private Map<String, CtxPropWrapper> field2Prop = new HashMap<String, CtxPropWrapper>();
	
	/**
	 * scrapbook for temporary context, scrapbook is not persistent
	 */
	private Map<String, Object> scrapBook = new HashMap<String, Object>();
	
	/**
	 * flag for lazy loading
	 */
	private volatile boolean loaded = false;
	
	/**
	 * session id this context associated with
	 */
	private long sessionId;
	
	/** step execution history */
	@CtxProp(dbType=CtxDbType.BLOB, saveType=CtxSaveType.AutoSave)
	private LinkedHashMap<String, StepLog> executionLogs = new LinkedHashMap<String, StepLog>();
	
	/** pct complete */
	@CtxProp(dbType= CtxDbType.VARCHAR, saveType=CtxSaveType.SaveOnChange)
	private int pctComplete;
	
	@CtxProp(dbType= CtxDbType.BLOB, saveType=CtxSaveType.SaveOnChange)
	private Map<String, Object> userData = new LinkedHashMap<String, Object>();
	
	/**
	 * Constructor
	 * @param sm
	 */
	public FlowContext() {
		init();
	}

	/**
	 * initialize the context, check all annotations and decide what to save
	 */
	private final void init() {
		Class klazz = this.getClass();
		do {

			for (Field field : klazz.getDeclaredFields()) {
				try {
					CtxProp prop = field.getAnnotation(CtxProp.class);
					if (prop != null) {
						String name = field.getName();
						Method setter = new PropertyDescriptor(name, klazz).getWriteMethod();
						Method getter = new PropertyDescriptor(name, klazz).getReadMethod();
						if (setter != null && getter != null) {
							CtxPropWrapper wrapper = new CtxPropWrapper(prop, field.getType(), setter, getter);
							field2Prop.put(name, wrapper);
						}
					}
				} catch (Throwable t) {
					logger.error("Failed to init flow context", t);
				}
			}
			
			klazz = klazz.getSuperclass();
			
		} while (klazz != null);
	}
	
	/**
	 * initialize pojo fields with data from persistence 
	 * @param meta
	 */
	public void initField(ISessionMetaData meta) {
		try {
			String propName = meta.getName();
			if (field2Prop.containsKey(propName)) {
				CtxPropWrapper wrapper = field2Prop.get(propName);
				String jsonV = wrapper.ctxProp.dbType() == CtxProp.CtxDbType.BLOB ? meta.getBlobValue().toString() : meta.getStrValue();
				Object objV = JsonUtil.decode(jsonV, wrapper.klazz);
				wrapper.setter.invoke(this, objV);
			}
		} catch (Throwable t) {
			logger.warn("error setting context value", t);
		} 
	}
	
	/**
	 * prepare save to db
	 */
	public void prepareSave() {
		for (Entry<String, CtxPropWrapper> prop : field2Prop.entrySet()) {
			try {
				String propName = prop.getKey();
				CtxPropWrapper wrapper = prop.getValue();
				boolean needSave = false;
				if (wrapper.ctxProp.saveType() == CtxSaveType.NoSave) {
					continue;
				}

				Object newV = prop.getValue().getter.invoke(this, Constants.NO_PARAMETER_VALUES);
				if (wrapper.ctxProp.saveType() == CtxSaveType.AutoSave) {
					needSave = true;
				}
				else {
					Object oldV = exist(propName) ? getMeta(propName, wrapper.klazz, wrapper.ctxProp.dbType()==CtxDbType.BLOB) : null;
					needSave = ( !(newV==oldV || (newV!=null && newV.equals(oldV))) ); 
				}
				if (needSave) {
					setMeta(propName, newV, wrapper.ctxProp.dbType()==CtxDbType.BLOB);
				}
				
			} catch (FlowContextException e) {
				// ignore, logged somewhere else
			} catch (Throwable t) {
				logger.error("Error save context property", t);
			}
		}
	}

	/**
	 * session id
	 * @param sessionId
	 */
	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
		lazyLoad();
	}
	public long getSessionId() {
		return sessionId;
	}
	
	/** loaded */
	void reload() {
		this.loaded = false;
		lazyLoad();
	}
	
	/**
	 * lazy load session context only when called upon
	 */
	private synchronized void lazyLoad() {
		if (!loaded && sessionId > 0) {
			try {
				List<ISessionMetaData> metas = SessionDataFactory.getInstance().getMetaDataManager().findByFlowId(this.sessionId);
				for (ISessionMetaData meta : metas) {
					if (!StringUtil.isNullOrEmpty(meta.getStrValue()) || meta.getBlobValue() != null) {
						context.put(meta.getName(), meta);
						initField(meta);
					}
				}
				loaded = true;
			} catch (DataAccessException e) {
				logger.error("Error loading context", e);
			}
		}
	}
	
	/**
	 * set pojo field into persisted DO
	 * @param name
	 * @param value
	 * @param isBlob
	 * @throws FlowContextException
	 */
	protected void setMeta(String name, Object value, boolean isBlob) throws FlowContextException {
		try {
		
			String jsonV = JsonUtil.encode(value);
			if (context.containsKey(name)) {
				// is update
				if (isBlob) {
					context.get(name).setBlobValue(jsonV);
				}
				else {
					context.get(name).setStrValue(jsonV);
				}			
				context.get(name).setDirty(true);
			}
			else {
				// new meta
				ISessionMetaData meta = SessionDataFactory.getInstance().getMetaDataManager().newInstance();
				// session id is set when it is being saved
				meta.setName(name);
				if (isBlob) {
					meta.setBlobValue(jsonV);
				}
				else {
					meta.setStrValue(jsonV);
				}			
				meta.setDirty(true);
				context.put(name, meta);
			}

		} catch (Throwable t) {
			logger.error("Error save context property", t);
			throw new FlowContextException(t);
		}
	}
	
	/**
	 * get object from persisted DO
	 * @param name
	 * @param klazz
	 * @param isBlob
	 * @return
	 * @throws FlowContextException
	 */
	protected Object getMeta(String name, Class klazz, boolean isBlob) throws FlowContextException {
		String jsonV = null;
		try {
			if (context != null && context.get(name) != null)
			{
				jsonV = isBlob ? (String) context.get(name).getBlobValue() : context.get(name).getStrValue(); 
				return JsonUtil.decode(jsonV, klazz);	
			}
		} catch (Throwable t) {
			logger.error("Error decode context property", t);
			logger.error(String.format("%s, %s, %s", name, klazz, jsonV));
			throw new FlowContextException(t);
		}
		return null;
	}

	/**
	 * get properties changed since last time saved
	 * @return
	 */
	protected List<ISessionMetaData> getDirtyMetas() {
		List<ISessionMetaData> rst = new ArrayList<ISessionMetaData>();
		for (ISessionMetaData meta: context.values()) {
			if (meta.isDirty()) {
				rst.add(meta);
			}
		}
		return rst;
	}
	
	/**
	 * if a property with specified name exists
	 * @param name
	 * @return
	 */
	public boolean exist(String name) {
		return context.containsKey(name) || hasScrapbookKey(name);
	}
	
	/**
	 * property name set
	 * @return
	 */
	public Set<String> getContextNames(){
		return Collections.unmodifiableSet(context.keySet());
	}

	/**
	 * get value by name with reflection
	 * @param name
	 * @return
	 */
	public <C> C getValueByName(String name) {
		try {
			Method getter = new PropertyDescriptor(name, this.getClass()).getReadMethod();
			return (C) getter.invoke(this, Constants.NO_PARAMETER_VALUES);
		} catch (Throwable t) {
			// no getter found, try load from scrapbook
			if (hasScrapbookKey(name)) {
				return (C) getFromScrapbook(name);
			}
			return null;
		}
	}
	
	/**
	 * set value for name
	 * @param name
	 * @return
	 */
	public void setValueForName(String name, Object value) {
		try {
			Method setter = new PropertyDescriptor(name, this.getClass()).getWriteMethod();
			setter.invoke(this, value);
		} catch (Throwable t) {
			// no setter found, save to scrapbook
			this.addToScrapbook(name, value);
		}
	}
	
	/**
	 * non blob type persistent context
	 * @return
	 */
	public HashMap<String, String> getSearchableContext() {
		HashMap<String, String> result = new HashMap<String, String>();
		for (Entry<String, CtxPropWrapper> entry : field2Prop.entrySet()) {
			CtxPropWrapper ctxProp = entry.getValue();
			if (ctxProp.ctxProp.dbType() == CtxProp.CtxDbType.VARCHAR) {
				try {
					Object v = ctxProp.getter.invoke(this, Constants.NO_PARAMETER_VALUES);
					result.put(entry.getKey(), v!=null ? v.toString() : null);
				} catch (Throwable t) {
					logger.error(t.getMessage(), t);
				}
			}
		}
		return result;
	}
	
	
	////////// execution logs //////////
	
	public LinkedHashMap<String, StepLog> getExecutionLogs() {
		return executionLogs;
	}
	
	public List<StepErrorLog> getLastErrors() {
		ArrayList<StepErrorLog> errorLogs = new ArrayList<StepErrorLog>();
		for (Entry<String, StepLog> entry : executionLogs.entrySet()) {
			StepLog stepLog = entry.getValue();
			if (!StringUtil.isNullOrEmpty(stepLog.getStackTrace())) {
				errorLogs.add(0, new StepErrorLog(stepLog.getStepName(), stepLog.getStackTrace()));
			}
			for (Entry<String, TaskLog> taskEntry : stepLog.getTasks().entrySet()) {
				TaskLog taskLog = taskEntry.getValue();
				if (!StringUtil.isNullOrEmpty(taskLog.getStackTrace())) {
					errorLogs.add(0, new StepErrorLog(stepLog.getStepName(), taskLog.getStackTrace()));
				}
			}
		}
		return errorLogs;
	}
	
	public void setExecutionLogs(LinkedHashMap<String, StepLog> executionLogs) {
		if(executionLogs == null || executionLogs.size() == 0) {
			return;
		}
		LinkedHashMap<String, StepLog> _execLogs = new LinkedHashMap<String, StepLog>(executionLogs.size());
		for(String key : executionLogs.keySet()) {
			StepLog o = executionLogs.get(key);
			_execLogs.put(key, o);
		}
		this.executionLogs = _execLogs;
	}

	public void addStep(IFlowStep step) {
		executionLogs.put(step.getStepId(), new StepLog(step.getStepId(), step.getStepName()));
	}
	public void addTask(String stepId, Task task) {
		if (executionLogs.containsKey(stepId)) {
			executionLogs.get(stepId).addTask(task);
		}
	}
	public void saveTaskResult(String stepId, Task task, TaskResult result) {
		if (executionLogs.containsKey(stepId)) {
			executionLogs.get(stepId).updateTaskResult(task, result);
		}
	}
	public void saveFlowError(String stepId, String stackTrace) {
		if (executionLogs.containsKey(stepId)) {
			executionLogs.get(stepId).updateStackTrace(stackTrace);
		}
	}
	public void saveFlowError(String stackTrace) {
		StepLog lastLog = null;
		for (Entry<String, StepLog> entry : executionLogs.entrySet()) {
			lastLog = entry.getValue();
		}
		lastLog.updateStackTrace(stackTrace);
	}
	public void setStepComplete(String stepId) {
		if (executionLogs.containsKey(stepId)) {
			executionLogs.get(stepId).setComplete();
		}
	}
	
	public void addToScrapbook(String key, Object val) {
		scrapBook.put(key, val);
	}
	public Object getFromScrapbook(String key) {
		return scrapBook.containsKey(key) ? scrapBook.get(key) : null;
	}
	public boolean hasScrapbookKey(String key) {
		return scrapBook.containsKey(key);
	}
	
	/////////////// flow pct complete ////////////////
	
	public int getPctComplete() {
		return pctComplete;
	}
	public void setPctComplete(int pctComplete) {
		this.pctComplete = pctComplete;
	}

	////////////// user data ///////////
	public Map<String, Object> getUserData() {
		return Collections.unmodifiableMap(userData);
	}

	public void setUserData(Map<String, Object> userData) {
		this.userData = userData;
		if (userData != null) {
			for (Entry<String, ? extends Object> entry : userData.entrySet()) {
				this.setValueForName(entry.getKey(), entry.getValue());
			}
		}
	}
	
	public void addUserData(String key, Object value) {
		this.userData.put(key, value);
		this.setValueForName(key, value);
	}
	
	public void addUserData(Map<String, ? extends Object> userData) {
		if (userData != null) {
			this.userData.putAll(userData);
			for (Entry<String, ? extends Object> entry : userData.entrySet()) {
				this.setValueForName(entry.getKey(), entry.getValue());
			}
		}
	}
	
	public <T> T getUserData(String key) {
		return (T) userData.get(key);
	}
	
	/**
	 * return required user data and its sample value (if any)
	 * @return
	 */
	public Map<String, String> getUserDataInfo() {
		HashMap<String, String> userDataInfo = new HashMap<String, String>();
		for (Entry<String, CtxPropWrapper> prop : field2Prop.entrySet()) {
			CtxProp ctxProp = prop.getValue().ctxProp;
			if (ctxProp.isUserData()) {
				userDataInfo.put(prop.getKey(), ctxProp.sampleUserDataValue());
			}
		}
		return userDataInfo;
	}

	/////////////// context property wrapper ////////////////

	static class CtxPropWrapper {
		CtxProp ctxProp;
		Class klazz;
		Method setter;
		Method getter;
		public CtxPropWrapper(CtxProp ctxProp, Class klazz, Method setter, Method getter) {
			this.ctxProp = ctxProp;
			this.klazz = klazz;
			this.setter = setter;
			this.getter = getter;
		}
	}

	private String flowKey;

	public String getFlowKey() {
		return flowKey;
	}

	public void setFlowKey(String flowKey) {
		this.flowKey = flowKey;
	}
	
	
}
