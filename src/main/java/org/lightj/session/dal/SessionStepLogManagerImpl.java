package org.lightj.session.dal;

import java.util.Date;
import java.util.List;

import org.lightj.dal.AbstractDAO;
import org.lightj.dal.BaseSequenceEnum;
import org.lightj.dal.DataAccessException;
import org.lightj.dal.DataAccessRuntimeException;
import org.lightj.dal.Query;

/**
 * {@link ISessionStepLogManager} implementation
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
public class SessionStepLogManagerImpl extends AbstractDAO<SessionStepLogImpl> implements ISessionStepLogManager<SessionStepLogImpl> {
	
	private static SessionStepLogManagerImpl me = new SessionStepLogManagerImpl();
	
	public static SessionStepLogManagerImpl getInstance() {
		return me;
	}
	
	private SessionStepLogDrainer drainer = null;
	
	private SessionStepLogManagerImpl () {
		super();
		String[] colNames = new String[] {"flow_step_id", "step_name", "creation_time", "result", "flow_id", "details"};
		String[] javaNames = new String[] {"stepId", "stepName", "creationDate", "result", "flowId", "details"};
		Class[] colTypes = new Class[] {long.class, String.class, Date.class, String.class, long.class, String.class};
		this.doKlass = SessionStepLogImpl.class; 
		try {
			register(SessionStepLogImpl.class, SessionStepLogImpl.TABLENAME, 
					null, BaseSequenceEnum.SEQ_FLOW_STEP_ID, colNames, colTypes, 
					findGetters(javaNames), findSetters(javaNames, colTypes));
		} catch (SecurityException e) {
			cat.error(null, e);
		} catch (NoSuchMethodException e) {
			cat.error(null, e);
		}
		// start drainer queue
		drainer = SessionStepLogDrainer.newInstance(this);
		drainer.startQ();
	}

	@Override
	public SessionStepLogImpl newInstance() throws DataAccessRuntimeException {
		try {
			return doKlass.newInstance();
		} catch (IllegalAccessException e) {
			throw new DataAccessRuntimeException(e);
		} catch (InstantiationException e) {
			throw new DataAccessRuntimeException(e);
		}
	}

	@Override
	public void queuedSave(SessionStepLogImpl data) {
		drainer.addData(data);
	}

	@Override
	public List<SessionStepLogImpl> findByFlowId(long flowId)
			throws DataAccessException {
		return search(new Query().and("flow_id", "=", Long.valueOf(flowId)));
	}
	
}
