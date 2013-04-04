package org.lightj.util;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

public class ActorUtil {
	
	/** create actor system */
	private static ActorSystem system = ActorSystem.create(); 

	public static final ActorRef createActor(UntypedActorFactory actorFactory) {
		// build the step
		
		return system.actorOf(new Props(actorFactory));
	}
}
