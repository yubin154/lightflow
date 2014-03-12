package org.lightj.task.asynchttp;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lightj.session.FlowContext;
import org.lightj.task.ExecuteOption;
import org.lightj.task.TaskExecutionRuntimeException;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;
import org.lightj.util.StringUtil;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

/**
 * http task, using async http client
 * 
 * @author binyu
 *
 * @param <T>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SimpleHttpTask<T extends FlowContext> extends AsyncHttpTask<T> {
	
	static int MSG_CONTENT_LEN = 2000; 
	
	/** keep transient materialized req and poll req */
	protected UrlRequest req;
	
	/** if populated, template variable will be populated with values from context at runtime */
	protected Map<String, String> valueFromContext;
	
	/** constructor */
	public SimpleHttpTask(AsyncHttpClient client, ExecuteOption execOptions) 
	{
		super(client, execOptions);
	}
	
	public UrlRequest getReq() {
		return req;
	}
	public void setReq(UrlRequest req) {
		this.req = req;
	}
	
	public void addValueFromContext(String variableName, String contextVariableName) {
		if (valueFromContext == null) {
			valueFromContext = new HashMap<String, String>();
		}
		valueFromContext.put(variableName, contextVariableName);
	}	
	
	/**
	 * build a ning http request builder
	 * @param req
	 * @return
	 */
	protected BoundRequestBuilder buildHttpRequest(UrlRequest req) {
		BoundRequestBuilder builder = null;
		if (valueFromContext != null) {
			for (Entry<String, String> entry : valueFromContext.entrySet()) {
				String variable = entry.getKey();
				String value = context.<String>getValueByName(entry.getValue());
				req.addTemplateValue(variable, value);
			}
		}
		String url = req.generateUrl();
		switch (req.getMethod()) {
		case GET:
			builder = client.prepareGet(url);
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
			throw new TaskExecutionRuntimeException("Failed to build http request, unknown method");
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
		try {
			int sCode = response.getStatusCode();
			String statusCode = Integer.toString(sCode);
			if (sCode >= 200 && sCode < 300) {
				res = hasResult(TaskResultEnum.Success, statusCode);
			}
			else {
				res = hasResult(TaskResultEnum.Failed, statusCode);
			}

			res.setRealResult(response.getResponseBodyExcerpt(MSG_CONTENT_LEN));
		
		} catch (Throwable t) {
			res = this.hasResult(TaskResultEnum.Failed, StringUtil.getStackTrace(t, MSG_CONTENT_LEN));
		}
		return res;
	}

	@Override
	public TaskResult onThrowable(Throwable t) {
		return this.failed(TaskResultEnum.Failed, t.getMessage(), t);
	}

	/**
	 * make a copy
	 * @return
	 */
	public SimpleHttpTask makeCopy() {
		SimpleHttpTask another = new SimpleHttpTask(client, execOptions);
		another.req = this.req;
		another.valueFromContext = this.valueFromContext;
		return another;
	}
	
}

