package org.lightj.task.asynchttp;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
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
	private ITemplateValueLookupFunction templateValueLookup;
	
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
		String key = k.matches(UrlTemplate.VAR_PATTERN) ? k : String.format("#:%s:#", k);
		if (urlTemplate.hasVariableKey(key)) {
			templateValues.put(key, v);
		}
		else {
			logger.warn("UrlTemplate for this UrlRequest does not have variable " + k);
		}
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
		return templateValues.containsKey("#:host:#") ? templateValues.get("#:host:#") : null;
	}
	@JsonIgnore
	public ITemplateValueLookupFunction getTemplateValueLookup() {
		return templateValueLookup;
	}
	public void setTemplateValueLookup(
			ITemplateValueLookupFunction templateValueLookup) {
		this.templateValueLookup = templateValueLookup;
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
		String urlV = urlTemplate.getUrl();
		urlV = templateReplaceAll(urlV, templateValues);
		urlV = templateReplaceAllByLookup(urlV);
		return urlV;
	}

	/** get real headers after template replacement */
	public HashMap<String, String> generateHeaders() {
		HashMap<String, String> headersReal = new HashMap<String, String>();
		for (Entry<String, String> header : urlTemplate.getHeaders().entrySet()) {
			String key = header.getKey();
			String value = header.getValue();
			key = templateReplaceAll(key, templateValues);
			key = templateReplaceAllByLookup(key);
			value = templateReplaceAll(value, templateValues);
			value = templateReplaceAllByLookup(value);
			headersReal.put(key, value);
		}
		return headersReal;
	}
	
	private String templateReplaceAll(String template, Map<String, String> values) {
		for (Entry<String, String> entry : values.entrySet()) {
			template = template.replaceAll(entry.getKey(), entry.getValue());
		}
		return template;
	}
	
	private String templateReplaceAllByLookup(String template) {
		if (templateValueLookup != null) {
			String pivotValue = getTemplateValue(templateValueLookup.getPivotVariableName());
			Map<String, String> externalTemplateValues = templateValueLookup.lookupByPivotValue(pivotValue);
			template = templateReplaceAll(template, externalTemplateValues);
		}
		return template;
	}
	
	/**
	 * externally lookup template value
	 * 
	 * @author binyu
	 *
	 */
	public interface ITemplateValueLookupFunction {
		
		/** what value to use to lookup externally as a key */
		public String getPivotVariableName();
		
		/** additional nv pairs to replace from external */
		public Map<String, String> lookupByPivotValue(String pivotValue);
	}

	
}
