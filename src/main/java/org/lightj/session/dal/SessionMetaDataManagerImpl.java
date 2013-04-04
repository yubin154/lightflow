package org.lightj.session.dal;

import java.sql.Blob;
import java.util.List;

import org.lightj.dal.AbstractDAO;
import org.lightj.dal.BaseSequenceEnum;
import org.lightj.dal.DataAccessException;
import org.lightj.dal.DataAccessRuntimeException;
import org.lightj.dal.Query;

/**
 * {@link ISessionMetaDataManager} implementation
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
public class SessionMetaDataManagerImpl extends AbstractDAO<SessionMetaDataImpl> implements ISessionMetaDataManager<SessionMetaDataImpl> {

	private static SessionMetaDataManagerImpl me = new SessionMetaDataManagerImpl();
	
	public static SessionMetaDataManagerImpl getInstance() {
		return me;
	}
	
	private SessionMetaDataManagerImpl () {
		super();
		String[] colNames = new String[] {"flow_meta_id", "flow_id", "name", "str_value", "blob_value"};
		String[] javaNames = new String[] {"flowMetaId", "flowId", "name", "strValue", "blobValue"};
		Class[] colTypes = new Class[] {long.class, long.class, String.class, String.class, Blob.class};
		this.doKlass = SessionMetaDataImpl.class; 
		try {
			register(SessionMetaDataImpl.class, SessionMetaDataImpl.TABLENAME, null, BaseSequenceEnum.SEQ_FLOW_META_ID, 
			colNames, colTypes, findGetters(javaNames), findSetters(javaNames, colTypes));
		} catch (SecurityException e) {
			cat.error(null, e);
		} catch (NoSuchMethodException e) {
			cat.error(null, e);
		}
	}

	@Override
	public SessionMetaDataImpl newInstance() throws DataAccessRuntimeException {
		try {
			return doKlass.newInstance();
		} catch (IllegalAccessException e) {
			throw new DataAccessRuntimeException(e);
		} catch (InstantiationException e) {
			throw new DataAccessRuntimeException(e);
		}
	}
	
	@Override
	public List<SessionMetaDataImpl> findByFlowId(long sessionId) throws DataAccessException {
		List<SessionMetaDataImpl> smMetaDos = 
			search(new Query().and("flow_id", "=", sessionId)); 
		return smMetaDos;
	}


	static final String[] UNX_COLS = {"flow_id", "NAME"};
	@Override
	public void createOrUpdate(SessionMetaDataImpl data) throws DataAccessException {
		SessionMetaDataImpl existing = new SessionMetaDataImpl();
		initUnique(existing, UNX_COLS, new Object[] {Long.valueOf(data.getFlowId()), data.getName()});
		if (!isNull(Integer.class, existing.getPrimaryKey())) {
			// update
			data.setFlowMetaId(existing.getPrimaryKey());
		}
		save(data);

	}
}
