package org.lightj.task.asynchttp;

import org.lightj.session.FlowContext;
import org.lightj.task.ExecuteOption;
import org.lightj.task.Task;
import org.lightj.task.TaskResult;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;

public abstract class HttpTask<T extends FlowContext> extends Task<T> {
	
	protected AsyncHttpClient client;
	public HttpTask(AsyncHttpClient client, 
			ExecuteOption execOptions) 
	{
		super(execOptions);
		this.client = client;
	}

	public HttpTask(AsyncHttpClient client) 
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
	
	@Override
	public String getTaskDetail() {
		return String.format("Request http");
	}
	
	public abstract BoundRequestBuilder createRequest();
	
	public abstract TaskResult processRequestResult(Response response);

}

