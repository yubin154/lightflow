package org.lightj.task.asynchttp;

import org.lightj.task.ExecuteOption;
import org.lightj.task.MonitorOption;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

/**
 * http task with polling, using async http client
 * 
 * @author binyu
 *
 * @param <T>
 */
public class SimpleHttpAsyncPollTask extends SimpleHttpTask {
	
	/** pull request */
	private UrlRequest pollReq;
	
	/** handle specific polling logic */
	private IHttpPollProcessor pollProcessor;
	
	/** constructor */
	public SimpleHttpAsyncPollTask(
			AsyncHttpClient client, 
			ExecuteOption execOptions, 
			MonitorOption monitorOption, 
			IHttpPollProcessor pollProcessor) 
	{
		super(client, execOptions);
		this.monitorOption = monitorOption;
		this.pollProcessor = pollProcessor;
	}
	
	public void setHttpParams(UrlRequest req, UrlRequest pollReq) {
		this.req = req;
		this.pollReq = pollReq;
	}
	
	public IHttpPollProcessor getPollProcessor() {
		return pollProcessor;
	}

	public void setPollProcessor(IHttpPollProcessor pollProcessor) {
		this.pollProcessor = pollProcessor;
	}
	
	protected BoundRequestBuilder buildHttpRequest(UrlRequest req) {
		BoundRequestBuilder builder = super.buildHttpRequest(req);
		pollReq.putTemplateValuesIfNull(req.getTemplateValues());
		if (req.getGlobalConext()!=null) {
			pollReq.setGlobalContext(req.getGlobalContext());
		}
		return builder;
	}

	@Override
	public TaskResult onComplete(Response response) {
		TaskResult res = null;
		try {
			if (pollProcessor != null) {
				res = pollProcessor.preparePollTask(this, response, pollReq);
			}
			if (res == null) {
				int sCode = response.getStatusCode();
				String statusCode = Integer.toString(sCode);
				if (sCode >= 400) {
					res = this.hasResult(TaskResultEnum.Failed, statusCode);
				}
				else {
					res = this.succeeded();
				}
			}
			
			if (TaskResultEnum.Success == res.getStatus()) {
				AsyncHttpTask pollTask = createPollTask(pollReq);
				res.setRawResult(pollTask);
			}
			else {
				res.setRawResult(new SimpleHttpResponse(response));
			}
			
		} catch (Throwable t) {
			res = this.failed(t.getMessage(), t);
		}
		return res;
	}

	/** create poll task */
	private AsyncHttpTask createPollTask(final UrlRequest pollReq) {
		
		return new AsyncHttpTask(client) {

			@Override
			public BoundRequestBuilder createRequest() {
				
				return buildHttpRequest(pollReq);
			}

			@Override
			public TaskResult onComplete(Response response) {
				TaskResult res = null;
				try {
					res = SimpleHttpAsyncPollTask.this.pollProcessor.checkPollProgress(SimpleHttpAsyncPollTask.this, response);
					if (res != null && res.isComplete()) {
						res.setRawResult(new SimpleHttpResponse(response));
					}
				} catch (Throwable t) {
					res = this.failed(t.getMessage(), t);
				}
				return res!=null ? res : this.hasResult(TaskResultEnum.Running, null);
			}

			@Override
			public TaskResult onThrowable(Throwable t) {
				return this.failed(TaskResultEnum.Failed, t.getMessage(), t);
			}
		};

	}
	
}

