package org.lightj.example.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lightj.example.task.HttpTaskRequest.TaskType;
import org.lightj.task.ExecutableTask;
import org.lightj.task.GroupTask;
import org.lightj.task.Task;
import org.lightj.task.TaskResult;
import org.lightj.task.asynchttp.IHttpPollProcessor;
import org.lightj.task.asynchttp.SimpleHttpAsyncPollTask;
import org.lightj.task.asynchttp.SimpleHttpTask;
import org.lightj.task.asynchttp.UrlRequest;
import org.lightj.util.SpringContextUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfigBean;
import com.ning.http.client.Response;

@SuppressWarnings("rawtypes")
@Configuration
public class HttpTaskBuilder {
	
	/**
	 * ning http client
	 * @return
	 */
	public @Bean @Scope("prototype") AsyncHttpClient httpClient() {
		// create and configure async http client
		AsyncHttpClientConfigBean config = new AsyncHttpClientConfigBean();
		config.setConnectionTimeOutInMs(3000);
		return new AsyncHttpClient(config);
	}
	
	/**
	 * dummy poll processor
	 * @return
	 */
	public @Bean @Scope("prototype") IHttpPollProcessor dummyPollProcessor() {
		
		return new IHttpPollProcessor() {

			@Override
			public TaskResult checkPollProgress(Task task, Response response) {
				return task.succeeded();
			}

			@Override
			public TaskResult preparePollTask(Task task, Response response,
					UrlRequest pollReq) {
				return task.succeeded();
			}
			
		};
	}
	
	/**
	 * agent poll processor
	 * @return
	 */
	public @Bean @Scope("prototype") IHttpPollProcessor agentPollProcessor() {

		final String successRegex = ".*\\\"progress\\\"\\s*:\\s*100.*";
		final String failureRegex = ".*\\\"error\\\"\\s*:\\s*(.*),.*";
		// matching pattern "status": "/status/uuid"
		final String uuidRegex = ".*\\\"/status/(.*?)\\\".*,";
		final Pattern r = Pattern.compile(uuidRegex);
		return new IHttpPollProcessor() {

			@Override
			public TaskResult checkPollProgress(Task task, Response response) throws IOException {
				
				int sCode = response.getStatusCode();
				if (sCode >= 400) {
					return task.failed(Integer.toString(sCode), null);
				}
				String body = response.getResponseBody();
				if (body.matches(successRegex)) {
					return task.succeeded();
				}
				else if (body.matches(failureRegex)) {
					return task.failed(body, null);
				}
				return null;
			}

			@Override
			public TaskResult preparePollTask(
					Task task,
					Response response,
					UrlRequest pollReq) throws IOException 
			{
				int sCode = response.getStatusCode();
				if (sCode >= 400) {
					if (sCode == 401) {
						// update key 
					}
					return task.failed(Integer.toString(sCode), null);
				}
				String body = response.getResponseBody();
				Matcher m = r.matcher(body);
				if (m.find()) {
					String uuid = m.group(1);
					task.setExtTaskUuid(uuid);
					pollReq.addTemplateValue("#uuid", uuid);
					return task.succeeded();
				} else {
					return task.failed("cannot find uuid", null);
				}
			}
			
		};
		
	}
	
	/**
	 * build a real http task from http task wrapper
	 * @param tw
	 * @return
	 */
	public static ExecutableTask buildTask(final HttpTaskRequest tw) {
		
		TaskType tt = TaskType.valueOf(tw.taskType);
		ExecutableTask task = null;
		boolean isGroupTask = (tw.hosts.length > 1 || 
				(tw.templateValues != null && tw.templateValues.size() > 1));
		
		final AsyncHttpClient client = SpringContextUtil.getBeanOfNameFromAllContext(tw.httpClientType, AsyncHttpClient.class);
		switch(tt) {
		case async:
			if (!isGroupTask) {
				SimpleHttpTask atask = new SimpleHttpTask(client, tw.executionOption);
				UrlRequest urlReq = new UrlRequest(tw.urlTemplate).setHost(tw.hosts[0]);
				if (tw.templateValues != null) {
					urlReq.addAllTemplateValues(tw.templateValues.get(0));
				}
				atask.setReq(urlReq);
				task = atask;
			}
			else {
				GroupTask agtask = new GroupTask() {

					@Override
					public SimpleHttpTask createTaskInstance() {
						SimpleHttpTask atask = new SimpleHttpTask(client, tw.executionOption);
						atask.setReq(new UrlRequest(tw.urlTemplate));
						return atask;
					}

					@Override
					public List<SimpleHttpTask> getTasks() {
						ArrayList<SimpleHttpTask> results = new ArrayList<SimpleHttpTask>();
						for (String host: tw.hosts) {
							if (tw.templateValues != null) {
								for (Map<String, String> tvalue : tw.templateValues) {
									SimpleHttpTask atask = createTaskInstance();
									atask.getReq().addAllTemplateValues(tvalue);
									atask.getReq().setHost(host);
									results.add(atask);
								}
							}
							else {
								SimpleHttpTask atask = createTaskInstance();
								atask.getReq().setHost(host);
								results.add(atask);
							}
						}
						return results;
					}
				};
				task = agtask;
			}
			break;
			
		case asyncpoll:
			if (!isGroupTask) {
				IHttpPollProcessor pp = SpringContextUtil.getBeanOfNameFromAllContext(tw.getPollProcessorName(), IHttpPollProcessor.class);
				SimpleHttpAsyncPollTask btask = new SimpleHttpAsyncPollTask(client, tw.executionOption, tw.monitorOption, pp);
				UrlRequest urlReq = new UrlRequest(tw.urlTemplate).setHost(tw.hosts[0]);
				if (tw.templateValues != null) {
					urlReq.addAllTemplateValues(tw.templateValues.get(0));
				}
				btask.setReq(urlReq);
				task = btask;
			}
			else {
				GroupTask bgtask = new GroupTask() {

					@Override
					public SimpleHttpAsyncPollTask createTaskInstance() {
						IHttpPollProcessor pp = SpringContextUtil.getBeanOfNameFromAllContext(tw.getPollProcessorName(), IHttpPollProcessor.class);
						SimpleHttpAsyncPollTask btask = new SimpleHttpAsyncPollTask(client, tw.executionOption, tw.monitorOption, pp);			
						btask.setHttpParams(new UrlRequest(tw.urlTemplate), new UrlRequest(tw.pollTemplate));
						return btask;
					}

					@Override
					public List<SimpleHttpAsyncPollTask> getTasks() {
						ArrayList<SimpleHttpAsyncPollTask> results = new ArrayList<SimpleHttpAsyncPollTask>();
						for (String host: tw.hosts) {
							if (tw.templateValues != null) {
								for (Map<String, String> tvalue : tw.templateValues) {
									SimpleHttpAsyncPollTask atask = createTaskInstance();
									atask.getReq().addAllTemplateValues(tvalue);
									atask.getReq().setHost(host);
									results.add(atask);
								}
							}
							else {
								SimpleHttpAsyncPollTask atask = createTaskInstance();
								atask.getReq().setHost(host);
								results.add(atask);
							}
						}
						return results;
					}
				};
				task = bgtask;
			}
			break;
			
		default:
			break;				
		}
		return task;
	}

	
}
