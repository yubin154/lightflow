package org.lightj.task.asynchttp;

import java.io.IOException;

import org.lightj.session.FlowContext;
import org.lightj.task.ExecutableTask;
import org.lightj.task.ExecuteOption;
import org.lightj.task.MonitorOption;
import org.lightj.task.TaskExecutionException;
import org.lightj.task.TaskResult;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;

/**
 * execute a http call via Ning asynchronous http client
 * @author biyu
 *
 * @param <T>
 */
public abstract class AsyncHttpTask<T extends FlowContext> extends ExecutableTask<T> {
	
	/** http client */
	protected AsyncHttpClient client;
	/** target url */
	private String targetUrl;

	public AsyncHttpTask(AsyncHttpClient client, ExecuteOption execOptions, MonitorOption monitorOption) 
	{
		super(execOptions, monitorOption);
		this.client = client;
	}

	public AsyncHttpTask(AsyncHttpClient client, ExecuteOption execOptions) 
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
	public TaskResult execute() throws TaskExecutionException {
		BoundRequestBuilder request = createRequest();
		targetUrl = request.build().getUrl();
		try {
			request.execute(new HttpAsyncHandler(this));
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
		private final AsyncHttpTask task;
		public HttpAsyncHandler(AsyncHttpTask task) {
			this.task = task;
		}
		@Override
		public TaskResult onCompleted(Response response) throws Exception {
			TaskResult res = task.onComplete(response);
			task.reply(res);
			return res;
		}

	    public void onThrowable(Throwable t) {
	    	TaskResult res = task.onThrowable(t);
	    	task.reply(res);
	    }

	}
}

