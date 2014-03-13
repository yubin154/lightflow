package org.lightj.session;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.lightj.clustering.ClusteringException;
import org.lightj.clustering.ClusteringModule;
import org.lightj.dal.BaseDatabaseType;
import org.lightj.dal.ConnectionHelper;
import org.lightj.dal.DatabaseModule;
import org.lightj.dal.HsqlDatabaseType;
import org.lightj.initialization.BaseInitializable;
import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;
import org.lightj.session.dal.SessionDataFactory;
import org.lightj.task.TaskModule;
import org.lightj.util.SpringContextUtil;
import org.springframework.context.ApplicationContext;

import com.sun.enterprise.ee.cms.core.GroupManagementService.MemberType;

/**
 * flow module, used for light flow framework initialization
 * 
 * @author biyu
 *
 */
public class FlowModule {

	/** singleton */
	private static FlowModuleInner s_Module = null;
	
	/** key */
	public static final String FLOW_CTX = "CTX_FLOW_MODULE";
	
	/** constructor */
	public FlowModule() {
		init();
	}
	
	private synchronized void init() {
		if (s_Module == null) s_Module = new FlowModuleInner();
	}

	/** module to be initialized */
	public BaseModule getModule() {
		return s_Module;
	}

	private static final void validateInit() {
		if (s_Module == null) {
			throw new InitializationException("FlowModule not initialized");
		}
	}

	public FlowModule setExectuorService(ExecutorService es) {
		s_Module.validateForChange();
		s_Module.es = es;
		return this;
	}
	
	public static ExecutorService getExecutorService() {
		validateInit();
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
	public static BaseDatabaseType getDbEnum() {
		validateInit();
		return s_Module.dbEnum;
	}
	
	/** add flows from spring context */
	public FlowModule setSpringContext(ApplicationContext flowCtx) {
		s_Module.validateForChange();
		s_Module.flowCtx = flowCtx;
		return this;
	}

	/**
	 * enable clustering with defined min/max delay milliseconds for recover flow 
	 * @param recoverDelayMinMs
	 * @param recoverDelayMaxMs
	 * @return
	 */
	public FlowModule enableCluster() {
		s_Module.validateForChange();
		s_Module.addDependentModule(new ClusteringModule().getModule());
		s_Module.clusterEnabled = true;
		return this;
	}

	/** cluster enablement */
	public static boolean isClusterEnabled() {
		validateInit();
		return s_Module.clusterEnabled;
	}
	
	/**
	 * real init
	 * @author biyu
	 *
	 */
	private static class FlowModuleInner extends BaseModule {
		
		/** enable cluster support */
		private boolean clusterEnabled;
		/** database */
		private BaseDatabaseType dbEnum;
		/** executor service */
		private ExecutorService es;
		/** spring context */
		private ApplicationContext flowCtx;

		
		/**
		 * constructor
		 * @param dbEnum
		 * @param flowTypes
		 */
		private FlowModuleInner() 
		{
			addInitializable(new BaseInitializable() {
				
				@Override
				protected void shutdown() {
					if (dbEnum instanceof HsqlDatabaseType) {
						cleanupMemTables(dbEnum);
					}
					clusterEnabled = false;
					dbEnum = null;
					es = null;
					flowCtx = null;
				}
				
				@Override
				@SuppressWarnings("rawtypes")
				protected void initialize() 
				{
					if (es == null) {
						throw new InitializationException("session flow requires a executor service");
					}
					
					if (dbEnum == null) {
						throw new InitializationException("session flow requires a database");
					}
					
					// load spring context
					if (flowCtx == null) {
						throw new InitializationException("session flow application context not set");
					}
					
					SessionDataFactory f = SessionDataFactory.getInstance();
					f.setDbEnum(dbEnum);

					SpringContextUtil.registerContext(FLOW_CTX, flowCtx);
					
//					f = (SessionDataFactory) SpringContextUtil.getBean(FLOW_CTX, "sessionDataFactory");

					// load registered flow bean classes
					List<Class<? extends FlowSession>> flowTypes = SpringContextUtil.getBeansClass(FLOW_CTX, FlowSession.class);
					for (Class<? extends FlowSession> flowType : flowTypes) {
						FlowSessionFactory.getInstance().addFlowKlazz(flowType);
					}

					/** setup in memory db tables */
					if (dbEnum instanceof HsqlDatabaseType) {
						setupMemTables(dbEnum);
					}
					
					// enabled cluster
					if (clusterEnabled) {
						try {
							ClusteringModule.startOrJoin(
									ClusteringModule.getClusterName(), 
									MemberType.CORE, 
									new FlowClusteringEventHandler());
						} catch (ClusteringException e) {
							throw new InitializationException(e);
						}
					}
					
					// recover crashed session if any from last shutdown
					FlowSessionFactory.getInstance().recoverMySession();
				}
			});
			
			addDependentModule(new TaskModule().getModule());
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
		} 
		catch (SQLException e) {
			throw new Error(e);
		}
	}
	
	static final String deleteSDSql = "drop table FLOW_SESSION";
	static final String deleteSDSeqSql = "drop sequence FLOW_ID_SEQ";
	static final String deleteSMDSeqSql = "drop sequence FLOW_META_ID_SEQ";
	static final String deleteSMDSql = "drop TABLE FLOW_SESSION_META";

	public static void cleanupMemTables(BaseDatabaseType dbEnum) {
		try {
			ConnectionHelper.executeUpdate(dbEnum, deleteSDSeqSql);
			ConnectionHelper.executeUpdate(dbEnum, deleteSDSql);
			ConnectionHelper.executeUpdate(dbEnum, deleteSMDSeqSql);
			ConnectionHelper.executeUpdate(dbEnum, deleteSMDSql);
		} 
		catch (SQLException e) {
			throw new Error(e);
		}

	}
	
}

