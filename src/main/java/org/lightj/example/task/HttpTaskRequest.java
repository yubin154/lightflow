package org.lightj.example.task;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lightj.task.BatchOption;
import org.lightj.task.ExecuteOption;
import org.lightj.task.MonitorOption;
import org.lightj.task.asynchttp.UrlTemplate;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * wrapper class of user's request, capturing everything needed to run a http task
 * 
 * @author binyu
 *
 */
public class HttpTaskRequest {

	/** task type */
	public static enum TaskType {
		async, asyncpoll;
	}

	/** type, async, asyncpoll */
	String taskType;
	
	/** client name used to look up for spring bean */
	String httpClientType;
	
	/** for sync */
	ExecuteOption executionOption;
	UrlTemplate urlTemplate;
	
	/** map of host to template value(s) **/
	Map<String, HostTemplateValues> hostTemplateValues;
	HostTemplateValues templateValuesForAllHosts;
	String[] hosts;

	/** additional for asyncpoll */
	MonitorOption monitorOption;
	UrlTemplate pollTemplate;
	String resProcessorName;
	
	/** custom handler name for spring bean, for custom result handling */
	String customHandler;
	
	/** global context name for spring bean, for shared information */
	String globalContext;
	
	/** batch option, for fanning out multiple requests */
	BatchOption batchOption;
	
	public HttpTaskRequest setSyncTaskOptions(String httpClientType, UrlTemplate urlTemplate, ExecuteOption executionOption, String resProcessorName) {
		this.taskType = TaskType.async.name();
		this.urlTemplate = urlTemplate;
		this.executionOption = executionOption;
		this.httpClientType = httpClientType;
		this.resProcessorName = resProcessorName;
		return this;
	}
	public HttpTaskRequest setAsyncPollTaskOption(String httpClientType, UrlTemplate urlTemplate, ExecuteOption executionOption,
			UrlTemplate pollTemplate, MonitorOption monitorOption, String resProcessorName) {
		this.taskType = TaskType.asyncpoll.name();
		this.httpClientType = httpClientType;
		this.urlTemplate = urlTemplate;
		this.executionOption = executionOption;
		this.pollTemplate = pollTemplate;
		this.monitorOption = monitorOption;
		this.resProcessorName = resProcessorName;
		return this;
	}
	public String getTaskType() {
		return taskType;
	}
	public HttpTaskRequest setTaskType(String taskType) {
		this.taskType = taskType;
		return this;
	}
	public String getHttpClientType() {
		return httpClientType;
	}
	public HttpTaskRequest setHttpClientType(String httpClientType) {
		this.httpClientType = httpClientType;
		return this;
	}
	public ExecuteOption getExecutionOption() {
		return executionOption;
	}
	public HttpTaskRequest setExecutionOption(ExecuteOption executionOption) {
		this.executionOption = executionOption;
		return this;
	}
	public UrlTemplate getUrlTemplate() {
		return urlTemplate;
	}
	public HttpTaskRequest setUrlTemplate(UrlTemplate urlTemplate) {
		this.urlTemplate = urlTemplate;
		return this;
	}
	
	/** template values for hosts */
	public Map<String, HostTemplateValues> getHostTemplateValues() {
		return hostTemplateValues;
	}
	/** template values for a specific hosts */ 
	public HttpTaskRequest addHostTemplateValues(String host, HostTemplateValues values) {
		if (hostTemplateValues == null) {
			hostTemplateValues = new LinkedHashMap<String, HostTemplateValues>();
		}
		if (!hostTemplateValues.containsKey(host)) {
			hostTemplateValues.put(host, values);
		}
		else {
			hostTemplateValues.get(host).addAllTemplateValues(values.getTemplateValues());
		}
		return this;
	}
	/** template values that apply to all hosts */ 
	public HttpTaskRequest setTemplateValuesForAllHosts(HostTemplateValues values) {
		if (templateValuesForAllHosts == null) {
			templateValuesForAllHosts = values;
		}
		else {
			templateValuesForAllHosts.addAllTemplateValues(values.getTemplateValues());
		}
		return this;
	}
	/** template values that apply to all hosts */ 
	public HostTemplateValues getTemplateValuesForAllHosts() {
		return templateValuesForAllHosts;
	}
	/**
	 * generate template values for this host
	 * @param host
	 * @return
	 */
	public HostTemplateValues getHostTemplateValuesForHost(String host) {
		HostTemplateValues result = new HostTemplateValues();
		List<Map<String, String>> templatesForAll = null;
		List<Map<String, String>> templatesForHost = null;
		if (templateValuesForAllHosts != null) {
			templatesForAll = templateValuesForAllHosts.getTemplateValues();
		}
		if (hostTemplateValues != null && hostTemplateValues.containsKey(host)) {
			templatesForHost = hostTemplateValues.get(host).getTemplateValues();
		}
		if (templatesForAll != null) {
			if (templatesForHost != null) {
				for (Map<String, String> template4All : templatesForAll) {
					for (Map<String, String> template4Host : templatesForHost) {
						result.addNewTemplateValue(template4Host).addToCurrentTemplate(template4All);
					}
				}
			}
			else {
				result.addAllTemplateValues(templatesForAll);
			}
		}
		else if (templatesForHost != null) {
			result.addAllTemplateValues(templatesForHost);
		}
		return result;
	}

	public MonitorOption getMonitorOption() {
		return monitorOption;
	}
	public HttpTaskRequest setMonitorOption(MonitorOption monitorOption) {
		this.monitorOption = monitorOption;
		return this;
	}
	public UrlTemplate getPollTemplate() {
		return pollTemplate;
	}
	public HttpTaskRequest setPollTemplate(UrlTemplate pullTemplate) {
		this.pollTemplate = pullTemplate;
		return this;
	}
	public String getResProcessorName() {
		return resProcessorName;
	}
	public HttpTaskRequest setResProcessorName(String pollProcessorName) {
		this.resProcessorName = pollProcessorName;
		return this;
	}
	public String getCustomHandler() {
		return customHandler;
	}
	public HttpTaskRequest setCustomHandler(String customHandler) {
		this.customHandler = customHandler;
		return this;
	}
	public String[] getHosts() {
		return hosts;
	}
	public HttpTaskRequest setHosts(String...hosts) {
		this.hosts = hosts;
		return this;
	}
	public String getGlobalContext() {
		return globalContext;
	}
	public HttpTaskRequest setGlobalContext(String globalContext) {
		this.globalContext = globalContext;
		return this;
	}
	public BatchOption getBatchOption() {
		return batchOption;
	}
	public HttpTaskRequest setBatchOption(BatchOption batchOption) {
		this.batchOption = batchOption;
		return this;
	}
	@JsonIgnore
	public boolean isGroupTask() {
		return (hosts.length > 1 || 
				(hostTemplateValues != null && (hostTemplateValues.size() > 1 || 
						hostTemplateValues.entrySet().iterator().next().getValue().hasMultiple())) ||
				(templateValuesForAllHosts != null && templateValuesForAllHosts.hasMultiple()));
	}
	@JsonIgnore
	public boolean isNoopTask() {
		return (hosts.length == 0);
	}
	
	public HttpTaskRequest createNew() {
		HttpTaskRequest newReq = new HttpTaskRequest();
		if (this.batchOption != null) {
			newReq.batchOption = new BatchOption(this.batchOption.getConcurrentRate(), 
					this.batchOption.getStrategy());
		}
		newReq.customHandler = this.customHandler;
		if (this.executionOption != null) {
			newReq.executionOption = new ExecuteOption(
					executionOption.getInitialDelayMs(), 
					executionOption.getTimeoutInMs(), 
					executionOption.getMaxRetry(), 
					executionOption.getRetryDelayMs());
		}
		newReq.globalContext = this.globalContext;
		newReq.httpClientType = this.httpClientType;
		if (this.monitorOption != null) {
			newReq.monitorOption = new MonitorOption(
					monitorOption.getInitialDelayMs(), 
					monitorOption.getMonitorIntervalMs(), 
					monitorOption.getTimeoutInMs(), 
					monitorOption.getMaxRetry(), 
					monitorOption.getRetryDelayMs());
		}
		newReq.resProcessorName = this.resProcessorName;
		if (this.pollTemplate != null) {
			newReq.pollTemplate = this.pollTemplate.createNew();
		}
		newReq.taskType = this.taskType;
		if (this.urlTemplate != null) {
			newReq.urlTemplate = this.urlTemplate.createNew();
		}
		return newReq;
	}
}