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
import org.lightj.util.JsonUtil;
import org.lightj.util.StringUtil;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

@SuppressWarnings("rawtypes")
public abstract class AgentAsyncTask<T extends FlowContext> extends AsyncHttpTask<T> {
	
	static final String STATUS_TEMPLATE = "https://#host:12020#statusuri";
	
	private UrlTemplate params;
	
	public AgentAsyncTask(AsyncHttpClient client, ExecuteOption execOptions, MonitorOption monitorOption) 
	{
		super(client, execOptions, monitorOption);
		this.client = client;
	}
	
	public abstract UrlTemplate createAgentRequest();

	@Override
	public BoundRequestBuilder createRequest() {
		BoundRequestBuilder builder = null;
		params = createAgentRequest();
		String url = params.getUrl();
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
		if (builder != null) {
			builder.addHeader("Authorization", "Basic YWdlbnQ6dG95YWdlbnQ=")
			.addHeader("content-type", "application/json")
			.addHeader("AUTHZ_TOKEN", "donoevil");
			if (!StringUtil.isNullOrEmpty(params.getBody())) {
				builder.setBody(params.getBody());
			}
			return builder;
		}
		else {
			throw new RuntimeTaskExecutionException("Failed to build agent request, unknown method");
		}
	}

	@Override
	public TaskResult onComplete(Response response) {
		TaskResult res = createTaskResult(TaskResultEnum.Success, "");
		try {
			res.setRealResult(response.getResponseBody());
			AgentStatus aStatus = JsonUtil.decode((String) res.getRealResult(), AgentStatus.class);
			final String pollUrl = STATUS_TEMPLATE.replace("#host", params.getHost()).replace("#statusuri", aStatus.getStatus());
			this.setExtTaskUuid(pollUrl);
			AsyncHttpTask pollTask = createPollTask(pollUrl);
			res.setRealResult(pollTask);
		} catch (IOException e) {
			return createErrorResult(TaskResultEnum.Failed, e.getMessage(), e);
		}
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
				TaskResult res = null;
				try {
					AgentStatus aStatus = JsonUtil.decode((String) response.getResponseBody(), AgentStatus.class);
					if (aStatus.getProgress() == 100) {
						if (aStatus.getError() == 0) {
							res = createTaskResult(TaskResultEnum.Success, aStatus.getStatus());
						}
						else {
							res = createTaskResult(TaskResultEnum.Failed, aStatus.getErrorMsg());
						}
						res.setRealResult(aStatus);
					}
					else {
						res = createTaskResult(TaskResultEnum.Running, null);
						res.setRealResult(this);
					}
					
				} catch (IOException e) {
					return createErrorResult(TaskResultEnum.Failed, e.getMessage(), e);
				}
				return res;
			}

			@Override
			public TaskResult onThrowable(Throwable t) {
				return this.createErrorResult(TaskResultEnum.Failed, t.getMessage(), t);
			}
		};

	}
	
	public static class AgentRequestParams {
		public HttpMethod httpMethod;
		public String host;
		public String uri;
		public String body;
		public AgentRequestParams(HttpMethod httpMethod, String host, String uri, String body) {
			this.httpMethod = httpMethod;
			this.host = host;
			this.uri = uri;
			this.body = body;
		}
		public AgentRequestParams(HttpMethod httpMethod, String host, String uri) {
			this(httpMethod, host, uri, null);
		}
	}

}

