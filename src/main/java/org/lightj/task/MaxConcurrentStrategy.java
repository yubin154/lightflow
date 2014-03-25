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

public class MaxConcurrentStrategy extends UntypedActor implements IWorker {

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
			
			if (message instanceof Task) {
				if (concurrentRate.incrementAndGet() <= maxConcurrentRate) {
					getSender().tell(message, getSelf());
				}
				else {
					taskQ.offer((Task) message);
				}
				
			}
			else if (message instanceof RateSettingMessage) {
				maxConcurrentRate = ((RateSettingMessage) message).getMaxRate();
			}
			else if (message == WorkerMessageType.COMPLETE_TASK) {
				
				concurrentRate.decrementAndGet();
				getSender().tell(taskQ.take(), getSelf());
				concurrentRate.incrementAndGet();
				
			} 
			else {
				unhandled(message);
			}
		} 
		catch (Throwable e) {
			// should have never happened
		}

		
	}

}
