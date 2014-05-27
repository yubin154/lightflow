package org.lightj.task.asynchttp;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lightj.task.IGlobalContext;
import org.lightj.task.asynchttp.AsyncHttpTask.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * url request
 * @author binyu
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class UrlRequest {

	/** logger */
	static Logger logger = LoggerFactory.getLogger(UrlRequest.class);

	/** nvp replacing variables in template with real values */
	final HashMap<String, String> templateValues = new HashMap<String, String>();
	
	/** url template, typically immutable and contains variables */
	UrlTemplate urlTemplate;
	
	/** optionally lookup template value from external through this function */
	IGlobalContext globalContext;
	
	/** constructor */
	public UrlRequest() {
	}
	
	/** constructor */
	public UrlRequest(UrlTemplate urlTemplate) { 
		this.urlTemplate = urlTemplate; 
		
	}
	
	public UrlTemplate getUrlTemplate() {
		return urlTemplate;
	}
	public void setUrlTemplate(UrlTemplate urlTemplate) {
		this.urlTemplate = urlTemplate;
	}

	public IGlobalContext getGlobalContext() {
		return globalContext;
	}

	public UrlRequest addTemplateValue(String k, String v) {
		if (!urlTemplate.hasVariableKey(k)) {
			logger.warn("UrlTemplate for this UrlRequest does not have variable " + k);
		}
		templateValues.put(UrlTemplate.encodeIfNeeded(k), v);
		return this;
	}
	public UrlRequest addAllTemplateValues(Map<String, String> values) {
		if (values != null) {
			for (Entry<String, String> entry : values.entrySet()) {
				this.addTemplateValue(entry.getKey(), entry.getValue());
			}
		}
		return this;
	}
	/** internal use only */
	UrlRequest putTemplateValuesIfNull(Map<String, String> values) {
		for (Entry<String, String> value : values.entrySet()) {
			if (!templateValues.containsKey(value.getKey())) {
				templateValues.put(value.getKey(), value.getValue());
			}
		}
		return this;
	}
	public String getTemplateValue(String k) {
		return templateValues.get(UrlTemplate.encodeIfNeeded(k));
	}
	/** internal use only */
	HashMap<String, String> getTemplateValues() {
		return templateValues;
	}
	public UrlRequest setHost(String host) {
		this.addTemplateValue("host", host);
		return this;
	}
	public String getHost() {
		return getTemplateValue("host");
	}
	@JsonIgnore
	public IGlobalContext getGlobalConext() {
		return globalContext;
	}
	public void setGlobalContext(
			IGlobalContext templateValueLookup) {
		this.globalContext = templateValueLookup;
	}
	
	public HttpMethod getMethod() {
		return urlTemplate.getMethod();
	}
	public boolean hasBody() {
		return !urlTemplate.getBody().isEmpty();
	}
	/** get real body after replacing template */
	public HashMap<String, String> generateBody() {
		HashMap<String, String> bodyReal = new HashMap<String, String>();
		for (Entry<String, String> param : urlTemplate.getBody().entrySet()) {
			replaceAndSet(param, bodyReal);
		}
		return bodyReal;
	}

	/** get real url after template replacement */
	public String generateUrl() {
		String urlV = replace(urlTemplate.getUrl());
		return urlV;
	}
	
	/** real parameters */
	public HashMap<String, String> generateParameters() {
		HashMap<String, String> paramsReal = new HashMap<String, String>();
		for (Entry<String, String> param : urlTemplate.getParameters().entrySet()) {
			replaceAndSet(param, paramsReal);
		}
		return paramsReal;
	}

	/** get real headers after template replacement */
	public HashMap<String, String> generateHeaders() {
		HashMap<String, String> headersReal = new HashMap<String, String>();
		for (Entry<String, String> header : urlTemplate.getHeaders().entrySet()) {
			replaceAndSet(header, headersReal);
		}
		return headersReal;
	}
	
	/** replace all variable in a string template */
	private String replace(String value) {
		value = templateReplaceAll(value, templateValues);
		value = templateReplaceAllByLookup(value);
		return value;
	}
	
	/**
	 * replace all variables in a map entry, and put values in a new map
	 * @param entry
	 * @param target
	 */
	private void replaceAndSet(Entry<String, String> entry, Map<String, String> target) {
		String key = entry.getKey();
		String value = entry.getValue();
		key = templateReplaceAll(key, templateValues);
		key = templateReplaceAllByLookup(key);
		value = templateReplaceAll(value, templateValues);
		value = templateReplaceAllByLookup(value);
		target.put(key, value);
	}
	
	private String templateReplaceAll(String template, Map<String, String> values) {
		for (Entry<String, String> entry : values.entrySet()) {
			template = template.replaceAll(entry.getKey(), entry.getValue());
		}
		return template;
	}
	
	private String templateReplaceAllByLookup(String template) {
		if (globalContext != null) {
			String pivotValue = getTemplateValue(globalContext.getPivotKey());
			for (String variable : urlTemplate.getVariableNames()) {
				if (globalContext.hasName(pivotValue, variable)) {
					String value = globalContext.<String>getValueByName(pivotValue, variable);
					template = template.replaceAll(UrlTemplate.encodeIfNeeded(variable), value);
				}
			}
		}
		return template;
	}
	
}
