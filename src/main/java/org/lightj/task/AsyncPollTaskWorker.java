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
 * Provides abstraction of an operation with polling.
 * 
 *  @author biyu
 */
@SuppressWarnings("rawtypes")
public abstract class AsyncPollTaskWorker<T extends Task> extends UntypedActor implements IWorker {
	
	/** task, poll option */
	private final T task;
	private final MonitorOption monitorOptions;
	
	/** intermediate result */
	private TaskResult taskSubmissionResult;
	private TaskResult taskPollResult;
	private TaskResult curResult;
	private boolean requestDone;
	
	/** runtime */
	private final SupervisorStrategy supervisorStrategy;
	private final List<ActorRef> asyncPollWorkers = new ArrayList<ActorRef>();
	private ActorRef asyncWorker = null;

	/** requester */
	private ActorRef sender = null;
	private boolean sentReply = false;
	private volatile int tryCount = 0;
	private volatile int pollTryCount = 0;
	
	/** any unfinished business */
	private Cancellable timeoutMessageCancellable = null;
	private Cancellable retryMessageCancellable = null;
	private Cancellable pollMessageCancellable = null;
	private FiniteDuration timeoutDuration = null;

	/** internal message type */
	private enum InternalMessageType {
		POLL_PROGRESS, OPERATION_TIMEOUT, PROCESS_REQUEST_RESULT, PROCESS_POLL_RESULT
	}

	/**
	 * constructor with single task
	 * @param task
	 * @param monitorOptions
	 * @param listener
	 */
	public AsyncPollTaskWorker(final T task, final MonitorOption monitorOptions) {
		super();
		
		this.task = task;
		this.monitorOptions = monitorOptions;
		
		// Other initialization
		this.supervisorStrategy = new OneForOneStrategy(0, Duration.Inf(), new Function<Throwable, Directive>() {
			public Directive apply(Throwable arg0) {
				getSelf().tell(task.createErrorResult(TaskResultEnum.Failed, "AsyncPollWorker crashed", null), getSelf());
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
				case POLL_PROGRESS:
					pollProgress();
					break;

				case OPERATION_TIMEOUT:
					operationTimeout();
					break;
					
				case PROCESS_REQUEST_RESULT:
					processRequestResult();
					break;
					
				case PROCESS_POLL_RESULT:
					processPollResult();
					break;
				}
			} 
			else if (message instanceof TaskResult) {
				final TaskResult r = (TaskResult) message;
				handleWorkerResponse(r);
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
	private final void processRequest() 
	{
		asyncWorker = getContext().actorOf(new Props(new UntypedActorFactory() {
			private static final long serialVersionUID = 1L;

			public Actor create() {
				return createRequestWorker(task);
			}
			
		}));
		
		asyncWorker.tell(WorkerMessageType.PROCESS_REQUEST, getSelf());
		asyncPollWorkers.add(asyncWorker);

		// To handle cases where this operation takes extremely long, schedule a 'timeout' message to be sent to us
		if (task.getExecOptions().hasTimeout() && timeoutMessageCancellable == null) {
			timeoutDuration = Duration.create(task.getExecOptions().getTimeoutInMs(), TimeUnit.MILLISECONDS);
			timeoutMessageCancellable = getContext().system().scheduler()
					.scheduleOnce(timeoutDuration, getSelf(), InternalMessageType.OPERATION_TIMEOUT, getContext().system().dispatcher());
		}

	}

	private final void processRequestResult() {
		
		if (curResult.getStatus().isSuccess()) {
			taskSubmissionResult = curResult;
			requestDone = true;
			replyTask(CallbackType.submitted, task);
			getSelf().tell(InternalMessageType.POLL_PROGRESS, getSelf());
		}
		else {
			retry(curResult.getStatus(), curResult.getMsg(), curResult.getStackTrace());
		}
		
	}
	
	private final void pollProgress() {
		
		final ActorRef pollWorker = getContext().actorOf(new Props(new UntypedActorFactory() {
			private static final long serialVersionUID = 1L;

			public Actor create() {
				return createPollWorker(task, taskSubmissionResult);
			}
			
		}));

		pollWorker.tell(WorkerMessageType.PROCESS_REQUEST, getSelf());
		
	}
	
	private final void processPollResult() {

		if (curResult.getStatus().isSuccess()) {
			taskPollResult = curResult;
			
			TaskResult taskResult = processPollResult(taskPollResult);
			boolean scheduleNextPoll = (taskResult == null || !taskResult.isComplete()); 
			
			if (scheduleNextPoll) {
				// Schedule next poll
				pollMessageCancellable = getContext()
						.system()
						.scheduler()
						.scheduleOnce(Duration.create(monitorOptions.getMonitorIntervalMs(), TimeUnit.MILLISECONDS), getSelf(),
								InternalMessageType.POLL_PROGRESS, getContext().system().dispatcher());
			}
			else {
				reply(taskResult);
			}
			
		}
		else {
			retry(curResult.getStatus(), curResult.getMsg(), curResult.getStackTrace());
		}
		
	}

	private final void handleWorkerResponse(TaskResult r) throws Exception {
		
		if (r.getStatus().isAnyError()) {
			retry(r.getStatus(), r.getMsg(), r.getStackTrace());
		} 
		else {
			curResult = r;
			getSelf().tell(requestDone ? InternalMessageType.PROCESS_POLL_RESULT : InternalMessageType.PROCESS_REQUEST_RESULT, getSelf());
		}
		
	}
	
	private final void operationTimeout() {
		if (asyncWorker != null && !asyncWorker.isTerminated()) {
			asyncWorker.tell(PoisonPill.getInstance(), null);
		}
		retry(TaskResultEnum.Timeout, String.format("OperationTimedout, took more than %d seconds", task.getExecOptions().getTimeoutInMs()/1000), null);
	}

	private final void retry(final TaskResultEnum status, final String errorMessage, final String stackTrace) {
		// Error response
		boolean retried = false;
		if (requestDone) {
			if (pollTryCount++ < monitorOptions.getMaxRetry()) {
				retryMessageCancellable = getContext()
						.system()
						.scheduler()
						.scheduleOnce(Duration.create(monitorOptions.getRetryDelayMs(), TimeUnit.MILLISECONDS), getSelf(),
								InternalMessageType.POLL_PROGRESS, getContext().system().dispatcher());
				retried = true;
			} 
		}
		else {
			if (tryCount++ < task.getExecOptions().getMaxRetry()) {
				retryMessageCancellable = getContext()
						.system()
						.scheduler()
						.scheduleOnce(Duration.create(task.getExecOptions().getRetryDelayMs(), TimeUnit.MILLISECONDS), getSelf(),
								WorkerMessageType.PROCESS_REQUEST, getContext().system().dispatcher());
				retried = true;
			} 
		}
		if (!retried) {
			// We have exceeded all retries, reply back to sender
			// with the error message
			replyError(status, "retry limit reached, last error: " + errorMessage, stackTrace);
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
		if (pollMessageCancellable != null && !pollMessageCancellable.isCancelled()) {
			pollMessageCancellable.cancel();
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
	 * create poll request based on result of the original request
	 * @param task
	 * @param result
	 * @return
	 */
	public abstract Actor createPollWorker(T task, TaskResult result);
	
	/**
	 * process poll result
	 * @param result
	 * @return
	 */
	public abstract TaskResult processPollResult(TaskResult result);

	/**
	 * create poll process
	 * @param task
	 * @return
	 */
	public abstract Actor createRequestWorker(T task);
}
