package org.lightj.clustering;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Test;
import org.lightj.BaseTestCase;
import org.lightj.RuntimeContext;
import org.lightj.initialization.BaseModule;
import org.lightj.session.FlowClusteringEventHandler;
import org.lightj.util.ConcurrentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.enterprise.ee.cms.core.FailureNotificationSignal;
import com.sun.enterprise.ee.cms.core.FailureSuspectedSignal;
import com.sun.enterprise.ee.cms.core.GroupLeadershipNotificationSignal;
import com.sun.enterprise.ee.cms.core.GroupManagementService.MemberType;
import com.sun.enterprise.ee.cms.core.JoinNotificationSignal;
import com.sun.enterprise.ee.cms.core.JoinedAndReadyNotificationSignal;
import com.sun.enterprise.ee.cms.core.PlannedShutdownSignal;
import com.sun.enterprise.ee.cms.core.Signal;
import com.sun.enterprise.ee.cms.core.SignalAcquireException;
import com.sun.enterprise.ee.cms.core.SignalReleaseException;

public class TestClustering extends BaseTestCase {
	
	static Logger logger = LoggerFactory.getLogger(TestClustering.class);
	static Logger getLogger() {
		return logger;
	}

	/** pause lock */
	protected ReentrantLock lock = new ReentrantLock();
	
	/** pause condition */
	protected Condition cond = lock.newCondition();


	@Test
	public void testMultiNodesCluster() throws Exception {
		final String group = "group";
		for (int i = 0; i < 3; i++) {
			final int j = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						ClusteringModule.startOrJoin(group, MemberType.CORE, new EvtHandler());
					} catch (ClusteringException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
		ConcurrentUtil.wait(lock, cond, 5000L);
	}

	@Test
	public void testSingleLeader() throws Exception {
		final String group = "group";
		ClusteringModule.startOrJoin(group, MemberType.CORE, new EvtHandler());
		ConcurrentUtil.wait(lock, cond, 5000L);
	}
	
	@Test
	public void testFlowClusterHandler() throws Exception {
		for (int i = 0; i < 3; i++) {
			ClusteringModule.startOrJoin(RuntimeContext.getClusterName(), MemberType.CORE, new FlowClusteringEventHandler());
		}
		ConcurrentUtil.wait(lock, cond, 5000L);
	}
	
	static class EvtHandler implements ClusteringEventHandler {

		public void handleFailureNotificationSignal(FailureNotificationSignal signal) {
			getDetails(signal);
		}

		public void handleFailureSuspectedSignal(FailureSuspectedSignal signal) {
			getDetails(signal);
		}

		public void handleGroupLeadershipSignal(GroupLeadershipNotificationSignal signal) {
			logger.info(String.format("I'm leader %s,%s", signal.getGroupName(), signal.getMemberToken()));
			getDetails(signal);
		}

		public void handleJoinNotificationSignal(JoinNotificationSignal signal) {
			logger.info(String.format("I joined %s,%s", signal.getGroupName(), signal.getMemberToken()));
			getDetails(signal);
		}

		public void handleJoinedAndReadySignal(JoinedAndReadyNotificationSignal signal) {
			getDetails(signal);
		}

		public void handlePlannedShutdownSignal(PlannedShutdownSignal signal) {
			getDetails(signal);
		}
		
		private void getDetails(Signal signal) {
			try {
				signal.acquire();
				logger.info(String.format("processed %s,%s,%s", signal.toString(), signal.getGroupName(), signal.getMemberToken()));
			} catch (SignalAcquireException e) {
				e.printStackTrace();
			} finally {
				try {
					signal.release();
				} catch (SignalReleaseException e) {
					e.printStackTrace();
				}
			}
			getLogger().info(new Date(signal.getStartTime()).toString());
			for (java.util.Map.Entry<Serializable, Serializable> entry : signal.getMemberDetails().entrySet()) {
				Serializable k = entry.getKey();
				Serializable v = entry.getValue();
				System.out.println(k.toString()+'='+v.toString());
			}
		}
	}

	@Override
	protected BaseModule[] getDependentModules() {
		return new BaseModule[] {
				new ClusteringModule().getModule(),
		};
	}
}
