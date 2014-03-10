package org.lightj.task.asynchttp;

import org.lightj.session.FlowContext;
import org.lightj.task.ExecuteOption;
import org.lightj.task.MonitorOption;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;
import org.lightj.util.StringUtil;

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
	private IPollProcessor pollProcessor;
	
	/** constructor */
	public SimpleHttpAsyncPollTask(
			AsyncHttpClient client, 
			ExecuteOption execOptions, 
			MonitorOption monitorOption, 
			IPollProcessor pollProcessor) 
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
	
	public IPollProcessor getPollProcessor() {
		return pollProcessor;
	}

	public void setPollProcessor(IPollProcessor pollProcessor) {
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
			int sCode = response.getStatusCode();
			if (sCode >= 200 && sCode < 300) {
				TaskResultEnum rst = pollProcessor.preparePollTask(response, pollReq);
				res = this.createTaskResult(rst, Integer.toString(sCode));
				if (TaskResultEnum.Success == rst) {
					AsyncHttpTask pollTask = createPollTask(pollReq);
					res.setRealResult(pollTask);
				}
			}
			else {
				res = createTaskResult(TaskResultEnum.Failed, Integer.toString(sCode));
				res.setRealResult(response.getResponseBodyExcerpt(MSG_CONTENT_LEN));
			}
		} catch (Throwable t) {
			res = this.createTaskResult(TaskResultEnum.Failed, StringUtil.getStackTrace(t, MSG_CONTENT_LEN));
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
					int sCode = response.getStatusCode();
					if (sCode >= 200 && sCode < 300) {
						TaskResultEnum rst = SimpleHttpAsyncPollTask.this.pollProcessor.checkPollProgress(response);
						res = this.createTaskResult(rst, Integer.toString(sCode));
						if (rst.isComplete()) {
							res.setRealResult(response.getResponseBodyExcerpt(MSG_CONTENT_LEN));
						}
					}
					else {
						res = createTaskResult(TaskResultEnum.Failed, Integer.toString(sCode));
						res.setRealResult(response.getResponseBodyExcerpt(MSG_CONTENT_LEN));
					}
				} catch (Throwable t) {
					res = this.createTaskResult(TaskResultEnum.Failed, StringUtil.getStackTrace(t, MSG_CONTENT_LEN));
				}
				return res;
			}

			@Override
			public TaskResult onThrowable(Throwable t) {
				return this.createErrorResult(TaskResultEnum.Failed, t.getMessage(), t);
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

