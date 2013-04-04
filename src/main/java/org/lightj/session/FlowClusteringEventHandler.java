package org.lightj.session;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.lightj.clustering.ClusteringEventHandler;
import org.lightj.clustering.ClusteringManager;
import org.lightj.util.Log4jProxy;
import org.lightj.util.StringUtil;

import com.sun.enterprise.ee.cms.core.FailureNotificationSignal;
import com.sun.enterprise.ee.cms.core.FailureSuspectedSignal;
import com.sun.enterprise.ee.cms.core.GroupLeadershipNotificationSignal;
import com.sun.enterprise.ee.cms.core.JoinNotificationSignal;
import com.sun.enterprise.ee.cms.core.JoinedAndReadyNotificationSignal;
import com.sun.enterprise.ee.cms.core.PlannedShutdownSignal;

public class FlowClusteringEventHandler implements ClusteringEventHandler {
	
	static Log4jProxy logger = Log4jProxy.getInstance(FlowClusteringEventHandler.class);

	private ConcurrentHashMap<String, Timer> recoverTimerMap = new ConcurrentHashMap<String, Timer>();
	private int minDelay = 10000;
	private int maxDelay = 60000;
	
	public FlowClusteringEventHandler() {
	}
	
	public FlowClusteringEventHandler(int minDelayMs, int maxDelayMs) {
		this.minDelay = minDelayMs;
		this.maxDelay = maxDelayMs;
	}
	
	public void handleFailureNotificationSignal(FailureNotificationSignal signal) {
		recoverFromDownNode(signal.getMemberToken());
	}

	public void handleFailureSuspectedSignal(FailureSuspectedSignal signal) {
	}

	public void handleGroupLeadershipSignal(GroupLeadershipNotificationSignal signal) {
	}

	public void handleJoinNotificationSignal(JoinNotificationSignal signal) {
		cancelRecover(signal.getMemberToken());
	}

	public void handleJoinedAndReadySignal(JoinedAndReadyNotificationSignal signal) {
	}

	public void handlePlannedShutdownSignal(PlannedShutdownSignal signal) {
		recoverFromDownNode(signal.getMemberToken());
	}

	/**
	 * recover session from crashed/down server
	 * @param nodeId
	 */
	private synchronized void recoverFromDownNode(String nodeId) {
		if (!StringUtil.isNullOrEmpty(nodeId)) {
			final String runBy = ClusteringManager.getNodeTokens(nodeId)[0];
			if (!StringUtil.isNullOrEmpty(runBy)) {
				Timer timer = new Timer();
				// generate a random delay of 10 - 60 seconds
				long delay = minDelay + (int)(Math.random() * ((maxDelay - minDelay) + 1));
				timer.schedule(new TimerTask() {
					
					@Override
					public void run() {
						logger.info("Try to recover sessions from remote crash/down server " + runBy);
						FlowSessionFactory.getInstance().recoverCrashedSession(runBy);
					}
				}, delay);
				recoverTimerMap.putIfAbsent(runBy, timer);
			}
		}
	}
	
	/**
	 * cancel an unfired recover timer if the target node recovered by itself
	 * @param nodeId
	 */
	private synchronized void cancelRecover(String nodeId) {
		if (!StringUtil.isNullOrEmpty(nodeId)) { 
			String runBy = ClusteringManager.getNodeTokens(nodeId)[0];
			Timer timer = recoverTimerMap.remove(runBy);
			if (timer != null) {
				timer.cancel();
			}
		}
	}
}

