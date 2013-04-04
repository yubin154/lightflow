package org.lightj.task;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.lightj.task.WorkerMessage.CallbackType;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.Actor;
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
 * Provides abstraction of a http operation. The operation can include long polling. 
 */
@SuppressWarnings("rawtypes")
public abstract class AsyncTaskWorker<T extends Task> extends UntypedActor implements IWorker {
	
	/** task */
	private final T task;
	
	/** intermediate result */
	private TaskResult curResult;

	/** requester */
	private ActorRef sender = null;
	private volatile int tryCount = 0;
	private FiniteDuration timeoutDuration = null;
	private boolean sentReply = false;

	/** runtime */
	private final SupervisorStrategy supervisorStrategy;

	/** unfinished work */
	private final List<ActorRef> asyncPollWorkers = new ArrayList<ActorRef>();
	private ActorRef asyncWorker = null;
	private Cancellable timeoutMessageCancellable = null;
	private Cancellable retryMessageCancellable = null;
	
	private enum InternalMessageType {
		OPERATION_TIMEOUT, PROCESS_REQUEST_RESULT
	}

	public AsyncTaskWorker(final T task) {
		super();
		
		this.task = task;
		
		// Other initialization
		this.supervisorStrategy = new OneForOneStrategy(0, Duration.Inf(), new Function<Throwable, Directive>() {
			public Directive apply(Throwable arg0) {
				getSelf().tell(task.createErrorResult(TaskResultEnum.Failed, "AsyncWorker creashed", null), getSelf());
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
					sender = getSender();
					processRequest();
					replyTask(CallbackType.created, task);
					break;
				}
			}
			else if (message instanceof InternalMessageType) {
				switch ((InternalMessageType) message) {

				case OPERATION_TIMEOUT:
					operationTimeout();
					break;
					
				case PROCESS_REQUEST_RESULT:
					processRequestResult();
					break;
					
				}
			} 
			else if (message instanceof TaskResult) {
				final TaskResult r = (TaskResult) message;
				handleHttpWorkerResponse(r);
			} 
			else {
				unhandled(message);
			}
		} 
		catch (Exception e) {
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

		asyncWorker = getContext().actorOf(new Props(new UntypedActorFactory() {
			private static final long serialVersionUID = 1L;

			public Actor create() {
				return createRequestWorker(task);
			}
		}));
		
		asyncWorker.tell(WorkerMessageType.PROCESS_REQUEST, getSelf());
		asyncPollWorkers.add(asyncWorker);

		// To handle cases where this operation takes extremely long, schedule a 'timeout' message to be sent to us
		if (task.getExecOptions().hasTimeout()) {
			timeoutDuration = Duration.create(task.getExecOptions().getTimeOutAt(), TimeUnit.MILLISECONDS);
			timeoutMessageCancellable = getContext().system().scheduler()
					.scheduleOnce(timeoutDuration, getSelf(), InternalMessageType.OPERATION_TIMEOUT, getContext().system().dispatcher());
		}


	}

	private final void processRequestResult() {
		
		if (curResult.getStatus().isSuccess()) {
			
			replyTask(CallbackType.submitted, task);
			TaskResult realResult = processRequestResult(task, curResult);
			
			if (realResult != null && realResult.getStatus().isComplete()) 
			{
				reply(realResult);
			}
		}
		else if (curResult.getStatus().isAnyError()) {
			retry(curResult.getMsg(), curResult.getStackTrace());
		}
		
	}
	
	private final void handleHttpWorkerResponse(TaskResult r) throws Exception {
		
		if (r.getStatus().isAnyError()) {
			retry(r.getMsg(), r.getStackTrace());
		} 
		else if (r.getStatus().isComplete()) {
			curResult = r;
			getSelf().tell(InternalMessageType.PROCESS_REQUEST_RESULT, getSelf());
		}
		
	}
	
	private final void operationTimeout() {
		if (asyncWorker != null && !asyncWorker.isTerminated()) {
			asyncWorker.tell(PoisonPill.getInstance(), null);
		}
		retry(String.format("OperationTimedout, took more than %d seconds", task.getExecOptions().getTimeoutInMs()/1000), null);
	}

	private final void retry(final String errorMessage, final String stackTrace) {
		// Error response
		if (tryCount++ < task.getExecOptions().getMaxRetry()) {
			retryMessageCancellable = getContext()
					.system()
					.scheduler()
					.scheduleOnce(Duration.create(task.getExecOptions().getRetryDelayMs(), TimeUnit.MILLISECONDS), getSelf(),
							"PROCESS_REQUEST", getContext().system().dispatcher());
		} 
		else {
			// We have exceeded all retries, reply back to sender
			// with the error message
			replyError(TaskResultEnum.Failed, errorMessage, stackTrace);
		}
	}

	@Override
	public void postStop() {
		if (retryMessageCancellable != null && !retryMessageCancellable.isCancelled()) {
			retryMessageCancellable.cancel();
		}
		if (timeoutMessageCancellable != null && !timeoutMessageCancellable.isCancelled()) {
			timeoutMessageCancellable.cancel();
		}
		for (final ActorRef asyncWorker : asyncPollWorkers) {
			if (asyncWorker != null && !asyncWorker.isTerminated()) {
				asyncWorker.tell(PoisonPill.getInstance(), null);
			}
		}
	}
	
	private final void replyTask(CallbackType type, Task task) {
		sender.tell(new WorkerMessage(type, task, null), getSelf());
	}

	private final void replyError(TaskResultEnum state, String msg, String stackTrace) {
		if (!sentReply) {
			TaskResult tr = task.createErrorResult(state, msg, stackTrace);
			sender.tell(new WorkerMessage(CallbackType.taskresult, task, tr), getSelf());
			sentReply = true;
		}

		// Self-terminate
		getSelf().tell(PoisonPill.getInstance(), null);
	}
	
	private final void reply(final TaskResult taskResult) {
		if (!sentReply) {
			sender.tell(new WorkerMessage(CallbackType.taskresult, task, taskResult), getSelf());
			sentReply = true;
		}

		// Self-terminate
		getSelf().tell(PoisonPill.getInstance(), null);
	}

	@Override
	public SupervisorStrategy supervisorStrategy() {
		return supervisorStrategy;
	}
	

	/**
	 * process poll result
	 * @param result
	 * @return
	 */
	public abstract TaskResult processRequestResult(T task, TaskResult result);

	/**
	 * create poll process
	 * @param task
	 * @return
	 */
	public abstract Actor createRequestWorker(T task);
}
