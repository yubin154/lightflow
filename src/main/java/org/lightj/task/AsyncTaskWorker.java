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
 * Provides abstraction of an business operation. The operation can include asynchronous callback.
 * @author binyu
 *
 * @param <T>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class AsyncTaskWorker<T extends ExecutableTask> extends UntypedActor {
	
	/** task */
	private T task;
	
	/** intermediate result */
	private TaskResult curResult;

	/** requester */
	private ActorRef sender = null;
	private volatile int tryCount = 0;

	/** runtime */
	private final SupervisorStrategy supervisorStrategy;

	/** unfinished work */
	private ActorRef asyncWorker = null;
	private Cancellable retryMessageCancellable = null;

	/** any unfinished operation and their schedule */
	private Cancellable timeoutMessageCancellable = null;
	private FiniteDuration timeoutDuration = null;

	/** internal message type for handling exception, result, and timeout */
	private enum InternalMessageType {
		PROCESS_ON_TIMEOUT
	}
	
	/** constructor */
	public AsyncTaskWorker() {
		super();

		// Other initialization
		this.supervisorStrategy = new OneForOneStrategy(0, Duration.Inf(), new Function<Throwable, Directive>() {
			public Directive apply(Throwable arg0) {
				getSelf().tell(task.failed(TaskResultEnum.Failed, "AsyncWorker creashed", arg0), getSelf());
				return SupervisorStrategy.stop();
			}
		});
	}

	@Override
	public void onReceive(Object message) throws Exception 
	{
		try {
			// original task
			if (message instanceof Task) {
				task = (T) message;
				sender = getSender();
				processRequest();
				if (tryCount == 0) {
					replyTask(CallbackType.created, task);
				}
			}
			// task result
			else if (message instanceof TaskResult) {
				final TaskResult r = (TaskResult) message;
				processRequestResult(r);
			} 
			// internal message for timeout
			else if (message instanceof InternalMessageType) {
				switch ((InternalMessageType) message) {

				case PROCESS_ON_TIMEOUT:
					reply(task.failed(TaskResultEnum.Timeout, "RequestTimeOut", null));

					break;
					
				}
			} 
			// something not expecting
			else {
				unhandled(message);
			}
		} 
		catch (Throwable e) {
			retry(task.failed(TaskResultEnum.Failed, e.toString(), e));
		}
	}
	
	/**
	 * send request
	 */
	private final void processRequest() {

		if (asyncWorker == null) {
			asyncWorker = getContext().actorOf(new Props(TaskModule.getExecutableTaskWorkerFactory()));
		}
		
		asyncWorker.tell(task, getSelf());
		
		// asynchronous, set timeout
		if (tryCount == 0 && task.getExecOptions().hasTimeout()) {
			timeoutDuration = Duration.create(task.getExecOptions().getTimeoutInMs(), TimeUnit.MILLISECONDS);
			timeoutMessageCancellable = getContext()
					.system()
					.scheduler()
					.scheduleOnce(timeoutDuration, getSelf(),
							InternalMessageType.PROCESS_ON_TIMEOUT,
							getContext().system().dispatcher());
		}

	}

	/**
	 * process result
	 * @param r
	 * @throws Exception
	 */
	private final void processRequestResult(TaskResult r) throws Exception {
		
		this.curResult = r;
		if (curResult.getStatus().isComplete()) {
			
			replyTask(CallbackType.submitted, task);
			TaskResult realResult = processRequestResult(task, curResult);
			
			if (realResult != null) {
				if (realResult.getStatus().isAnyError()) {
					retry(curResult);
				}
				else if (realResult.getStatus().isComplete()) 
				{
					reply(realResult);
				}
			}
		}

	}
	
	/**
	 * handle retry
	 * @param result
	 */
	private final void retry(TaskResult result) {
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
			reply(result);
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
		if (asyncWorker != null && !asyncWorker.isTerminated()) {
			asyncWorker.tell(PoisonPill.getInstance(), null);
		}
	}
	
	/**
	 * callback with task
	 * @param type
	 * @param task
	 */
	private final void replyTask(CallbackType type, Task task) {
		if (!getContext().system().deadLetters().equals(sender)) {
			sender.tell(new WorkerMessage(type, task, null), getSelf());
		}
	}

	/**
	 * callback with result
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
	

	/**
	 * process request result
	 * @param result
	 * @return
	 */
	public TaskResult processRequestResult(T task, TaskResult result) {
		return result;
	}

}
