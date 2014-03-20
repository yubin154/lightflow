package org.lightj.example.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lightj.task.ExecuteOption;
import org.lightj.task.MonitorOption;
import org.lightj.task.asynchttp.UrlTemplate;

/**
 * wrapper class of user's request
 * @author binyu
 *
 */
public class HttpTaskRequest {

	/** task type */
	public static enum TaskType {
		async, asyncpoll;
	}

	/** type, async, asyncpoll, async_group, asyncpoll_group*/
	String taskType;
	/** client name used to look up for spring bean */
	String httpClientType;
	
	/** for async */
	ExecuteOption executionOption;
	UrlTemplate urlTemplate;
	List<Map<String, String>> templateValues;
	String[] hosts;

	/** additional for asyncpoll */
	MonitorOption monitorOption;
	UrlTemplate pollTemplate;
	private String pollProcessorName;
	
	/** custom handler name for spring bean */
	private String customHandler;
	
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
	public List<Map<String, String>> getTemplateValues() {
		return templateValues;
	}
	public void setTemplateValues(List<Map<String, String>> templateValues) {
		this.templateValues = templateValues;
	}
	public void addTemplateValue(Map<String, String> value) {
		if (templateValues == null) {
			templateValues = new ArrayList<Map<String, String>>();
		}
		this.templateValues.add(value);
	}
	public void addAllTemplateValues(List<Map<String, String>> templateValues) {
		if (templateValues == null) {
			templateValues = new ArrayList<Map<String, String>>();
		}
		this.templateValues.addAll(templateValues);
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
	public String getPollProcessorName() {
		return pollProcessorName;
	}
	public void setPollProcessorName(String pollProcessorName) {
		this.pollProcessorName = pollProcessorName;
	}
	public String getCustomHandler() {
		return customHandler;
	}
	public void setCustomHandler(String customHandler) {
		this.customHandler = customHandler;
	}
	public String[] getHosts() {
		return hosts;
	}
	public void setHosts(String[] hosts) {
		this.hosts = hosts;
	}
	public void setHost(String host) {
		this.hosts = new String[] {host};
	}
}