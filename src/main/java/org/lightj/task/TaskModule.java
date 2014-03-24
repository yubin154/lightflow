package org.lightj.task;

import org.lightj.initialization.BaseInitializable;
import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;

import akka.actor.Actor;
import akka.actor.ActorSystem;
import akka.actor.UntypedActorFactory;

import com.typesafe.config.Config;

/**
 * initialization of task exeuction framework
 * 
 * @author binyu
 *
 */
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
	
	/** set actor system name and config */
	public TaskModule setActorSystemConfig(String actorSystemName, Config actorSystemConfig) {
		s_Module.actorSystemName = actorSystemName;
		s_Module.actorSystemConfig = actorSystemConfig;
		return this;
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
	
	public static UntypedActorFactory getExecutableTaskWorkerFactory() {
		validateInit();
		return s_Module.executableTaskActorFactory;
	}

	private static class TaskModuleInner extends BaseModule {
		
		private String actorSystemName;
		private Config actorSystemConfig;
		
		/** create actor system */
		private ActorSystem system;
		private UntypedActorFactory asyncActorFactory;
		private UntypedActorFactory asyncPollActorFctory;
		private UntypedActorFactory executableTaskActorFactory;

		private TaskModuleInner() {
			
			addInitializable(new BaseInitializable() {

				@Override
				protected void initialize() {
					
					// create actor system
					if (actorSystemName == null) {
						actorSystemName = "lightflowTaskModule";
					}
					if (actorSystemConfig != null) {
						system = ActorSystem.create(actorSystemName, actorSystemConfig);
					}
					else  {
						system = ActorSystem.create(actorSystemName);
					}
					
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
					
					executableTaskActorFactory = new UntypedActorFactory() {
						private static final long serialVersionUID = 1L;

						public Actor create() {
							return new ExecutableTaskWorker();
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
