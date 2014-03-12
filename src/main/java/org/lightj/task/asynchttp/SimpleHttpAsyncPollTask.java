package org.lightj.task.asynchttp;

import org.lightj.session.FlowContext;
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
@SuppressWarnings({"rawtypes", "unchecked"})
public class SimpleHttpAsyncPollTask<T extends FlowContext> extends SimpleHttpTask<T> {
	
	/** variables to be copied from req to poll template */
	private String[] sharedVariables = null;
	
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
	
	public void setHttpParams(UrlRequest req, UrlRequest pollReq, String...sharedVariables) {
		this.req = req;
		this.pollReq = pollReq;
		this.sharedVariables = sharedVariables;
	}
	
	public IHttpPollProcessor getPollProcessor() {
		return pollProcessor;
	}

	public void setPollProcessor(IHttpPollProcessor pollProcessor) {
		this.pollProcessor = pollProcessor;
	}
	
	protected BoundRequestBuilder buildHttpRequest(UrlRequest req) {
		BoundRequestBuilder builder = super.buildHttpRequest(req);
		for (String sharableVariable : this.sharedVariables) {
			pollReq.addTemplateValue(sharableVariable, req.getTemplateValue(sharableVariable));
		}
		return builder;
	}

	@Override
	public TaskResult onComplete(Response response) {
		TaskResult res = null;
		try {
			res = pollProcessor.preparePollTask(this, response, pollReq);
			if (TaskResultEnum.Success == res.getStatus()) {
				AsyncHttpTask pollTask = createPollTask(pollReq);
				res.setRealResult(pollTask);
			}
		} catch (Throwable t) {
			res = this.failed(t.getMessage(), t);
		}
		return res;
	}

	/** create poll task */
	private AsyncHttpTask createPollTask(final UrlRequest pollReq) {
		
		return new AsyncHttpTask<FlowContext>(client) {

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
						res.setRealResult(response.getResponseBody());
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
	
	/**
	 * make a copy
	 * @return
	 */
	public SimpleHttpAsyncPollTask makeCopy() {
		SimpleHttpAsyncPollTask another = new SimpleHttpAsyncPollTask(client, execOptions, monitorOption, pollProcessor);
		another.req = this.req;
		another.valueFromContext = this.valueFromContext;
		another.sharedVariables = this.sharedVariables;
		another.pollReq = this.pollReq;
		another.pollProcessor = this.pollProcessor;
		return another;
	}
}

