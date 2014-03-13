package org.lightj.session.dal;

import org.lightj.dal.BaseDatabaseType;
import org.lightj.dal.mongo.MongoDatabaseType;
import org.lightj.session.dal.mongo.MongoSessionDataManagerImpl;
import org.lightj.session.dal.mongo.MongoSessionMetaDataManagerImpl;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * data factory
 * @author biyu
 *
 * @param <S>
 * @param <M>
 * @param <L>
 */
@SuppressWarnings("rawtypes")
public class SessionDataFactory {
	
	/** session data manager */
	@Autowired
	private ISessionDataManager dataManager;
	/** session meta data manager */
	@Autowired
	private ISessionMetaDataManager metaDataManager;

	private SessionDataFactory() {}
	
	/** get session data manager */
	public final ISessionDataManager getDataManager() {
		return dataManager;
	}
	/** get meta data manager */
	public final ISessionMetaDataManager getMetaDataManager() {
		return metaDataManager;
	}
	public void setDataManager(ISessionDataManager dataManager) {
		this.dataManager = dataManager;
	}

	public void setMetaDataManager(ISessionMetaDataManager metaDataManager) {
		this.metaDataManager = metaDataManager;
	}

	public void setDbEnum(BaseDatabaseType dbEnum) {
		if (dbEnum instanceof MongoDatabaseType) {
			if (dataManager == null) {
				this.dataManager = new org.lightj.session.dal.mongo.MongoSessionDataManagerImpl();
			}
			if (metaDataManager == null) {
				this.metaDataManager = new org.lightj.session.dal.mongo.MongoSessionMetaDataManagerImpl();
				((MongoSessionMetaDataManagerImpl) this.metaDataManager).setSessionDataManager((MongoSessionDataManagerImpl) this.dataManager);
			}
		}
		else {
			if (dataManager == null) {
				this.dataManager = org.lightj.session.dal.rdbms.SessionDataManagerImpl.getInstance();
			}
			if (metaDataManager == null) {
				this.metaDataManager = org.lightj.session.dal.rdbms.SessionMetaDataManagerImpl.getInstance();
			}
		}
		this.dataManager.setDbEnum(dbEnum);
		this.metaDataManager.setDbEnum(dbEnum);
	}

	/**
	 * singleton holder
	 * @author biyu
	 *
	 */
	private static class SessionDataFactoryHolder {
		static SessionDataFactory singleton = new SessionDataFactory();
	}
	
	/**
	 * singleton getter
	 * @return
	 */
	public static SessionDataFactory getInstance() {
		return SessionDataFactoryHolder.singleton;
	}
	
}

