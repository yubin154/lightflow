package org.lightj.session;

import org.lightj.clustering.ClusteringEventHandler;
import org.lightj.clustering.ClusteringModule;
import org.lightj.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.enterprise.ee.cms.core.FailureNotificationSignal;
import com.sun.enterprise.ee.cms.core.FailureSuspectedSignal;
import com.sun.enterprise.ee.cms.core.GroupLeadershipNotificationSignal;
import com.sun.enterprise.ee.cms.core.JoinNotificationSignal;
import com.sun.enterprise.ee.cms.core.JoinedAndReadyNotificationSignal;
import com.sun.enterprise.ee.cms.core.PlannedShutdownSignal;
import com.sun.enterprise.ee.cms.core.Signal;
import com.sun.enterprise.ee.cms.core.SignalAcquireException;
import com.sun.enterprise.ee.cms.core.SignalReleaseException;

/**
 * handle clustering events
 * @author binyu
 *
 */
public class FlowClusteringEventHandler implements ClusteringEventHandler {
	
	static Logger logger = LoggerFactory.getLogger(FlowClusteringEventHandler.class);

	public FlowClusteringEventHandler() {
	}
	
	public void handleFailureNotificationSignal(FailureNotificationSignal signal) {
		recoverFromDownNode(signal);
	}

	public void handleFailureSuspectedSignal(FailureSuspectedSignal signal) {
	}

	public void handleGroupLeadershipSignal(GroupLeadershipNotificationSignal signal) {
	}

	public void handleJoinNotificationSignal(JoinNotificationSignal signal) {
	}

	public void handleJoinedAndReadySignal(JoinedAndReadyNotificationSignal signal) {
	}

	public void handlePlannedShutdownSignal(PlannedShutdownSignal signal) {
		recoverFromDownNode(signal);
	}

	/**
	 * recover session from crashed/down server
	 * @param nodeId
	 */
	private synchronized void recoverFromDownNode(Signal signal) {
		try {
			signal.acquire();
			String nodeId = signal.getMemberToken();
			if (!StringUtil.isNullOrEmpty(nodeId)) {
				final String runBy = ClusteringModule.getNodeTokens(nodeId)[0];
				logger.info("Try to recover sessions from remote crash/down server " + runBy);
				FlowSessionFactory.getInstance().recoverCrashedSession(runBy);
			}
		} catch (SignalAcquireException e) {
			logger.error("fail to acquire signal for recovery, " + e.getMessage());
		} finally {
			try {
				signal.release();
			} catch (SignalReleaseException e) {
				// ignore
			}
		}
	}
	
}

