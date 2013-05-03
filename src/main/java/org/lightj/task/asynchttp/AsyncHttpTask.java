package org.lightj.task.asynchttp;

import java.io.IOException;

import org.lightj.session.FlowContext;
import org.lightj.task.ExecutableTask;
import org.lightj.task.ExecuteOption;
import org.lightj.task.TaskExecutionException;
import org.lightj.task.TaskResult;

import akka.actor.ActorRef;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;

public abstract class AsyncHttpTask<T extends FlowContext> extends ExecutableTask<T> {
	
	protected AsyncHttpClient client;
	private String targetUrl;

	public AsyncHttpTask(AsyncHttpClient client, 
			ExecuteOption execOptions) 
	{
		super(execOptions);
		this.client = client;
	}

	public AsyncHttpTask(AsyncHttpClient client) 
	{
		super();
		this.client = client;
	}

	public AsyncHttpClient getClient() {
		return client;
	}
	public AsyncHttpClientConfig getConfig() {
		return client.getConfig();
	}
	
	public String toString() {
		return String.format("Request http %s", targetUrl);
	}

	public String getTargetUrl() {
		return targetUrl;
	}
	public void setTargetUrl(String targetUrl) {
		this.targetUrl = targetUrl;
	}

	@Override
	public TaskResult execute(ActorRef executingActor) throws TaskExecutionException {
		BoundRequestBuilder request = createRequest();
		targetUrl = request.toString();
		try {
			request.execute(new HttpAsyncHandler(this, executingActor));
		} catch (IOException e) {
			throw new TaskExecutionException(e);
		}
		return null;
	}
	
	/**
	 * create request
	 * @return
	 */
	public abstract BoundRequestBuilder createRequest();

	/**
	 * process request result
	 * @param result
	 * @return
	 */
	public abstract TaskResult onComplete(Response response);

	/**
	 * handle error
	 * @param t
	 * @return
	 */
	public abstract TaskResult onThrowable(Throwable t);

	/**
	 * handler wrapper
	 * 
	 * @author biyu
	 *
	 */
	@SuppressWarnings("rawtypes")
	static class HttpAsyncHandler extends AsyncCompletionHandler<TaskResult> {
		private final ActorRef executingActor;
		private final AsyncHttpTask task;
		public HttpAsyncHandler(AsyncHttpTask task, ActorRef executingActor) {
			this.task = task;
			this.executingActor = executingActor;
		}
		@Override
		public TaskResult onCompleted(Response response) throws Exception {
			TaskResult res = task.onComplete(response);
			executingActor.tell(res, null);
			return res;
		}

	    public void onThrowable(Throwable t) {
	    	TaskResult res = task.onThrowable(t);
			executingActor.tell(res, null);
	    }

	}
}

