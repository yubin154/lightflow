package org.lightj.session.dal.rdbms;

import org.lightj.dal.AbstractDbDrainer;
import org.lightj.session.dal.ISessionStepLog;

/**
 * asynchronous queue based drainer to drain {@link ISessionStepLog} to data store
 * @author biyu
 *
 */
public class SessionStepLogDrainer extends AbstractDbDrainer<SessionStepLogManagerImpl> {
	
	private SessionStepLogDrainer(SessionStepLogManagerImpl dao) {
		super(dao, 1 * 1000L);
	}

	/**
	 * create new instance of the drainer
	 * @param mngr
	 * @return
	 */
	static SessionStepLogDrainer newInstance(SessionStepLogManagerImpl mngr) {
		return new SessionStepLogDrainer(mngr);
	}
}
