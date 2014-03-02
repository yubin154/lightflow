package org.lightj.task.asynchttp;

import java.io.IOException;

import org.lightj.session.FlowContext;
import org.lightj.task.ExecuteOption;
import org.lightj.task.MonitorOption;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;

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
	
	public abstract TaskResult checkPollProgress(Response response);
	
	@Override
	public TaskResult onComplete(Response response) {
		TaskResult res = null;
		String statusCode = Integer.toString(response.getStatusCode());
		if (statusCode.matches("2[0-9][0-9]")) {
			res = createTaskResult(TaskResultEnum.Success, statusCode);
			for (String transferableVariable : this.transferableVariables) {
				pollReq.addTemplateValue(transferableVariable, req.getTemplateValue(transferableVariable));
			}
			this.setExtTaskUuid(pollReq.generateUrl());
			AsyncHttpTask pollTask = createPollTask(pollReq);
			res.setRealResult(pollTask);
		}
		else {
			res = createTaskResult(TaskResultEnum.Failed, statusCode);
			try {
				res.setRealResult(response.getResponseBody());
			} catch (IOException e) {
				res.setRealResult(e.getMessage());
			}
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
				return SimpleHttpAsyncPollTask.this.checkPollProgress(response);
			}

			@Override
			public TaskResult onThrowable(Throwable t) {
				return this.createErrorResult(TaskResultEnum.Failed, t.getMessage(), t);
			}
		};

	}
	
}

