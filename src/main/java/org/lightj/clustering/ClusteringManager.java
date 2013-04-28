package org.lightj.clustering;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
 * clustering manager
 * @author biyu
 *
 */
public class ClusteringManager {

	/** logger */
	static Logger logger = LoggerFactory.getLogger(ClusterManager.class);
	
	public static final String NODEID_DELIMITER = ":"; 
	
	/**
	 * hostname encoding dictionary
	 */
	public static Map<String, String> hostNameEncoding = new HashMap<String, String>();
	
	static {
		hostNameEncoding.put(NODEID_DELIMITER, "#colon#");
	}

	/** singleton */
	private static ClusteringManager s_instance;

	/** existing clusters */
	private ConcurrentMap<String, Wrapper> existingGroups = new ConcurrentHashMap<String, Wrapper>(); 
	
	/** singleton */
	public synchronized static ClusteringManager getInstance() {
		if (s_instance == null) {
			s_instance = new ClusteringManager();
		}
		return s_instance;
	}
	
	/** private constructor */
	private ClusteringManager() {
	}

	/**
	 * parse and escape nodeid token and return tokens
	 * @param nodeId
	 * @return tokens array [hostname, uuid]
	 */
	public static String[] getNodeTokens(String nodeId) {
		String[] result = new String[0];
		if (!StringUtil.isNullOrEmpty(nodeId)) {
			result = nodeId.split(ClusteringManager.NODEID_DELIMITER);
			for (int idx = 0; idx < result.length; idx++) {
				result[idx] = StringUtil.decode(ClusteringManager.hostNameEncoding, result[idx]);
			}
		}
		return result;
	}
	
	/**
	 * start or join a cluster
	 * @param group
	 * @param type
	 * @param evtHandler
	 * @throws ClusteringException
	 */
	public synchronized void startOrJoin(String group, 
			GroupManagementService.MemberType type, 
			ClusteringEventHandler evtHandler) throws ClusteringException
	{
		if (StringUtil.isNullOrEmpty(group)) {
			throw new IllegalArgumentException("Cluster group cannot be empty");
		}
		
		boolean existing = existingGroups.containsKey(group);
		EventHandler handler = existing ? existingGroups.get(group).handler : new EventHandler();
		handler.addEventHandler(evtHandler);
		
		if (!existing) {
			String serverName = StringUtil.encode(hostNameEncoding, NetUtil.getMyHostName()) + NODEID_DELIMITER + UUID.randomUUID().toString();

			//initialize Group Management Service
			GroupManagementService gms = initializeGMS(serverName, group, type);

			//register for Group Events
			logger.info("Registering for group event notifications");
			gms.addActionFactory(new JoinNotificationActionFactoryImpl(handler));
			gms.addActionFactory(new FailureNotificationActionFactoryImpl(handler));
			gms.addActionFactory(new FailureSuspectedActionFactoryImpl(handler));
			gms.addActionFactory(new GroupLeadershipNotificationActionFactoryImpl(handler));
			gms.addActionFactory(new JoinedAndReadyNotificationActionFactoryImpl(handler));
			gms.addActionFactory(new JoinNotificationActionFactoryImpl(handler));
			gms.addActionFactory(new PlannedShutdownActionFactoryImpl(handler));

			//join group
			logger.info("Joining Group " + group);
			try {
				gms.join();
			} catch (GMSException e) {
				throw new ClusteringException(e);
			}
			existingGroups.put(group, new Wrapper(gms, handler));
		}

	}
	
	/**
	 * leave a cluster
	 * @param group
	 * @throws GMSException
	 */
	public synchronized void leave(String group) {
		if (existingGroups.containsKey(group)) {
			existingGroups.get(group).gms.shutdown(GMSConstants.shutdownType.INSTANCE_SHUTDOWN);
			EventHandler handler = existingGroups.get(group).handler;
			handler.removeAllEventHandlers();

		}
	}
	
	/**
	 * shutdown all groups
	 *
	 */
	synchronized void shutdown() {
		for (String group : existingGroups.keySet()) {
			leave(group);
		}
	}

	/**
	 * init group management service
	 * @param serverName
	 * @param groupName
	 * @param type
	 * @return
	 */
	private GroupManagementService initializeGMS(String serverName,
			String groupName, GroupManagementService.MemberType type) 
	{
		logger.info("Initializing Shoal for member: "	+ serverName + " group:" + groupName);
		return (GroupManagementService) GMSFactory.startGMSModule(
					serverName,	groupName, type, new Properties());
	}

	/**
	 * simple wrapper
	 * @author biyu
	 *
	 */
	private class Wrapper {
		GroupManagementService gms;
		EventHandler handler;
		Wrapper(GroupManagementService gms, EventHandler handler) {
			this.gms = gms;
			this.handler = handler;
		}
	}
	
	
	/**
	 * deligate event for internal handling
	 * @author biyu
	 *
	 */
	private class EventHandler implements CallBack {

		private Set<ClusteringEventHandler> handlers = new HashSet<ClusteringEventHandler>();

		EventHandler() {
		}
		
		void addEventHandler(ClusteringEventHandler handler) {
			handlers.add(handler);
		}
		void removeAllEventHandlers() {
			handlers.clear();
		}
		
		public void processNotification(Signal notification) {
			logger.info("***Cluster signal received: GroupName = "
							+ notification.getGroupName()
							+ ", Signal.getMemberToken() = "
							+ notification.getMemberToken());
			for (ClusteringEventHandler handler : handlers)  {
				if (notification instanceof FailureNotificationSignal) {
					handler.handleFailureNotificationSignal((FailureNotificationSignal) notification);
				} 
				else if (notification instanceof FailureSuspectedSignal) {
					handler.handleFailureSuspectedSignal((FailureSuspectedSignal) notification);
				} 
				else if (notification instanceof GroupLeadershipNotificationSignal) {
					handler.handleGroupLeadershipSignal((GroupLeadershipNotificationSignal) notification);
				} 
				else if (notification instanceof JoinedAndReadyNotificationSignal) {
					handler.handleJoinedAndReadySignal((JoinedAndReadyNotificationSignal) notification);
				} 
				else if (notification instanceof JoinNotificationSignal) {
					handler.handleJoinNotificationSignal((JoinNotificationSignal) notification);
				} 
				else if (notification instanceof PlannedShutdownSignal) {
					handler.handlePlannedShutdownSignal((PlannedShutdownSignal) notification);
				} else {
					logger.error("received unkown notification type:"	+ notification);
				}
			}
		}
	}

}
