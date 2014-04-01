package org.lightj.task.asynchttp;

import java.io.IOException;
import java.util.Map.Entry;

import org.lightj.task.ExecuteOption;
import org.lightj.task.Task;
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
	
	/** optional response processor */
	protected IHttpProcessor resProcessor;
	
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
	
	public IHttpProcessor getResProcessor() {
		return resProcessor;
	}

	public void setResProcessor(IHttpProcessor resProcessor) {
		this.resProcessor = resProcessor;
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
		
		try {
			return resProcessor != null ? resProcessor.processHttpReponse(this, response) : 
				new HttpResponseProcessor().processHttpReponse(this, response);
		} catch (IOException e) {
			return this.failed(e.getMessage(), e);
		}
		
	}

	@Override
	public TaskResult onThrowable(Throwable t) {
		return this.failed(TaskResultEnum.Failed, t.getMessage(), t);
	}
	
	/**
	 * default http response processor
	 * @author biyu
	 *
	 */
	static class HttpResponseProcessor implements IHttpProcessor {

		@Override
		public TaskResult processHttpReponse(Task task, Response response)
				throws IOException {
			TaskResult res = null;
			try {
				int sCode = response.getStatusCode();
				String statusCode = Integer.toString(sCode);
				if (sCode >= 200 && sCode < 300) {
					res = task.succeeded();
				}
				else {
					res = task.hasResult(TaskResultEnum.Failed, statusCode);
				}

				res.setRawResult(new SimpleHttpResponse(response));
			
			} catch (Throwable t) {
				res = task.failed(t.getMessage(), t);
			}
			return res;
		}
		
	}
	
}

