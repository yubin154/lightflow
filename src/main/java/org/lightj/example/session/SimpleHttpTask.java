package org.lightj.example.session;

import java.io.IOException;
import java.util.Map.Entry;

import org.lightj.session.FlowContext;
import org.lightj.task.ExecuteOption;
import org.lightj.task.MonitorOption;
import org.lightj.task.RuntimeTaskExecutionException;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;
import org.lightj.task.asynchttp.AsyncHttpTask;
import org.lightj.task.asynchttp.UrlRequest;
import org.lightj.task.asynchttp.UrlTemplate;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

public abstract class SimpleHttpTask<T extends FlowContext> extends AsyncHttpTask<T> {
	
	/** request template */
	private UrlTemplate reqTemplate;
	
	/** keep transient materialized req and poll req */
	private UrlRequest req;
	
	/** constructor */
	public SimpleHttpTask(AsyncHttpClient client, ExecuteOption execOptions, MonitorOption monitorOption) 
	{
		super(client, execOptions, monitorOption);
		this.client = client;
	}
	
	public UrlTemplate getReqTemplate() {
		return reqTemplate;
	}
	public void setReqTemplate(UrlTemplate reqTemplate) {
		this.reqTemplate = reqTemplate;
	}

	public void setHttpParams(UrlTemplate reqTemplate) {
		this.reqTemplate = reqTemplate;
	}
	
	public abstract UrlRequest createRequest(UrlTemplate reqTemplate);
	

	/**
	 * build a ning http request builder
	 * @param req
	 * @return
	 */
	private BoundRequestBuilder buildHttpRequest(UrlRequest req) {
		BoundRequestBuilder builder = null;
		UrlRequest realReq = createRequest(reqTemplate);
		String url = realReq.generateUrl();
		switch (req.getMethod()) {
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
		else {
			for (Entry<String, String> header : req.generateHeaders().entrySet()) {
				builder.addHeader(header.getKey(), header.getValue());
			}
			if (req.getBody() != null) {
				builder.setBody(req.generateBody());
			}
		}
		return builder;
	}
	
	@Override
	public BoundRequestBuilder createRequest() {
		
		req = createRequest(reqTemplate);
		return buildHttpRequest(req);
		
	}

	@Override
	public TaskResult onComplete(Response response) {
		TaskResult res = null;
		String statusCode = Integer.toString(response.getStatusCode());
		if (statusCode.matches("2[0-9][0-9]")) {
			res = createTaskResult(TaskResultEnum.Success, statusCode);
		}
		else {
			res = createTaskResult(TaskResultEnum.Failed, statusCode);
		}
		try {
			res.setRealResult(response.getResponseBody());
		} catch (IOException e) {
			res.setRealResult(e.getMessage());
		}
		return res;
	}

	@Override
	public TaskResult onThrowable(Throwable t) {
		return this.createErrorResult(TaskResultEnum.Failed, t.getMessage(), t);
	}
	
}

