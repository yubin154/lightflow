package org.lightj.example.session;

import java.io.IOException;

import org.lightj.session.FlowContext;
import org.lightj.task.ExecuteOption;
import org.lightj.task.MonitorOption;
import org.lightj.task.RuntimeTaskExecutionException;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;
import org.lightj.task.asynchttp.AsyncHttpTask;
import org.lightj.task.asynchttp.UrlTemplate;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

@SuppressWarnings("rawtypes")
public abstract class SimpleHttpAsyncPollTask<T extends FlowContext> extends AsyncHttpTask<T> {
	
	static final String STATUS_TEMPLATE = "http://#host";
	
	private UrlTemplate params;
	
	public SimpleHttpAsyncPollTask(AsyncHttpClient client, ExecuteOption execOptions, MonitorOption monitorOption) 
	{
		super(client, execOptions, monitorOption);
		this.client = client;
	}
	
	public abstract UrlTemplate createHttpRequest();

	@Override
	public BoundRequestBuilder createRequest() {
		BoundRequestBuilder builder = null;
		params = createHttpRequest();
		String url = params.getUrl();
		url = url.replace("#host", params.getHost());
		switch (params.getMethod()) {
		case GET:
			builder = client.preparePost(url);
			break;
		case POST:
			builder = client.preparePost(url);
			break;
		case PUT:
			builder = client.preparePut(url);
			break;
		case DELETE:
			builder = client.prepareDelete(url);
			break;
		default:
			break;	
		}
		if (builder == null) {
			throw new RuntimeTaskExecutionException("Failed to build agent request, unknown method");
		}
		return builder;
	}

	@Override
	public TaskResult onComplete(Response response) {
		TaskResult res = createTaskResult(TaskResultEnum.Success, "");
		final String pollUrl = STATUS_TEMPLATE.replace("#host", params.getHost());
		this.setExtTaskUuid(pollUrl);
		AsyncHttpTask pollTask = createPollTask(pollUrl);
		res.setRealResult(pollTask);
		return res;
	}

	@Override
	public TaskResult onThrowable(Throwable t) {
		return this.createErrorResult(TaskResultEnum.Failed, t.getMessage(), t);
	}
	
	/** create poll task */
	private AsyncHttpTask createPollTask(final String pollUrl) {
		
		return new AsyncHttpTask<FlowContext>(client) {

			@Override
			public BoundRequestBuilder createRequest() {
				return client.prepareGet(pollUrl);
			}

			@Override
			public TaskResult onComplete(Response response) {
				try {
					return createTaskResult(TaskResultEnum.Success, response.getResponseBody());
				} catch (IOException e) {
					return createTaskResult(TaskResultEnum.Failed, e.getMessage());
				}
			}

			@Override
			public TaskResult onThrowable(Throwable t) {
				return this.createErrorResult(TaskResultEnum.Failed, t.getMessage(), t);
			}
		};

	}
	
}

