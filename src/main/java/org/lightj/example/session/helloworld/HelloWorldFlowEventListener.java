package org.lightj.example.session.helloworld;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.lightj.session.FlowEvent;
import org.lightj.session.FlowSession;
import org.lightj.session.IFlowEventListener;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepTransition;
import org.lightj.util.ConcurrentUtil;


/**
 * a dummy flow event listener
 * 
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
public class HelloWorldFlowEventListener implements IFlowEventListener {
	
	private ReentrantLock l;
	private Condition c;
	
	public HelloWorldFlowEventListener() {}
	
	public HelloWorldFlowEventListener(ReentrantLock l, Condition c) {
		this.l = l;
		this.c = c;
	}

	public void handleError(Throwable t, FlowSession session) {
//		if (l != null && c != null) {
//			ConcurrentUtil.signalAll(l, c);
//		}
	}

	public void handleFlowEvent(FlowEvent event, FlowSession session, String msg) {
		if (event == FlowEvent.stop || event == FlowEvent.pause) {
			if (l != null && c != null) {
				ConcurrentUtil.signalAll(l, c);
			}
		}
	}

	public void handleStepEvent(FlowEvent event, FlowSession session,
			IFlowStep flowStep, StepTransition stepTransition) {
	}

}
