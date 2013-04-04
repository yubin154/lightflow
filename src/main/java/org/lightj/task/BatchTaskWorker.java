package org.lightj.task;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.OneForOneStrategy;
import akka.actor.PoisonPill;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.UntypedActor;
import akka.japi.Function;


/**
 * Provides abstraction of a http operation. The operation can include long polling. 
 */
@SuppressWarnings("rawtypes")
public class BatchTaskWorker extends UntypedActor implements IWorker {
	
	/** task */
	private final BatchTask task;
	private final ActorRef[] workers;
	private final ITaskListener listener;
	private final AtomicInteger resultCounter;
	private final boolean isBatch;
	
	/** requester */
	private ActorRef sender = null;
	private boolean sentReply = false;

	/** runtime */
	private final SupervisorStrategy supervisorStrategy;

	/** unfinished work */
	private Cancellable timeoutMessageCancellable = null;
	private Cancellable retryMessageCancellable = null;
	private FiniteDuration timeoutDuration = null;
	
	private enum InternalMessageType {
		OPERATION_TIMEOUT
	}

	public BatchTaskWorker(final BatchTask task, final ActorRef[] workers, final ITaskListener listener) {
		super();
		this.task = task;
		this.workers = workers;
		this.listener = listener;
		this.resultCounter = new AtomicInteger(workers.length);
		this.isBatch = (workers.length > 1);
		
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
				case PROCESS_REQUEST:
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
		catch (Exception e) {
			// should have never happened
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			replyError(TaskResultEnum.Failed, e.toString(), sw.toString());
		}
	}
	
	/**
	 * send request
	 */
	private final void processRequest() {
		sender = getSender();
		for (ActorRef worker : workers) {
			worker.tell(WorkerMessageType.PROCESS_REQUEST, getSelf());
		}
		
		// To handle cases where this operation takes extremely long, schedule a 'timeout' message to be sent to us
		if (task.getExecOptions().hasTimeout() && timeoutMessageCancellable == null) {
			timeoutDuration = Duration.create(task.getExecOptions().getTimeoutInMs(), TimeUnit.MILLISECONDS);
			timeoutMessageCancellable = getContext().system().scheduler()
					.scheduleOnce(timeoutDuration, getSelf(), InternalMessageType.OPERATION_TIMEOUT, getContext().system().dispatcher());
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
			listener.handleTaskResult(workerMsg.getTask(), workerMsg.getResult());
			if (resultCounter.decrementAndGet() == 0) {
				if (!isBatch) {
					replySingle(workerMsg.getTask());
				} else {
					replyBatch(task.createTaskResult(TaskResultEnum.Success, null));
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
				String.format("OperationTimedout, took more than %d seconds", task.getExecOptions().getTimeoutInMs()/1000), 
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
		for (final ActorRef worker : workers) {
			if (worker != null && !worker.isTerminated()) {
				worker.tell(PoisonPill.getInstance(), null);
			}
		}
	}
	
	private final void replyError(TaskResultEnum state, String msg, String stackTrace) {
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
	
	private final void replySingle(final Task task) {
		if (!sentReply) {
			listener.taskCompleted(task);
			sender.tell(WorkerMessageType.COMPLETE_REQUEST, getSelf());
			sentReply = true;
		}

		// Self-terminate
		getSelf().tell(PoisonPill.getInstance(), null);
		
	}
	
	private final void replyBatch(final TaskResult taskResult) {
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
