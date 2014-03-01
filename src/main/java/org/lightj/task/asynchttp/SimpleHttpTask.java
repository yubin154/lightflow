package org.lightj.task.asynchttp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lightj.session.FlowContext;
import org.lightj.task.ExecuteOption;
import org.lightj.task.MonitorOption;
import org.lightj.task.RuntimeTaskExecutionException;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

public class SimpleHttpTask<T extends FlowContext> extends AsyncHttpTask<T> {
	
	/** keep transient materialized req and poll req */
	protected UrlRequest req;
	
	/** if populated, template variable will be populated with values from context at runtime */
	protected Map<String, String> templateVariableFromContext;
	
	/** constructor */
	public SimpleHttpTask(AsyncHttpClient client, ExecuteOption execOptions, MonitorOption monitorOption) 
	{
		super(client, execOptions, monitorOption);
	}
	
	public UrlRequest getReq() {
		return req;
	}
	public void setReq(UrlRequest req) {
		this.req = req;
	}
	
	public void addTemplateVariableFromContext(String variableName, String contextVariableName) {
		if (templateVariableFromContext == null) {
			templateVariableFromContext = new HashMap<String, String>();
		}
		templateVariableFromContext.put(variableName, contextVariableName);
	}
	
	/**
	 * build a ning http request builder
	 * @param req
	 * @return
	 */
	protected BoundRequestBuilder buildHttpRequest(UrlRequest req) {
		BoundRequestBuilder builder = null;
		if (templateVariableFromContext != null) {
			for (Entry<String, String> entry : templateVariableFromContext.entrySet()) {
				String variable = entry.getKey();
				Object value = context.getValueByName(entry.getValue());
				req.addTemplateValue(variable, value!=null ? value.toString() : "");
			}
		}
		String url = req.generateUrl();
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
			throw new RuntimeTaskExecutionException("Failed to build http request, unknown method");
		}
		else {
			for (Entry<String, String> header : req.generateHeaders().entrySet()) {
				builder.addHeader(header.getKey(), header.getValue());
			}
			if (req.hasBody()) {
				builder.setBody(req.generateBody());
			}
		}
		return builder;
	}
	
	@Override
	public BoundRequestBuilder createRequest() {
		
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

