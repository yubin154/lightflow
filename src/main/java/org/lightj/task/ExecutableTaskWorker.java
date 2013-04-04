package org.lightj.task;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import org.lightj.task.IWorker.WorkerMessageType;
import org.lightj.util.Log4jProxy;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.PoisonPill;
import akka.actor.UntypedActor;

import com.ning.http.client.ListenableFuture;

/**
 * simple actor to execute a synchronous task
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
public class ExecutableTaskWorker<T extends ExecutableTask> extends UntypedActor {
	
	/** the task*/
	private final T task;
	
	/** sender actor */
	private ActorRef sender = null;
	private boolean sentReply = false;
	private ListenableFuture responseFuture = null;
	
	/** any exception */
	private Throwable cause;
	
	/** any unfinished operation and their schedule */
	private Cancellable timeoutMessageCancellable = null;
	private Cancellable retryMessageCancellable = null;
	private FiniteDuration timeoutDuration = null;

	/** try count */
	private volatile int tryCount = 0;

	/** logger */
	private static Log4jProxy logger = null;

	/** internal message type for handling exception, result, and timeout */
	private enum InternalMessageType {
		PROCESS_ON_TIMEOUT, PROCESS_ON_EXCEPTION, PROCESS_RESULT
	}

	public ExecutableTaskWorker(T task) {
		this.task = task;
	}

	@Override
	public void onReceive(Object message) throws Exception {
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

				case PROCESS_ON_TIMEOUT:
					reply(TaskResultEnum.Timeout, "RequestTimeOut", null);

					break;
					
				case PROCESS_ON_EXCEPTION:
					final StringWriter sw = new StringWriter();
					final PrintWriter pw = new PrintWriter(sw);
					cause.printStackTrace(pw);
					retry(task.createErrorResult(TaskResultEnum.Failed, cause.toString(), sw.toString()));

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
		} catch (Exception e) {
			this.cause = e;
			getSelf().tell(InternalMessageType.PROCESS_ON_EXCEPTION, getSelf());
		}
	}
	
	private final void processRequest() {
		sender = getSender();

		// To handle cases where nio response never comes back, we
		// schedule a 'timeout' message to be sent to us 2 seconds
		// after NIO's SO_TIMEOUT
		if (task.getExecOptions().hasTimeout()) {
			timeoutDuration = Duration.create(task.getExecOptions()
					.getTimeOutAt(), TimeUnit.MILLISECONDS);
			timeoutMessageCancellable = getContext()
					.system()
					.scheduler()
					.scheduleOnce(timeoutDuration, getSelf(),
							InternalMessageType.PROCESS_ON_TIMEOUT,
							getContext().system().dispatcher());
		}

		TaskResult result = task.execute(getSelf());
		if (result != null) {
			if (result.getStatus().isAnyError()) {
				retry(result);
			} else if (result.getStatus().isComplete()) {
				reply(result);
			}
		}
	}
	
	private final void handleHttpWorkerResponse(TaskResult r) throws Exception {
		
		if (r.getStatus().isAnyError()) {
			retry(r);
		} 
		else if (r.getStatus().isComplete()) {
			reply(r);
		}
		
	}


	@Override
	public void postStop() {
		if (responseFuture != null && !responseFuture.isDone()) {
			responseFuture.cancel(true);
		}
		if (timeoutMessageCancellable != null && !timeoutMessageCancellable.isCancelled()) {
			timeoutMessageCancellable.cancel();
		}
		if (retryMessageCancellable != null && !retryMessageCancellable.isCancelled()) {
			retryMessageCancellable.cancel();
		}
	}
	
	public static Log4jProxy getLogger() {
		if (logger == null) {
			logger = Log4jProxy.getInstance(ExecutableTaskWorker.class);
		}

		return logger;
	}

	private final void retry(final TaskResult res) {
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
			reply(res);
		}
	}
	
	private void reply(final TaskResult res) {
		if (!sentReply) {
			if (!getContext().system().deadLetters().equals(sender)) {
				sender.tell(res, getSelf());
			}
			sentReply = true;
		}

		// Self-terminate
		getSelf().tell(PoisonPill.getInstance(), null);
	}
	

	private void reply(final TaskResultEnum state, final String errorMessage, final String stackTrace) {
		if (!sentReply) {
			final TaskResult res = task.createErrorResult(state, errorMessage, stackTrace);
			if (!getContext().system().deadLetters().equals(sender)) {
				sender.tell(res, getSelf());
			}

			sentReply = true;
		}

		// Self-terminate
		getSelf().tell(PoisonPill.getInstance(), null);
	}

}
