package org.lightj.task;

import java.util.concurrent.TimeUnit;

import org.lightj.task.WorkerMessage.CallbackType;

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
import akka.japi.Function;


/**
 * Provides abstraction of an operation with polling.
 * 
 *  @author biyu
 */
@SuppressWarnings({"unchecked"})
public class AsyncPollTaskWorker<T extends ExecutableTask> extends UntypedActor {
	
	/** task, poll option */
	private T task;
	
	/** intermediate result */
	private TaskResult curResult;
	private boolean requestDone;
	
	/** runtime */
	private final SupervisorStrategy supervisorStrategy;
	private ActorRef asyncWorker = null;
	private ActorRef pollWorker = null;
	private ExecutableTask pollTask = null;

	/** requester */
	private ActorRef sender = null;
	private volatile int tryCount = 0;
	private volatile int pollTryCount = 0;
	
	/** any unfinished business */
	private Cancellable retryMessageCancellable = null;
	private Cancellable pollMessageCancellable = null;

	/** any unfinished operation and their schedule */
	private Cancellable timeoutMessageCancellable = null;
	private FiniteDuration timeoutDuration = null;

	/** internal message type */
	private enum InternalMessageType {
		PROCESS_REQUEST, RETRY_REQUEST, PROCESS_ON_TIMEOUT, POLL_PROGRESS, PROCESS_REQUEST_RESULT, PROCESS_POLL_RESULT
	}

	/**
	 * constructor with single task
	 * @param task
	 * @param monitorOptions
	 * @param listener
	 */
	public AsyncPollTaskWorker() {
		super();
		
		// Other initialization
		this.supervisorStrategy = new OneForOneStrategy(0, Duration.Inf(), new Function<Throwable, Directive>() {
			public Directive apply(Throwable arg0) {
				getSelf().tell(task.failed(TaskResultEnum.Failed, "AsyncPollWorker crashed", arg0), getSelf());
				return SupervisorStrategy.stop();
			}
		});
	}

	@Override
	public void onReceive(Object message) throws Exception 
	{
		try {
			// This is original request from external sender
			if (message instanceof ExecutableTask) {
				
				task = (T) message;
				sender = getSender();
				if (tryCount == 0) {
					replyTask(CallbackType.created, task);
				}
				
				// task have initial delay, schedule it
				if (task.getExecOptions().getInitDelaySec() > 0) {
					retryMessageCancellable = getContext()
							.system()
							.scheduler()
							.scheduleOnce(Duration.create(task.getExecOptions().getInitDelaySec(), TimeUnit.SECONDS), getSelf(),
									InternalMessageType.PROCESS_REQUEST, getContext().system().dispatcher());
				} 
				// run right away
				else {
					processRequest();
				}
				
			}
			
			// Internal messages
			else if (message instanceof InternalMessageType) {
				
				switch ((InternalMessageType) message) {
				
				case PROCESS_REQUEST:
					processRequest();
					break;
					
				case RETRY_REQUEST:
					processRequest();
					break;

				case PROCESS_REQUEST_RESULT:
					processRequestResult();
					break;
					
				case POLL_PROGRESS:
					pollProgress();
					break;
					
				case PROCESS_POLL_RESULT:
					processPollResult();
					break;
				}
				
			}
			
			// task result
			else if (message instanceof TaskResult) {
				final TaskResult r = (TaskResult) message;
				handleWorkerResponse(r);
			}
			
			// something unexpected
			else {
				unhandled(message);
			}
		} 
		catch (Throwable e) {
			retry(TaskResultEnum.Failed, e.toString(), e);
		}
	}
	
	/**
	 * send request
	 */
	private final void processRequest() 
	{

		if (task.getMonitorOption() == null) {
			replyError(TaskResultEnum.Failed, "monitor option missing", null);
			return;
		}
		
		if (asyncWorker == null) {
			asyncWorker = getContext().actorOf(new Props(TaskModule.getExecutableTaskWorkerFactory()));
		}
		
		asyncWorker.tell(task, getSelf());

		// asynchronous, set timeout
		if (tryCount == 0 && task.getExecOptions().getTimeOutSec()>0) {
			timeoutDuration = Duration.create(task.getExecOptions().getTimeOutSec(), TimeUnit.SECONDS);
			timeoutMessageCancellable = getContext()
					.system()
					.scheduler()
					.scheduleOnce(timeoutDuration, getSelf(),
							InternalMessageType.PROCESS_ON_TIMEOUT,
							getContext().system().dispatcher());
		}

	}

	/**
	 * process request result
	 */
	private final void processRequestResult() {
		
		if (curResult.getStatus().isAnyError()) {
			retry(curResult.getStatus(), curResult.getMsg(), curResult.getStackTrace());
		}
		else if (curResult.getStatus().isComplete()) {
			requestDone = true;
			replyTask(CallbackType.submitted, task);
			if (curResult.getRawResult() instanceof ExecutableTask) {

				if (pollWorker == null) {
					pollWorker = getContext().actorOf(new Props(TaskModule.getExecutableTaskWorkerFactory()));
				}
				if (pollTask == null) {
					pollTask = (ExecutableTask) curResult.getRawResult();
				}

				// start polling
				pollProgress();

			}
			// we don't know how to poll, just return
			else {
				reply(curResult);
			}

		}
		
	}
	
	/**
	 * poll progress
	 */
	private final void pollProgress() {

		pollWorker.tell(pollTask, getSelf());
		
	}
	
	/**
	 * process polling result
	 */
	private final void processPollResult() {
		
		boolean scheduleNextPoll = false;
		if (curResult.getStatus().isAnyError()) {
			scheduleNextPoll = retry(curResult.getStatus(), curResult.getMsg(), curResult.getStackTrace());
		}
		else if (!curResult.getStatus().isComplete()) {
			scheduleNextPoll = true;
		}

		if (scheduleNextPoll) {
			// Schedule next poll
			pollMessageCancellable = getContext()
					.system()
					.scheduler()
					.scheduleOnce(Duration.create(task.getMonitorOption().getIntervalSec(), TimeUnit.SECONDS), getSelf(),
							InternalMessageType.POLL_PROGRESS, getContext().system().dispatcher());
		}
		else {
			reply(curResult);
		}
		
	}

	/**
	 * worker response
	 * @param r
	 * @throws Exception
	 */
	private final void handleWorkerResponse(TaskResult r) throws Exception {
		
		curResult = r;
		getSelf().tell(requestDone ? InternalMessageType.PROCESS_POLL_RESULT : InternalMessageType.PROCESS_REQUEST_RESULT, getSelf());
		
	}

	/**
	 * handle retry
	 * @param status
	 * @param errorMessage
	 * @param stackTrace
	 */
	private final boolean retry(final TaskResultEnum status, final String errorMessage, final Throwable stackTrace) {
		// Error response
		boolean retried = false;
		if (requestDone) {
			if (pollTryCount++ < task.getMonitorOption().getMaxRetry()) {
				// noop, scheduled poll is same as retry
				retried = true;
			} 
		}
		else {
			if (tryCount++ < task.getExecOptions().getMaxRetry()) {
				retryMessageCancellable = getContext()
						.system()
						.scheduler()
						.scheduleOnce(Duration.create(task.getExecOptions().getRetryDelaySec(), TimeUnit.SECONDS), getSelf(),
								InternalMessageType.RETRY_REQUEST, getContext().system().dispatcher());
				retried = true;
			} 
		}
		if (!retried) {
			// We have exceeded all retries, reply back to sender
			// with the error message
			replyError(status, (requestDone ? "request" : "poll") + " retry limit reached, last error: " + errorMessage, stackTrace);
		}
		return retried;
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
		if (asyncWorker != null && !asyncWorker.isTerminated()) {
			asyncWorker.tell(PoisonPill.getInstance(), null);
		}
		if (pollWorker != null && !pollWorker.isTerminated()) {
			pollWorker.tell(PoisonPill.getInstance(), null);
		}
	}
	
	/**
	 * send non result callbacks
	 * @param type
	 * @param task
	 */
	private final void replyTask(CallbackType type, Task task) {
		if (!getContext().system().deadLetters().equals(sender)) {
			sender.tell(new WorkerMessage(type, task, null), getSelf());
		}
	}

	/**
	 * send error result
	 * @param state
	 * @param msg
	 * @param stackTrace
	 */
	private final void replyError(TaskResultEnum state, String msg, Throwable stackTrace) {
		TaskResult tr = task.failed(state, msg, stackTrace);
		if (!getContext().system().deadLetters().equals(sender)) {
			sender.tell(new WorkerMessage(CallbackType.taskresult, task, tr), getSelf());
		}
	}
	
	/**
	 * send result
	 * @param taskResult
	 */
	private final void reply(final TaskResult taskResult) {
		if (!getContext().system().deadLetters().equals(sender)) {
			sender.tell(new WorkerMessage(CallbackType.taskresult, task, taskResult), getSelf());
		}
	}

	@Override
	public SupervisorStrategy supervisorStrategy() {
		return supervisorStrategy;
	}
	
}
