package org.lightj.task;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.lightj.task.asynchttp.AsyncHttpTask;
import org.lightj.util.ActorUtil;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.UntypedActorFactory;
import akka.pattern.Patterns;
import akka.util.Timeout;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

@SuppressWarnings("rawtypes")
public class TaskWorkerUtil {

	public static void runTasks(
			final ITaskListener listener,
			final UntypedActorFactory actorFactory,
			final BatchOption batchOption, 
			final ExecutableTask... tasks) throws Exception 
	{
		final BatchTask batchTask = new BatchTask(tasks);
		ActorRef batchWorker = ActorUtil.createActor(new UntypedActorFactory() {

			private static final long serialVersionUID = 1L;

			@Override
			public Actor create() throws Exception {
				return new BatchTaskWorker(batchTask, actorFactory, listener);
			}
		});
		final FiniteDuration timeout = Duration.create(10, TimeUnit.MINUTES);
		Patterns.ask(batchWorker, IWorker.WorkerMessageType.REPROCESS_REQUEST, new Timeout(timeout));

	}

	/**
	 * utility to create actor factory for async poll task
	 * 
	 * @param pollMonitor
	 * @return
	 */
	public static UntypedActorFactory createAsyncPollActorFactory() {
		return new UntypedActorFactory() {
			private static final long serialVersionUID = 1L;

			@Override
			public Actor create() throws Exception {
				return new AsyncPollTaskWorker<ExecutableTask>();
			}

		};
	}

	/**
	 * utility to create actor fatory for async task
	 * 
	 * @return
	 */
	public static UntypedActorFactory createAsyncActorFactory() {
		return new UntypedActorFactory() {
			private static final long serialVersionUID = 1L;

			@Override
			public Actor create() throws Exception {
				return new AsyncTaskWorker<ExecutableTask>();
			}

		};
	}

	public static void main(String[] args) {
		try {
			TaskWorkerUtil.runTasks(
			new ITaskListener() {

				@Override
				public void taskSubmitted(Task task) {
				}

				@Override
				public void taskCreated(Task task) {
				}

				@Override
				public void taskCompleted(Task result) {
				}

				@Override
				public void handleTaskResult(Task task, TaskResult result) {
					System.out.println(result.getRealResult());
				}
			}, 
			createAsyncActorFactory(), 
			null, 
			new AsyncHttpTask(
					new AsyncHttpClient()) {

				@Override
				public BoundRequestBuilder createRequest() {
					return client.prepareGet("http://www.ebay.com");
				}

				@Override
				public TaskResult onComplete(Response response) {
					try {
						return createTaskResult(response.getResponseBody(),
								TaskResultEnum.Success, "");
					} catch (IOException e) {
						return createErrorResult(TaskResultEnum.Failed, e.getMessage(), e);
					}
				}

				@Override
				public TaskResult onThrowable(Throwable t) {
					return createErrorResult(TaskResultEnum.Failed, t.getMessage(), t);
				}

			});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
