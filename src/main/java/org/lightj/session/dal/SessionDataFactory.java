package org.lightj.session.dal;

import org.lightj.dal.BaseDatabaseType;


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
	private ISessionDataManager dataManager;
	/** session meta data manager */
	private ISessionMetaDataManager metaDataManager;
	/** session log manager */
	private ISessionStepLogManager stepLogManager;

	private SessionDataFactory() {}
	
	/** get session data manager */
	public final ISessionDataManager getDataManager() {
		return dataManager;
	}
	/** get meta data manager */
	public final ISessionMetaDataManager getMetaDataManager() {
		return metaDataManager;
	}
	/** get log manager */
	public final ISessionStepLogManager getStepLogManager() {
		return stepLogManager;
	}

	public void setDataManager(ISessionDataManager dataManager) {
		this.dataManager = dataManager;
	}

	public void setMetaDataManager(ISessionMetaDataManager metaDataManager) {
		this.metaDataManager = metaDataManager;
	}

	public void setStepLogManager(ISessionStepLogManager stepLogManager) {
		this.stepLogManager = stepLogManager;
	}
	
	public void setDbEnum(BaseDatabaseType dbEnum) {
		this.dataManager.setDbEnum(dbEnum);
		this.metaDataManager.setDbEnum(dbEnum);
		this.stepLogManager.setDbEnum(dbEnum);
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

