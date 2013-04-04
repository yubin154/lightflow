package org.lightj.task.asynchttp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import org.lightj.task.IWorker.WorkerMessageType;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;
import org.lightj.util.Log4jProxy;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.PoisonPill;
import akka.actor.UntypedActor;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

/**
 * actor abstraction of running an asynchronous http task
 * 
 * @author biyu
 *
 */
@SuppressWarnings({"rawtypes"})
public class HttpWorker extends UntypedActor {
	
	/** task */
	private final HttpTask task;
	
	private ActorRef sender = null;
	private ListenableFuture responseFuture = null;
	private boolean sentReply = false;
	private volatile int tryCount = 0;
	
	/** result */
	private TaskResult result = null;
	private Throwable cause;

	/** unfinished work */
	private Cancellable timeoutMessageCancellable = null;
	private Cancellable retryMessageCancellable = null;
	private FiniteDuration timeoutDuration = null;

	private static Log4jProxy logger = null;

	public enum InternalMessageType {
		PROCESS_ON_RESPONSE, PROCESS_ON_EXCEPTION, PROCESS_ON_TIMEOUT
	}

	public HttpWorker(HttpTask task) {
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

				case PROCESS_ON_EXCEPTION:
					final StringWriter sw = new StringWriter();
					final PrintWriter pw = new PrintWriter(sw);
					cause.printStackTrace(pw);
					retry(task.createErrorResult(TaskResultEnum.Failed, cause.toString(), sw.toString()));

					break;

				case PROCESS_ON_TIMEOUT:
					reply(task.createTaskResult(TaskResultEnum.Timeout, "RequestTimeOut"));

					break;

				case PROCESS_ON_RESPONSE:
					processResponse();
					
					break;

				}
			} 
			else {
				unhandled(message);
			}
		} catch (Exception e) {
			this.cause = e;
			getSelf().tell(InternalMessageType.PROCESS_ON_EXCEPTION, getSelf());
		}
	}
	
	private final void processRequest() throws IOException {
		sender = getSender();

		// Submit NIO Request
		BoundRequestBuilder request = task.createRequest();
		responseFuture = request.execute(new HttpAsyncHandler());

		if (task.getExecOptions().hasTimeout()) {
			timeoutDuration = Duration.create(task.getConfig().getRequestTimeoutInMs()/1000 + 2, TimeUnit.SECONDS);

			// To handle cases where nio response never comes back, we
			// schedule a 'timeout' message to be sent to us 2 seconds
			// after NIO's SO_TIMEOUT
			timeoutMessageCancellable = getContext().system().scheduler()
					.scheduleOnce(timeoutDuration, getSelf(), InternalMessageType.PROCESS_ON_TIMEOUT, getContext().system().dispatcher());
		}

	}
	
	private final void processResponse() {
		
		if (result == null || result.getStatus().isAnyError()) {
			retry(result);
		}
		else if (result.getStatus().isSuccess()) {
			reply(task.createTaskResult(result, TaskResultEnum.Success, null));
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
			logger = Log4jProxy.getInstance(HttpWorker.class);
		}

		return logger;
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

	/**
	 * handler wrapper
	 * 
	 * @author biyu
	 *
	 */
	class HttpAsyncHandler extends AsyncCompletionHandler<TaskResult> {
		@Override
		public TaskResult onCompleted(Response response) throws Exception {
			TaskResult res = task.processRequestResult(response);
			HttpWorker.this.result = res;  
			getSelf().tell(InternalMessageType.PROCESS_ON_RESPONSE, getSelf());
			return res;
		}

	    public void onThrowable(Throwable t) {
			HttpWorker.this.cause = t;
			getSelf().tell(InternalMessageType.PROCESS_ON_EXCEPTION, getSelf());
	    }

	}
}
