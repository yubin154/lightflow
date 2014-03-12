package org.lightj.task;

import org.lightj.initialization.BaseInitializable;
import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;

import akka.actor.Actor;
import akka.actor.ActorSystem;
import akka.actor.UntypedActorFactory;

@SuppressWarnings("rawtypes")
public class TaskModule {

	/** singleton */
	private static TaskModuleInner s_Module = null;
	
	/** constructor */
	public TaskModule() {
		init();
	}
	
	private synchronized void init() {
		if (s_Module == null) s_Module = new TaskModuleInner();
	}

	/** module to be initialized */
	public BaseModule getModule() {
		return s_Module;
	}
	
	private static final void validateInit() {
		if (s_Module == null) {
			throw new InitializationException("TaskModule not initialized");
		}
	}

	/** get actor system */
	public static ActorSystem getActorSystem() {
		validateInit();
		return s_Module.system;
	}
	
	public static UntypedActorFactory getAsyncWorkerFactory() {
		validateInit();
		return s_Module.asyncActorFactory;
	}
	
	public static UntypedActorFactory getAsyncPollWorkerFactory() {
		validateInit();
		return s_Module.asyncPollActorFctory;
	}

	private static class TaskModuleInner extends BaseModule {
		
		/** create actor system */
		private ActorSystem system;
		private UntypedActorFactory asyncActorFactory;
		private UntypedActorFactory asyncPollActorFctory;

		private TaskModuleInner() {
			addInitializable(new BaseInitializable() {

				@Override
				protected void initialize() {
					// create actor system
					system = ActorSystem.create();
					
					asyncActorFactory = new UntypedActorFactory() {
						private static final long serialVersionUID = 1L;

						@Override
						public Actor create() throws Exception {
							return new AsyncTaskWorker<ExecutableTask>();
						}

					};
					
					asyncPollActorFctory = new UntypedActorFactory() {
						private static final long serialVersionUID = 1L;

						@Override
						public Actor create() throws Exception {
							return new AsyncPollTaskWorker<ExecutableTask>();
						}

					};
				}

				@Override
				protected void shutdown() {
					system.shutdown();
				}
				
			});
		}
	}


}
