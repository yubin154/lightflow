package org.lightj.task;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
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
	private final AtomicInteger resultCount;
	private final boolean isBatch;
	private final int taskCount;
	private volatile int submittedIdx = 0;
	private ActorRef[] workers;
	
	/** requester */
	private ActorRef sender = null;
	private boolean sentReply = false;

	/** runtime */
	private final SupervisorStrategy supervisorStrategy;

	/** unfinished work */
	private Cancellable timeoutMessageCancellable = null;
	private Cancellable retryMessageCancellable = null;
	private FiniteDuration timeoutDuration = null;
	
	/** internal message */
	private enum InternalMessageType {
		OPERATION_TIMEOUT
	}

	/** constructor */
	public BatchTaskWorker(final BatchTask task, final UntypedActorFactory workerFactory, final ITaskListener listener) {
		super();
		this.task = task;
		this.workerFactory = workerFactory;
		this.listener = listener;
		this.taskCount = task.getTasks().length;
		this.resultCount = new AtomicInteger(taskCount);
		this.isBatch = (taskCount > 1);
		
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
			else if (message instanceof InternalMessageType) {
				
				switch ((InternalMessageType) message) {
				
				case OPERATION_TIMEOUT:
					operationTimeout();
					break;
				
				}
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
	 * send request
	 */
	private final void processRequest() {
		sender = getSender();
		workers = new ActorRef[taskCount];
		for (int idx = 0; idx < taskCount; idx++) {
			Task atask = task.getTasks()[idx];
			ActorRef worker = getContext().actorOf(new Props(workerFactory));
			workers[idx] = worker;
			if (task.getBatchOption()==null || submittedIdx < task.getBatchOption().getMaxConcurrencyCount()) {
				worker.tell(atask, getSelf());
				submittedIdx++;
			}
		}
		
		// To handle cases where this operation takes extremely long, schedule a 'timeout' message to be sent to us
		if (task.getExecOptions().hasTimeout() && timeoutMessageCancellable == null) {
			timeoutDuration = Duration.create(task.getExecOptions().getTimeoutInMs(), TimeUnit.MILLISECONDS);
			timeoutMessageCancellable = getContext()
					.system()
					.scheduler()
					.scheduleOnce(timeoutDuration, getSelf(), 
							InternalMessageType.OPERATION_TIMEOUT, 
							getContext().system().dispatcher());
		}
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
			if (submittedIdx < taskCount) {
				workers[submittedIdx].tell(task.getTasks()[submittedIdx], getSelf());
				submittedIdx++;
			}
			listener.handleTaskResult(workerMsg.getTask(), workerMsg.getResult());
			if (resultCount.decrementAndGet() == 0) {
				if (!isBatch) {
					replySingle(workerMsg.getTask());
				} else {
					replyBatch();
				}
			}
			break;
		case completed:
			// ignore
			break;
		default:
			break;
		}
	}
	
	/**
	 * handle timeout
	 */
	private final void operationTimeout() {
		replyError(TaskResultEnum.Timeout, 
				String.format("OperationTimedout, took more than %d ms", task.getExecOptions().getTimeoutInMs()), 
				null);
	}


	@Override
	public void postStop() {
		if (retryMessageCancellable != null && !retryMessageCancellable.isCancelled()) {
			retryMessageCancellable.cancel();
		}
		if (timeoutMessageCancellable != null && !timeoutMessageCancellable.isCancelled()) {
			timeoutMessageCancellable.cancel();
		}
		if (workers != null) {
			for (ActorRef worker : workers) {
				if (worker != null && !worker.isTerminated()) {
					worker.tell(PoisonPill.getInstance(), null);
				}
			}
		}
	}
	
	/**
	 * reply with error result, batch failed
	 * @param state
	 * @param msg
	 * @param stackTrace
	 */
	private final void replyError(TaskResultEnum state, String msg, Throwable stackTrace) {
		if (!sentReply) {
			TaskResult tr = task.createErrorResult(state, msg, stackTrace);
			listener.handleTaskResult(task, tr);
			listener.taskCompleted(task);
			sender.tell(WorkerMessageType.COMPLETE_REQUEST, getSelf());
			sentReply = true;
		}

		// Self-terminate
		getSelf().tell(PoisonPill.getInstance(), null);
	}
	
	/**
	 * reply with single result, batch of a single task complete
	 * @param task
	 */
	private final void replySingle(final Task task) {
		if (!sentReply) {
			listener.taskCompleted(task);
			sender.tell(WorkerMessageType.COMPLETE_REQUEST, getSelf());
			sentReply = true;
		}

		// Self-terminate
		getSelf().tell(PoisonPill.getInstance(), null);
		
	}
	
	/**
	 * reply with batch result, batch of multiple tasks complete
	 * @param taskResult
	 */
	private final void replyBatch() {
		if (!sentReply) {
			listener.taskCompleted(task);
			sender.tell(WorkerMessageType.COMPLETE_REQUEST, getSelf());
			sentReply = true;
		}

		// Self-terminate
		getSelf().tell(PoisonPill.getInstance(), null);
	}

	@Override
	public SupervisorStrategy supervisorStrategy() {
		return supervisorStrategy;
	}

}
