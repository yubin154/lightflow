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

@SuppressWarnings("rawtypes")
public abstract class SimpleHttpAsyncPollTask<T extends FlowContext> extends SimpleHttpTask<T> {
	
	/** variables to be copied from req to poll template */
	private String[] transferableVariables = null;
	
	/** pull request */
	private UrlRequest pollReq;
	
	/** constructor */
	public SimpleHttpAsyncPollTask(AsyncHttpClient client, ExecuteOption execOptions, MonitorOption monitorOption) 
	{
		super(client, execOptions);
		this.monitorOption = monitorOption;
	}
	
	public void setHttpParams(UrlRequest req, UrlRequest pollReq, String...transferableVariables) {
		this.req = req;
		this.pollReq = pollReq;
		this.transferableVariables = transferableVariables;
	}
	
	public abstract TaskResultEnum preparePollTask(Response reponse, UrlRequest pollReq); 
	
	public abstract TaskResultEnum checkPollProgress(Response response);
	
	@Override
	public TaskResult onComplete(Response response) {
		TaskResult res = null;
		try {
			int sCode = response.getStatusCode();
			if (sCode >= 200 && sCode < 300) {
				TaskResultEnum rst = preparePollTask(response, pollReq);
				res = this.createTaskResult(rst, Integer.toString(sCode));
				if (TaskResultEnum.Success == rst) {
					for (String transferableVariable : this.transferableVariables) {
						pollReq.addTemplateValue(transferableVariable, req.getTemplateValue(transferableVariable));
					}
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
						TaskResultEnum rst = SimpleHttpAsyncPollTask.this.checkPollProgress(response);
						res = this.createTaskResult(rst, Integer.toString(sCode));
						if (rst.isComplete()) {
							response.getResponseBodyExcerpt(MSG_CONTENT_LEN); 
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
	
}

