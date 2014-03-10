package org.lightj.example.session.simplehttpflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.lightj.session.FlowModule;
import org.lightj.task.ExecutableTask;
import org.lightj.task.ExecuteOption;
import org.lightj.task.GroupTask;
import org.lightj.task.MonitorOption;
import org.lightj.task.TaskResultEnum;
import org.lightj.task.asynchttp.IPollProcessor;
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
	
	public @Bean @Scope("prototype") IPollProcessor dummyPollProcessor() {
		
		return new IPollProcessor() {

			@Override
			public TaskResultEnum checkPollProgress(Response response) {
				return TaskResultEnum.Success;
			}

			@Override
			public TaskResultEnum preparePollTask(Response reponse,
					UrlRequest pollReq) {
				return TaskResultEnum.Success;
			}
			
		};
	}
	
	/**
	 * build a real http task from http task wrapper
	 * @param tw
	 * @return
	 */
	public static ExecutableTask buildTask(final HttpTaskWrapper tw) {
		
		TaskType tt = TaskType.valueOf(tw.taskType);
		ExecutableTask task = null;
		
		final AsyncHttpClient client = SpringContextUtil.getBean(FlowModule.FLOW_CTX, tw.httpClientType, AsyncHttpClient.class);
		switch(tt) {
		case async:
			SimpleHttpTask atask = new SimpleHttpTask(client, tw.executionOption);
			atask.setReq(new UrlRequest(tw.urlTemplate).addAllTemplateValues(tw.templateValues));
			task = atask;
			break;
			
		case asyncpoll:
			IPollProcessor pp = SpringContextUtil.getBean(FlowModule.FLOW_CTX, tw.getPollProcessorName(), IPollProcessor.class);
			SimpleHttpAsyncPollTask btask = new SimpleHttpAsyncPollTask(client, tw.executionOption, tw.monitorOption, pp);			
			btask.setHttpParams(new UrlRequest(tw.urlTemplate).addAllTemplateValues(tw.templateValues), new UrlRequest(tw.pollTemplate),
					tw.getSharableVariables()!=null ? tw.getSharableVariables().toArray(new String[0]) : null);
			task = btask;
			break;
			
		case asyncgroup:
			GroupTask agtask = new GroupTask() {

				@Override
				public SimpleHttpTask createTaskInstance() {
					SimpleHttpTask atask = new SimpleHttpTask(client, tw.executionOption);
					atask.setReq(new UrlRequest(tw.urlTemplate).addAllTemplateValues(tw.templateValues));
					return atask;
				}

				@Override
				public List<SimpleHttpTask> getTasks() {
					ArrayList<SimpleHttpTask> results = new ArrayList<SimpleHttpTask>();
					for (String fanoutValue: tw.fanoutValues) {
						SimpleHttpTask atask = createTaskInstance();
						atask.getReq().addTemplateValue(tw.fanoutFactor, fanoutValue);
						results.add(atask);
					}
					return results;
				}
			};
			task = agtask;
			break;
			
		case asyncpollgroup:
			
			GroupTask bgtask = new GroupTask() {

				@Override
				public SimpleHttpAsyncPollTask createTaskInstance() {
					IPollProcessor pp = SpringContextUtil.getBean(FlowModule.FLOW_CTX, tw.getPollProcessorName(), IPollProcessor.class);
					SimpleHttpAsyncPollTask btask = new SimpleHttpAsyncPollTask(client, tw.executionOption, tw.monitorOption, pp);			
					btask.setHttpParams(new UrlRequest(tw.urlTemplate).addAllTemplateValues(tw.templateValues), new UrlRequest(tw.pollTemplate),
							tw.getSharableVariables()!=null ? tw.getSharableVariables().toArray(new String[0]) : null);
					return btask;
				}

				@Override
				public List<SimpleHttpAsyncPollTask> getTasks() {
					ArrayList<SimpleHttpAsyncPollTask> results = new ArrayList<SimpleHttpAsyncPollTask>();
					for (String host: tw.fanoutValues) {
						SimpleHttpAsyncPollTask atask = createTaskInstance();
						atask.getReq().addTemplateValue(tw.fanoutFactor, host);
						results.add(atask);
					}
					return results;
				}
			};
			task = bgtask;
			break;
			
			
		default:
			break;				
		}
		return task;
	}

	
	/**
	 * wrapper class of user's request
	 * @author binyu
	 *
	 */
	public static class HttpTaskWrapper {
		/** type, async, asyncpull, async_group, asyncpull_group*/
		private String taskType;
		/** client name used to look up for spring bean */
		private String httpClientType;
		
		/** for async */
		private ExecuteOption executionOption;
		private UrlTemplate urlTemplate;
		private HashMap<String, String> templateValues;

		/** additional for asyncpull */
		private MonitorOption monitorOption;
		private UrlTemplate pollTemplate;
		private List<String> sharableVariables;
		private String pollProcessorName;
		
		/** additional for group task */
		private String fanoutFactor;
		private String[] fanoutValues;
		
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
		public UrlTemplate getPollTemplate() {
			return pollTemplate;
		}
		public void setPollTemplate(UrlTemplate pullTemplate) {
			this.pollTemplate = pullTemplate;
		}
		public List<String> getSharableVariables() {
			return sharableVariables;
		}
		public void setSharableVariables(List<String> sharableVariables) {
			this.sharableVariables = sharableVariables;
		}
		public String getPollProcessorName() {
			return pollProcessorName;
		}
		public void setPollProcessorName(String pollProcessorName) {
			this.pollProcessorName = pollProcessorName;
		}
		public String getFanoutFactor() {
			return fanoutFactor;
		}
		public void setFanoutFactor(String fanoutFactor) {
			this.fanoutFactor = fanoutFactor;
		}
		public String[] getFanoutValues() {
			return fanoutValues;
		}
		public void setFanoutValues(String[] fanoutValues) {
			this.fanoutValues = fanoutValues;
		}
	}
	
	/** task type */
	public static enum TaskType {
		async, asyncpoll, asyncgroup, asyncpollgroup;
	}
}
