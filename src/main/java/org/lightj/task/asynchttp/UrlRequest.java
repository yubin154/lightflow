package org.lightj.task.asynchttp;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.lightj.task.IGlobalContext;
import org.lightj.task.asynchttp.AsyncHttpTask.HttpMethod;
import org.lightj.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private HashMap<String, String> templateValues = new HashMap<String, String>();
	
	/** url template, typically immutable and contains variables */
	private UrlTemplate urlTemplate;
	
	/** optionally lookup template value from external through this function */
	private IGlobalContext globalContext;
	
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

	/**
	 * dont use this directly, use {@link #addTemplateValue(String, String)} or {@link #addAllTemplateValues(Map)} instead
	 * @param templateValues
	 */
	public void setTemplateValues(HashMap<String, String> templateValues) {
		this.templateValues = templateValues;
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
	protected UrlRequest putTemplateValuesIfNull(Map<String, String> values) {
		for (Entry<String, String> value : values.entrySet()) {
			if (!templateValues.containsKey(value.getKey())) {
				templateValues.put(value.getKey(), value.getValue());
			}
		}
		return this;
	}
	public String getTemplateValue(String k) {
		return templateValues.get(k);
	}
	public HashMap<String, String> getTemplateValues() {
		return templateValues;
	}
	public UrlRequest setHost(String host) {
		this.addTemplateValue("host", host);
		return this;
	}
	public String getHost() {
		String encodedHost = UrlTemplate.encodeIfNeeded("host");
		return templateValues.containsKey(encodedHost) ? templateValues.get(encodedHost) : null;
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
		return !StringUtil.isNullOrEmpty(urlTemplate.getBody());
	}
	/** get real body after replacing template */
	public String generateBody() {
		String bodyV = urlTemplate.getBody();
		bodyV = templateReplaceAll(bodyV, templateValues);
		bodyV = templateReplaceAllByLookup(bodyV);
		return bodyV;
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
	
	private String replace(String value) {
		value = templateReplaceAll(value, templateValues);
		value = templateReplaceAllByLookup(value);
		return value;
	}
	
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
			String pivotValue = globalContext.getPivotValue();
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
