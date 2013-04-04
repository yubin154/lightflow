package org.lightj.session;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;

import org.lightj.RuntimeContext;
import org.lightj.clustering.ClusteringException;
import org.lightj.clustering.ClusteringManager;
import org.lightj.clustering.ClusteringModule;
import org.lightj.dal.BaseDatabaseType;
import org.lightj.dal.ConnectionHelper;
import org.lightj.dal.DatabaseModule;
import org.lightj.dal.HsqlDatabaseType;
import org.lightj.dal.ITransactional;
import org.lightj.initialization.BaseInitializable;
import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;
import org.lightj.locking.ILockManager;
import org.lightj.locking.LockException;
import org.lightj.session.dal.SessionDataFactory;
import org.lightj.session.dal.SessionDataManagerImpl;
import org.lightj.session.dal.SessionMetaDataManagerImpl;
import org.lightj.session.dal.SessionStepLogManagerImpl;
import org.lightj.util.SpringContextUtil;

import com.sun.enterprise.ee.cms.core.GroupManagementService.MemberType;

/**
 * session module
 * 
 * @author biyu
 *
 */
public class FlowModule {

	/** singleton */
	private static SessionModuleInner s_Module = null;
	
	/** constructor */
	public FlowModule() {
		init();
	}
	
	private synchronized void init() {
		if (s_Module == null) s_Module = new SessionModuleInner();
	}

	/** module to be initialized */
	public BaseModule getModule() {
		return s_Module;
	}
	
	public FlowModule setExectuorService(ExecutorService es) {
		s_Module.validateForChange();
		s_Module.es = es;
		return this;
	}
	
	public static ExecutorService getExecutorService() {
		if (s_Module == null) throw new RuntimeException("FlowModule not initialized");
		return s_Module.es;
	}
	
	/** set session database */
	public FlowModule setDb(BaseDatabaseType dbEnum) {
		s_Module.validateForChange();
		s_Module.addDependentModule(new DatabaseModule().addDatabases(dbEnum).getModule());
		s_Module.dbEnum = dbEnum;
		return this;
	}
	
	/** get session database */
	public BaseDatabaseType getDbEnum() {
		return s_Module.dbEnum;
	}
	
	/** enable clustering, session from failed nodes will be recovered by other nodes in the cluster */
	public FlowModule enableCluster(ILockManager lm) {
		return this.enableCluster(lm, 10000, 60000);
	}

	/**
	 * enable clustering with defined min/max delay milliseconds for recover flow 
	 * @param recoverDelayMinMs
	 * @param recoverDelayMaxMs
	 * @return
	 */
	public FlowModule enableCluster(ILockManager semaphore, int recoverDelayMinMs, int recoverDelayMaxMs) {
		s_Module.validateForChange();
		s_Module.addDependentModule(new ClusteringModule().getModule());
		s_Module.clusterEnabled = true;
		s_Module.lockManager = semaphore;
		s_Module.recoverMinDelayMs = recoverDelayMinMs;
		s_Module.recoverMaxDelayMs = recoverDelayMaxMs;
		return this;
	}

	/** cluster enablement */
	public static boolean isClusterEnabled() {
		return s_Module==null ? false : s_Module.clusterEnabled;
	}
	
	/**
	 * global semaphore
	 * @return
	 */
	public static ILockManager getLockManager() {
		if (s_Module == null) throw new RuntimeException("FlowModule not initialized");
		return s_Module.lockManager;
	}
	
	/** load sessions from spring context */
	public FlowModule setSpringContext(String path) {
		s_Module.validateForChange();
		s_Module.springCtxPath = path;
		return this;
	}

	/**
	 * real init
	 * @author biyu
	 *
	 */
	private static class SessionModuleInner extends BaseModule {
		
		/** enable cluster support */
		private boolean clusterEnabled;
		private ILockManager lockManager = new ILockManager() {

			@Override
			public void lock(String targetKey, boolean isExclusive)
					throws LockException {
				throw new LockException("not implemented");
			}

			@Override
			public void unlock(String targetKey) throws LockException {
				throw new LockException("not implemented");
			}

			@Override
			public void synchronizeObject(String semaphoreKey,
					ITransactional context) throws LockException {
				throw new LockException("not implemented");
			}

			@Override
			public int getLockCount(String targetKey) {
				return 0;
			}
			
		};
		private int recoverMinDelayMs;
		private int recoverMaxDelayMs;
		/** database */
		private BaseDatabaseType dbEnum;
		/** spring context */
		private String springCtxPath;
		/** executor service */
		private ExecutorService es;
		
		/**
		 * constructor
		 * @param dbEnum
		 * @param flowTypes
		 */
		private SessionModuleInner() 
		{
			addInitializable(new BaseInitializable() {
				
				@Override
				protected void shutdown() {
					if (dbEnum instanceof HsqlDatabaseType) {
						cleanupMemTables(dbEnum);
					}
				}
				
				@Override
				protected void initialize() 
				{
					if (es == null) {
						throw new InitializationException("session flow requires a executor service");
					}
					
					if (dbEnum == null) {
						throw new InitializationException("session flow requires a database");
					}
					
					SessionDataFactory f = null;
					// load spring context
					if (springCtxPath != null) {
						SpringContextUtil.loadContext("SessionModule", springCtxPath);
						f = (SessionDataFactory) SpringContextUtil.getContext("SessionModule").getBean("sessionDataFactory");
					}
					else {
						f = SessionDataFactory.getInstance();
						f.setDataManager(SessionDataManagerImpl.getInstance());
						f.setMetaDataManager(SessionMetaDataManagerImpl.getInstance());
						f.setStepLogManager(SessionStepLogManagerImpl.getInstance());
					}
					f.setDbEnum(dbEnum);
					
					/** setup in memory db tables */
					if (dbEnum instanceof HsqlDatabaseType) {
						setupMemTables(dbEnum);
					}
					
					// enabled cluster
					if (clusterEnabled) {
						if  (!dbEnum.isShared()) {
							throw new InitializationException("Clustering can only be enabled with shared database"); 
						}
						try {
							ClusteringManager.getInstance().startOrJoin(
									RuntimeContext.getClusterName(), 
									MemberType.CORE, 
									new FlowClusteringEventHandler(recoverMinDelayMs, recoverMaxDelayMs));
						} catch (ClusteringException e) {
							throw new InitializationException(e);
						}
					}
					
					// recover crashed session if any from last shutdown
					FlowSessionFactory.getInstance().recoverMySession();
				}
			});
		}
		
	}
	
	/**
	 * db table creation sql for in memory db
	 */
	static final String createSDSql = "create table FLOW_SESSION ("
			+ "  FLOW_ID        INTEGER primary key," 
			+ "  FLOW_KEY		VARCHAR(255),"
			+ "  CREATION_DATE  DATETIME default sysdate,"
			+ "  END_DATE       DATETIME," 
			+ "  FLOW_STATUS    VARCHAR(255),"
			+ "  TARGET         VARCHAR(1024)," 
			+ "  FLOW_TYPE      VARCHAR(255),"
			+ "  PARENT_ID      INTEGER," 
			+ "  CURRENT_ACTION VARCHAR(64),"
			+ "  NEXT_ACTION    VARCHAR(64)," 
			+ "  FLOW_STATE     VARCHAR(64),"
			+ "  FLOW_RESULT    VARCHAR(64)," 
			+ "  LAST_MODIFIED  DATETIME," 
			+ "  RUN_BY         VARCHAR(128),"
			+ "  REQUESTER      VARCHAR(2000))";

	static final String createSDSeqSql = "create sequence FLOW_ID_SEQ start with 100 increment by 1";

	static final String[] createSDIdxSqls = new String[] {
			"create index SESCOP_PRNT_IDX on FLOW_SESSION (PARENT_ID)",
			"create index FS_ACTIONSTATUS on FLOW_SESSION (FLOW_STATE)",
			"create index FS_CD_IDX on FLOW_SESSION (CREATION_DATE)",
			"create index FS_EI on FLOW_SESSION (END_DATE, FLOW_ID)",
			"create index FS_ENDDATE on FLOW_SESSION (END_DATE)",
			"create index FS_KEY on FLOW_SESSION (TARGET)",
			"create index FS_TYPE_IDX on FLOW_SESSION (FLOW_TYPE)",
			"create index FS_FKEY_IDX on FLOW_SESSION (FLOW_KEY)"
	};

	static final String createSMDSeqSql = "create sequence FLOW_META_ID_SEQ start with 100 increment by 1";

	static final String createSMDSql = "CREATE TABLE FLOW_SESSION_META (	"
			+ "  FLOW_META_ID 	INTEGER PRIMARY KEY,"
			+ "  FLOW_ID 		INTEGER," 
			+ "  NAME		 	VARCHAR(255),"
			+ "  STR_VALUE 		VARCHAR(4000)," 
			+ "  BLOB_VALUE 	BLOB)";

	static final String[] createSMDIdxSql = new String[] { "CREATE INDEX FSM_SSNID_IDX ON FLOW_SESSION_META (FLOW_ID)" };

	static final String createSTLSeqSql = "create sequence FLOW_STEP_ID_SEQ start with 100 increment by 1";

	static final String createSTLSql = "create table FLOW_STEP_LOG ("
			+ "  FLOW_STEP_ID   INTEGER PRIMARY KEY,"
			+ "  STEP_NAME      VARCHAR(128)," 
			+ "  CREATION_TIME  DATETIME,"
			+ "  RESULT         VARCHAR(128),"
			+ "  FLOW_ID	    INTEGER," 
			+ "  DETAILS        VARCHAR(4000))";

	public static void setupMemTables(BaseDatabaseType dbEnum) {
		// setup tables
		try {
			ConnectionHelper.executeUpdate(dbEnum, createSDSeqSql);
			ConnectionHelper.executeUpdate(dbEnum, createSDSql);
			for (String sql : createSDIdxSqls) {
				ConnectionHelper.executeUpdate(dbEnum, sql);
			}
			ConnectionHelper.executeUpdate(dbEnum, createSMDSeqSql);
			ConnectionHelper.executeUpdate(dbEnum, createSMDSql);
			for (String sql : createSMDIdxSql) {
				ConnectionHelper.executeUpdate(dbEnum, sql);
			}
			ConnectionHelper.executeUpdate(dbEnum, createSTLSeqSql);
			ConnectionHelper.executeUpdate(dbEnum, createSTLSql);
		} 
		catch (SQLException e) {
			throw new Error(e);
		}
	}
	
	static final String deleteSDSql = "drop table FLOW_SESSION";
	static final String deleteSDSeqSql = "drop sequence FLOW_ID_SEQ";
	static final String deleteSMDSeqSql = "drop sequence FLOW_META_ID_SEQ";
	static final String deleteSMDSql = "drop TABLE FLOW_SESSION_META";
	static final String deleteSTLSeqSql = "drop sequence FLOW_STEP_ID_SEQ";
	static final String deleteSTLSql = "drop table FLOW_STEP_LOG";

	public static void cleanupMemTables(BaseDatabaseType dbEnum) {
		try {
			ConnectionHelper.executeUpdate(dbEnum, deleteSDSeqSql);
			ConnectionHelper.executeUpdate(dbEnum, deleteSDSql);
			ConnectionHelper.executeUpdate(dbEnum, deleteSMDSeqSql);
			ConnectionHelper.executeUpdate(dbEnum, deleteSMDSql);
			ConnectionHelper.executeUpdate(dbEnum, deleteSTLSeqSql);
			ConnectionHelper.executeUpdate(dbEnum, deleteSTLSql);
		} 
		catch (SQLException e) {
			throw new Error(e);
		}

	}
	
}

