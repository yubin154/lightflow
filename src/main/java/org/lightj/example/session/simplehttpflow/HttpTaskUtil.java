package org.lightj.example.session.simplehttpflow;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.lightj.session.FlowModule;
import org.lightj.task.ExecutableTask;
import org.lightj.task.ExecuteOption;
import org.lightj.task.MonitorOption;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;
import org.lightj.task.asynchttp.SimpleHttpAsyncPollTask;
import org.lightj.task.asynchttp.SimpleHttpTask;
import org.lightj.task.asynchttp.UrlRequest;
import org.lightj.task.asynchttp.UrlTemplate;
import org.lightj.util.SpringContextUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfigBean;
import com.ning.http.client.Response;

@SuppressWarnings("rawtypes")
@Configuration
public class HttpTaskUtil {
	
	public @Bean @Scope("prototype") AsyncHttpClient httpClient() {
		// create and configure async http client
		AsyncHttpClientConfigBean config = new AsyncHttpClientConfigBean();
		config.setConnectionTimeOutInMs(3000);
		return new AsyncHttpClient(config);
	}
	
	public static ExecutableTask buildTask(HttpTaskWrapper tw) {
		
		TaskType tt = TaskType.valueOf(tw.taskType);
		ExecutableTask task = null;
		
		AsyncHttpClient client = SpringContextUtil.getBean(FlowModule.FLOW_CTX, tw.httpClientType, AsyncHttpClient.class);
		switch(tt) {
		case async:
			SimpleHttpTask atask = new SimpleHttpTask(client, tw.executionOption);
			atask.setReq(new UrlRequest(tw.urlTemplate).addAllTemplateValues(tw.templateValues));
			task = atask;
			break;
		case asyncpull:
			SimpleHttpAsyncPollTask btask = new SimpleHttpAsyncPollTask(client, tw.executionOption, tw.monitorOption) {

				@Override
				public TaskResult checkPollProgress(Response response) {
					TaskResult res = null;
					int sCode = response.getStatusCode();
					String statusCode = Integer.toString(sCode);
					if (sCode >= 200 && sCode <300) {
						res = createTaskResult(TaskResultEnum.Success, statusCode);
					}
					else {
						res = createTaskResult(TaskResultEnum.Failed, statusCode);
					}
					try {
						res.setRealResult(response.getResponseBody());
					} catch (IOException e) {
						// ignore
					}
					return res;
				}
				
			};
			btask.setHttpParams(new UrlRequest(tw.urlTemplate), new UrlRequest(tw.pullTemplate), 
					tw.getTransferableVariables()!=null ? tw.getTransferableVariables().toArray(new String[0]) : null);
			task = btask;
			break;
		default:
			break;				
		}
		return task;
	}

	
	public static class HttpTaskWrapper {
		/** type, async, asyncpull, async_group, asyncpull_group*/
		private String taskType;
		private String httpClientType;
		
		/** for async */
		private ExecuteOption executionOption;
		private UrlTemplate urlTemplate;
		private HashMap<String, String> templateValues;

		/** for asyncpull */
		private MonitorOption monitorOption;
		private UrlTemplate pullTemplate;
		private List<String> transferableVariables;
		
		/** for group_ */
		public String getTaskType() {
			return taskType;
		}
		public void setTaskType(String taskType) {
			this.taskType = taskType;
		}
		public String getHttpClientType() {
			return httpClientType;
		}
		public void setHttpClientType(String httpClientType) {
			this.httpClientType = httpClientType;
		}
		public ExecuteOption getExecutionOption() {
			return executionOption;
		}
		public void setExecutionOption(ExecuteOption executionOption) {
			this.executionOption = executionOption;
		}
		public UrlTemplate getUrlTemplate() {
			return urlTemplate;
		}
		public void setUrlTemplate(UrlTemplate urlTemplate) {
			this.urlTemplate = urlTemplate;
		}
		public HashMap<String, String> getTemplateValues() {
			return templateValues;
		}
		public void setTemplateValues(HashMap<String, String> templateValues) {
			this.templateValues = templateValues;
		}
		public MonitorOption getMonitorOption() {
			return monitorOption;
		}
		public void setMonitorOption(MonitorOption monitorOption) {
			this.monitorOption = monitorOption;
		}
		public UrlTemplate getPullTemplate() {
			return pullTemplate;
		}
		public void setPullTemplate(UrlTemplate pullTemplate) {
			this.pullTemplate = pullTemplate;
		}
		public List<String> getTransferableVariables() {
			return transferableVariables;
		}
		public void setTransferableVariables(List<String> transferableVariables) {
			this.transferableVariables = transferableVariables;
		}
	}
	
	private static enum TaskType {
		async, asyncpull, group_async, group_asyncpull;
	}
}
