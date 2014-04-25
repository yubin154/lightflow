package org.lightj.task;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.lightj.task.BatchOption.RateSettingMessage;

import akka.actor.UntypedActor;

/**
 * max concurrent sliding rate strategy
 * @author binyu
 *
 */

public class MaxConcurrentStrategy extends UntypedActor {

	/** concurrent rate */
	private final AtomicInteger concurrentRate = new AtomicInteger(0);
	/** max rate */
	private int maxConcurrentRate;
	/** pending tasks */
	private final LinkedBlockingQueue<Task> taskQ = new LinkedBlockingQueue<Task>();
	
	/** constructor */
	public MaxConcurrentStrategy() {
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		
		try {
			
			// throttle a task
			if (message instanceof Task) {
				if (concurrentRate.incrementAndGet() <= maxConcurrentRate) {
					getSender().tell(message, getSelf());
				}
				else {
					taskQ.offer((Task) message);
				}
				
			}
			
			// initialize the strategy with RateSettingMessage
			else if (message instanceof RateSettingMessage) {
				maxConcurrentRate = ((RateSettingMessage) message).getMaxRate();
			}
			
			// one task complete, launch more 
			else if (message == WorkerMessage.Type.COMPLETE_TASK) {

				if (!taskQ.isEmpty()) {
					getSender().tell(taskQ.take(), getSelf());
				}
				else {
					concurrentRate.decrementAndGet();
				}
				// do nothing when queue is empty
			}
			
			// invalid msg
			else {
				unhandled(message);
			}
		} 
		catch (Throwable e) {
			// should have never happened
		}

		
	}

}
