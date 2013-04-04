package org.lightj.clustering;

import com.sun.enterprise.ee.cms.core.FailureNotificationSignal;
import com.sun.enterprise.ee.cms.core.FailureSuspectedSignal;
import com.sun.enterprise.ee.cms.core.GroupLeadershipNotificationSignal;
import com.sun.enterprise.ee.cms.core.JoinNotificationSignal;
import com.sun.enterprise.ee.cms.core.JoinedAndReadyNotificationSignal;
import com.sun.enterprise.ee.cms.core.PlannedShutdownSignal;

/**
 * clustering event handler
 * @author biyu
 *
 */
public interface ClusteringEventHandler {
	
	/**
	 * node failure event
	 * @param signal
	 */
	public void handleFailureNotificationSignal(FailureNotificationSignal signal);
	
	/**
	 * node suspect failure event
	 * @param signal
	 */
	public void handleFailureSuspectedSignal(FailureSuspectedSignal signal);
	
	/**
	 * group leadership event
	 * @param signal
	 */
	public void handleGroupLeadershipSignal(GroupLeadershipNotificationSignal signal);
	
	/**
	 * a node joined and ready
	 * @param signal
	 */
	public void handleJoinedAndReadySignal(JoinedAndReadyNotificationSignal signal);
	
	/**
	 * a node joined
	 * @param signal
	 */
	public void handleJoinNotificationSignal(JoinNotificationSignal signal);
	
	/**
	 * planned shutdown event
	 * @param signal
	 */
	public void handlePlannedShutdownSignal(PlannedShutdownSignal signal);

}
