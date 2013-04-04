package org.lightj.clustering;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.lightj.BaseTestCase;
import org.lightj.RuntimeContext;
import org.lightj.clustering.ClusteringEventHandler;
import org.lightj.clustering.ClusteringManager;
import org.lightj.clustering.ClusteringModule;
import org.lightj.initialization.BaseModule;
import org.lightj.session.FlowClusteringEventHandler;
import org.lightj.util.ConcurrentUtil;

import com.sun.enterprise.ee.cms.core.FailureNotificationSignal;
import com.sun.enterprise.ee.cms.core.FailureSuspectedSignal;
import com.sun.enterprise.ee.cms.core.GroupLeadershipNotificationSignal;
import com.sun.enterprise.ee.cms.core.JoinNotificationSignal;
import com.sun.enterprise.ee.cms.core.JoinedAndReadyNotificationSignal;
import com.sun.enterprise.ee.cms.core.PlannedShutdownSignal;
import com.sun.enterprise.ee.cms.core.GroupManagementService.MemberType;

public class TestClustering extends BaseTestCase {
	
	static Logger logger = Logger.getLogger(TestClustering.class);
	static Logger getLogger() {
		return logger;
	}

	/** pause lock */
	protected ReentrantLock lock = new ReentrantLock();
	
	/** pause condition */
	protected Condition cond = lock.newCondition();


	@Test
	public void testMultiNodesCluster() throws Exception {
		String group = "group";
		for (int i = 0; i < 3; i++) {
			ClusteringManager.getInstance().startOrJoin(group+i, MemberType.CORE, new EvtHandler());
		}
		ConcurrentUtil.wait(lock, cond, 10000L);
	}
	
	@Test
	public void testFlowClusterHandler() throws Exception {
		ClusteringManager.getInstance().startOrJoin(RuntimeContext.getClusterName(), MemberType.CORE, new FlowClusteringEventHandler());
		ConcurrentUtil.wait(lock, cond, 10000L);
	}
	
	static class EvtHandler implements ClusteringEventHandler {

		public void handleFailureNotificationSignal(FailureNotificationSignal signal) {
			getLogger().info(signal.getGroupName());
			getLogger().info(signal.getMemberToken());
			getLogger().info(new Date(signal.getStartTime()).toString());
			for (java.util.Map.Entry<Serializable, Serializable> entry : signal.getMemberDetails().entrySet()) {
				Serializable k = entry.getKey();
				Serializable v = entry.getValue();
				getLogger().info(k.toString()+'='+v.toString());
			}
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
		}
		
	}

	@Override
	protected BaseModule[] getDependentModules() {
		return new BaseModule[] {
				new ClusteringModule().getModule(),
		};
	}
}
