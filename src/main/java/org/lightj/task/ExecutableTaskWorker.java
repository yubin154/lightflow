package org.lightj.task;

import org.lightj.util.StringUtil;

import com.ning.http.client.ListenableFuture;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

/**
 * simple actor to execute a task
 * @author biyu
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ExecutableTaskWorker<T extends ExecutableTask> extends UntypedActor {
	
	/** logger */
//	private static Log4jProxy logger = Log4jProxy.getInstance(ExecutableTaskWorker.class);

	/** the task*/
	private T task;
	
	/** sender actor */
	private ActorRef sender = null;
	private ListenableFuture responseFuture = null;
	
	/**
	 * constructor
	 */
	public ExecutableTaskWorker() {}

	@Override
	public void onReceive(Object message) throws Exception {
		try {
			if (message instanceof ExecutableTask) {
				task = (T) message;
				processRequest();
			}			
			else if (message instanceof TaskResult) {
				final TaskResult r = (TaskResult) message;
				handleWorkerResponse(r);
			} 
			else {
				unhandled(message);
			}
		} catch (Exception e) {
			reply(task.createErrorResult(TaskResultEnum.Failed, e.getMessage(), StringUtil.getStackTrace(e)));
		}
	}
	
	private final void processRequest() throws TaskExecutionException {
		sender = getSender();

		TaskResult result = task.execute(getSelf());
		
		// synchronous execution with result
		if (result != null && result.getStatus().isComplete()) {
			reply(result);
		}
		
	}
	
	/**
	 * handle async result
	 * @param r
	 * @throws Exception
	 */
	private final void handleWorkerResponse(TaskResult r) throws Exception {
		
		if (r != null && r.getStatus().isComplete()) {
			reply(r);
		}
		
	}

	@Override
	public void postStop() {
		if (responseFuture != null && !responseFuture.isDone()) {
			responseFuture.cancel(true);
		}
	}
	
	/**
	 * send result to requester
	 * @param res
	 */
	private void reply(final TaskResult res) {
		if (!getContext().system().deadLetters().equals(sender)) {
			sender.tell(res, getSelf());
		}
	}
	
}
