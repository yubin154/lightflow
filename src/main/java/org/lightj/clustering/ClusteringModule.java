package org.lightj.clustering;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.lightj.RuntimeContext;
import org.lightj.initialization.BaseInitializable;
import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;
import org.lightj.util.NetUtil;
import org.lightj.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.enterprise.ee.cms.core.CallBack;
import com.sun.enterprise.ee.cms.core.FailureNotificationSignal;
import com.sun.enterprise.ee.cms.core.FailureSuspectedSignal;
import com.sun.enterprise.ee.cms.core.GMSConstants;
import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.core.GMSFactory;
import com.sun.enterprise.ee.cms.core.GMSNotEnabledException;
import com.sun.enterprise.ee.cms.core.GMSNotInitializedException;
import com.sun.enterprise.ee.cms.core.GroupLeadershipNotificationSignal;
import com.sun.enterprise.ee.cms.core.GroupManagementService;
import com.sun.enterprise.ee.cms.core.JoinNotificationSignal;
import com.sun.enterprise.ee.cms.core.JoinedAndReadyNotificationSignal;
import com.sun.enterprise.ee.cms.core.PlannedShutdownSignal;
import com.sun.enterprise.ee.cms.core.Signal;
import com.sun.enterprise.ee.cms.impl.client.FailureNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.FailureSuspectedActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.GroupLeadershipNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.JoinNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.JoinedAndReadyNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.PlannedShutdownActionFactoryImpl;
import com.sun.enterprise.mgmt.ClusterManager;

/**
 * module initializing cluster capability
 * @author biyu
 *
 */
public class ClusteringModule  {

	/** logger */
	static Logger logger = LoggerFactory.getLogger(ClusterManager.class);

	/** delimiter */
	public static final String NODEID_DELIMITER = ":";
	public static Map<String, String> hostNameEncoding = new HashMap<String, String>();
	static {
		hostNameEncoding.put(NODEID_DELIMITER, "#colon#");
	}

	/** inner module that controls singleton behavior */
	private static ClusteringInnerModule s_Module = null;
	
	/** constructor */
	public ClusteringModule() {
		init();
	}
	
	/** init */
	private synchronized void init() {
		if (s_Module == null) {
			s_Module = new ClusteringInnerModule();
		}
	}
	
	public BaseModule getModule() {
		return s_Module;
	}
	
	static void validateChange() {
		if (s_Module == null) {
			throw new InitializationException("ClusteringModule not initialized");
		}
		s_Module.validateForChange();
	}
	
	static void validateInit() {
		if (s_Module == null) {
			throw new InitializationException("ClusteringModule not initialized");
		}
	}
	
	/**
	 * parse and escape nodeid token and return tokens
	 * 
	 * @param nodeId
	 * @return tokens array [hostname, uuid]
	 */
	public static String[] getNodeTokens(String nodeId) {
		String[] result = new String[0];
		if (!StringUtil.isNullOrEmpty(nodeId)) {
			result = nodeId.split(NODEID_DELIMITER);
			for (int idx = 0; idx < result.length; idx++) {
				result[idx] = StringUtil.decode(hostNameEncoding, result[idx]);
			}
		}
		return result;
	}

	/**
	 * is master node within the cluster
	 * @param group
	 * @return
	 */
	public static boolean isMaster(String group) {
		validateInit();
		if (GMSFactory.isGMSEnabled(group)) {
			try {
				return GMSFactory.getGMSModule(group).getGroupHandle().isGroupLeader();
			} catch (GMSNotEnabledException e) {
				// ignore, not in a cluster, is local master
				return true;
			} catch (GMSNotInitializedException e) {
				// ignore, not in a cluster, is local master
				return true;
			} catch (GMSException e) {
				// ignore
				return false;
			}
		}
		else {
			return true;
		}
	}

	/**
	 * is master node within the default cluster
	 * @return
	 */
	public static boolean isMaster() {
		return isMaster(ClusteringModule.getClusterName());
	}

	/**
	 * start or join with default cluster name
	 * 
	 * @param type
	 * @param evtHandler
	 * @throws ClusteringException
	 */
	public static synchronized void startOrJoin(
			GroupManagementService.MemberType type,
			ClusteringEventHandler evtHandler) throws ClusteringException {
		startOrJoin(ClusteringModule.getClusterName(), type, evtHandler);
	}

	/**
	 * start or join a cluster
	 * 
	 * @param group
	 * @param type
	 * @param evtHandler
	 * @throws ClusteringException
	 */
	public static synchronized void startOrJoin(String group,
			GroupManagementService.MemberType type,
			ClusteringEventHandler evtHandler) throws ClusteringException 
	{
		validateInit();
		if (StringUtil.isNullOrEmpty(group)) {
			throw new IllegalArgumentException("Cluster group cannot be empty");
		}

		GroupManagementService gms = null;
		EventHandler handler = new EventHandler(evtHandler);
		boolean existing = true; 
		
		try {
			gms = GMSFactory.getGMSModule(group);
		} catch (GMSNotEnabledException e1) {
			existing = false;
		} catch (GMSNotInitializedException e1) {
			existing = false;
		} catch (GMSException e1) {
			throw new ClusteringException(e1);
		}

		if (!existing) {
			String serverName = StringUtil.encode(hostNameEncoding,
					NetUtil.getMyHostName())
					+ NODEID_DELIMITER + UUID.randomUUID().toString();

			// initialize Group Management Service
			logger.info("Initializing Shoal for member: " + serverName + " group:" + group);
			gms = (GroupManagementService) GMSFactory.startGMSModule(serverName, group, type, new Properties());

			// join group
			logger.info("Joining Group " + group);
			try {
				gms.join();
			} catch (GMSException e) {
				throw new ClusteringException(e);
			}
		}
		
		// register for Group Events
		logger.info("Registering for group event notifications");
		gms.addActionFactory(new JoinNotificationActionFactoryImpl(handler));
		gms.addActionFactory(new FailureNotificationActionFactoryImpl(handler));
		gms.addActionFactory(new FailureSuspectedActionFactoryImpl(handler));
		gms.addActionFactory(new GroupLeadershipNotificationActionFactoryImpl(handler));
		gms.addActionFactory(new JoinedAndReadyNotificationActionFactoryImpl(handler));
		gms.addActionFactory(new JoinNotificationActionFactoryImpl(handler));
		gms.addActionFactory(new PlannedShutdownActionFactoryImpl(handler));

	}

	/**
	 * deligate event for internal handling
	 * 
	 * @author biyu
	 * 
	 */
	private static class EventHandler implements CallBack {

		private ClusteringEventHandler handler;

		EventHandler(ClusteringEventHandler handler) {
			this.handler = handler;
		}

		public void processNotification(Signal notification) {
			logger.info("***Cluster signal received: GroupName = "
					+ notification.getGroupName()
					+ ", Signal.getMemberToken() = "
					+ notification.getMemberToken());
			if (notification instanceof FailureNotificationSignal) {
				handler.handleFailureNotificationSignal((FailureNotificationSignal) notification);
			} else if (notification instanceof FailureSuspectedSignal) {
				handler.handleFailureSuspectedSignal((FailureSuspectedSignal) notification);
			} else if (notification instanceof GroupLeadershipNotificationSignal) {
				handler.handleGroupLeadershipSignal((GroupLeadershipNotificationSignal) notification);
			} else if (notification instanceof JoinedAndReadyNotificationSignal) {
				handler.handleJoinedAndReadySignal((JoinedAndReadyNotificationSignal) notification);
			} else if (notification instanceof JoinNotificationSignal) {
				handler.handleJoinNotificationSignal((JoinNotificationSignal) notification);
			} else if (notification instanceof PlannedShutdownSignal) {
				handler.handlePlannedShutdownSignal((PlannedShutdownSignal) notification);
			} else {
				logger.error("received unkown notification type:"
						+ notification);
			}
		}

	}

	public ClusteringModule setClusterName(String clusterName) {
		s_Module.validateForChange();
		s_Module.clusterName = clusterName;
		return this;
	}
	
	public static String getClusterName() {
		return s_Module.clusterName!=null ? s_Module.clusterName : RuntimeContext.getClusterName();
	}
	
	/** inner module does the real init */
	private class ClusteringInnerModule extends BaseModule {

		private String clusterName;
		ClusteringInnerModule() {
			
			super(new BaseModule[] {});

			addInitializable(new BaseInitializable() {
				@Override
				protected void initialize() {
				}

				@Override
				protected void shutdown() {
					shutdownAllClusters();
				}

			});

		}

		/**
		 * shutdown all groups
		 * 
		 */
		private synchronized void shutdownAllClusters() {
			for (Object service : GMSFactory.getAllGMSInstancesForMember()) {
				((GroupManagementService) service).shutdown(GMSConstants.shutdownType.INSTANCE_SHUTDOWN);
			}
		}

	}
}
