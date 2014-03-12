package org.lightj.task;

import scala.concurrent.duration.Duration;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.japi.Function;


/**
 * Provides abstraction of batch operation. 
 */
@SuppressWarnings("rawtypes")
public class BatchTaskWorker extends UntypedActor implements IWorker {
	
	/** task */
	private final BatchTask task;
	private final UntypedActorFactory workerFactory;
	private final ITaskListener listener;
	
	/** runtime */
	private final SupervisorStrategy supervisorStrategy;
	
	/** batch strategy, concurrent rate etc. */
	private ActorRef batchingStrategy;

	/** constructor */
	public BatchTaskWorker(final BatchTask task, final UntypedActorFactory workerFactory, final ITaskListener listener) {
		super();
		this.task = task;
		this.workerFactory = workerFactory;
		this.listener = listener;
		listener.setExpectedResultCount(task.getTasks().length);
		
		// Other initialization
		this.supervisorStrategy = new OneForOneStrategy(0, Duration.Inf(), new Function<Throwable, Directive>() {
			public Directive apply(Throwable arg0) {
				replyError(TaskResultEnum.Failed, "BatchWorker crashed", null);
				return SupervisorStrategy.stop();
			}
		});
	}

	@Override
	public void onReceive(Object message) throws Exception 
	{
		try {
			if (message instanceof WorkerMessageType) {
				
				switch ((WorkerMessageType) message) {
				
				case REPROCESS_REQUEST:
					processRequest();
					break;
				}
			
			}
			else if (message instanceof Task) {
				
				processTask((Task) message);
				
			}
			else if (message instanceof WorkerMessage) {
				
				final WorkerMessage r = (WorkerMessage) message;
				handleWorkerMessage(r);
			
			} 
			else {
				unhandled(message);
			}
		} 
		catch (Throwable e) {
			// should have never happened
			replyError(TaskResultEnum.Failed, e.toString(), e);
		}
	}
	
	/**
	 * send request to batching strategy
	 */
	private final void processRequest() {
		UntypedActorFactory strategyFactory = null;
		if (task.getBatchOption() != null) {
			
			switch (task.getBatchOption().getStrategy()) {
			
			case MAX_CONCURRENT_RATE_SLIDING:
				strategyFactory = new UntypedActorFactory() {
					private static final long serialVersionUID = 1L;

					@Override
					public Actor create() throws Exception {
						return new MaxConcurrentStrategy(task.getBatchOption().getConcurrentRate());
					}

				};
				break;
			default:
				break;
			}
		}
		else {
			strategyFactory = new UntypedActorFactory() {
				private static final long serialVersionUID = 1L;

				@Override
				public Actor create() throws Exception {
					return new UnlimitedStrategy();
				}

			};
			
		}
		batchingStrategy = getContext().actorOf(new Props(strategyFactory));
		for (Task atask : task.getTasks()) {
			batchingStrategy.tell(atask, getSelf());
		}
	}
	
	/**
	 * process task send by strategy
	 * @param atask
	 */
	private final void processTask(Task atask) {
		ActorRef worker = getContext().actorOf(new Props(workerFactory));
		worker.tell(atask, getSelf());
	}

	/**
	 * handle worker message
	 * @param workerMsg
	 */
	private final void handleWorkerMessage(WorkerMessage workerMsg) {
		
		switch(workerMsg.getCallbackType()) {
		case created:
			listener.taskCreated(workerMsg.getTask());
			break;
		case submitted:
			listener.taskSubmitted(workerMsg.getTask());
			break;
		case taskresult:
			batchingStrategy.tell(WorkerMessageType.COMPLETE_TASK, getSelf());
			int remaining = listener.handleTaskResult(workerMsg.getTask(), workerMsg.getResult());
			if (remaining == 0) {
				getSender().tell(WorkerMessageType.COMPLETE_REQUEST, getSelf());

				// Self-terminate
				getSelf().tell(PoisonPill.getInstance(), null);
			}
			break;
		default:
			break;
		}
	}
	
	/**
	 * reply with error result, batch failed
	 * @param state
	 * @param msg
	 * @param stackTrace
	 */
	private final void replyError(TaskResultEnum state, String msg, Throwable stackTrace) {
		TaskResult tr = task.createErrorResult(state, msg, stackTrace);
		listener.handleTaskResult(task, tr);
		getSender().tell(WorkerMessageType.COMPLETE_REQUEST, getSelf());

		// Self-terminate
		getSelf().tell(PoisonPill.getInstance(), null);
	}
	
	@Override
	public SupervisorStrategy supervisorStrategy() {
		return supervisorStrategy;
	}

}
