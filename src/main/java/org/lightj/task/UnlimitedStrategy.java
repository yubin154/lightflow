package org.lightj.task;

import akka.actor.UntypedActor;

/**
 * default unlimited rate strategy
 * 
 * @author binyu
 *
 */
public class UnlimitedStrategy extends UntypedActor implements IWorker {

	@Override
	public void onReceive(Object message) throws Exception {

		try {
			if (message instanceof Task) {
				getSender().tell(message, getSelf());
				
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
