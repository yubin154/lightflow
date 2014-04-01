package org.lightj.task.asynchttp;

import java.util.Map.Entry;

import org.lightj.task.ExecuteOption;
import org.lightj.task.TaskExecutionRuntimeException;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;

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

public class SimpleHttpTask extends AsyncHttpTask {
	
	static int MSG_CONTENT_LEN = 2000; 
	
	/** keep transient materialized req and poll req */
	protected UrlRequest req;
	
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
	
	/**
	 * build a ning http request builder
	 * @param req
	 * @return
	 */
	protected BoundRequestBuilder buildHttpRequest(UrlRequest req) {
		BoundRequestBuilder builder = null;
		if (this.hasGlobalContext()) {
			req.setGlobalContext(this.getGlobalContext());
		}
		String url = req.generateUrl();
		this.setExtTaskUuid(url);
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
			for (Entry<String, String> param : req.generateParameters().entrySet()) {
				if (req.getMethod() == HttpMethod.GET) {
					builder.addQueryParameter(param.getKey(), param.getValue());
				}
				else if (req.getMethod() == HttpMethod.POST) {
					builder.addParameter(param.getKey(), param.getValue());
				}
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
				res = this.succeeded();
			}
			else {
				res = hasResult(TaskResultEnum.Failed, statusCode);
			}

			res.setRawResult(new SimpleHttpResponse(response));
		
		} catch (Throwable t) {
			res = this.failed(t.getMessage(), t);
		}
		return res;
	}

	@Override
	public TaskResult onThrowable(Throwable t) {
		return this.failed(TaskResultEnum.Failed, t.getMessage(), t);
	}
	
}

