package org.lightj.task.asynchttp;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lightj.task.asynchttp.AsyncHttpTask.HttpMethod;
import org.lightj.util.StringUtil;

/**
 * url request
 * @author binyu
 *
 */
public class UrlRequest {

	private HashMap<String, String> templateValues = new HashMap<String, String>();
	final private UrlTemplate urlTemplate;
	
	/** constructor */
	public UrlRequest(UrlTemplate urlTemplate) { 
		this.urlTemplate = urlTemplate; 
	}
	
	public UrlRequest addTemplateValue(String k, String v) {
		this.templateValues.put(k, v);
		return this;
	}
	public UrlRequest addAllTemplateValues(Map<String, String> values) {
		this.templateValues.putAll(values);
		return this;
	}
	public String getTemplateValue(String k) {
		return templateValues.get(k);
	}
	public HashMap<String, String> getTemplateValues() {
		return templateValues;
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
		for (Entry<String, String> entry : templateValues.entrySet()) {
			bodyV = bodyV.replaceAll(entry.getKey(), entry.getValue());
		}
		return bodyV;
	}

	/** get real url after template replacement */
	public String generateUrl() {
		String urlV = urlTemplate.getUrl();
		for (Entry<String, String> entry : templateValues.entrySet()) {
			urlV = urlV.replaceAll(entry.getKey(), entry.getValue());
		}
		return urlV;
	}

	/** get real headers after template replacement */
	public HashMap<String, String> generateHeaders() {
		HashMap<String, String> headersReal = new HashMap<String, String>();
		for (Entry<String, String> header : urlTemplate.getHeaders().entrySet()) {
			String key = header.getKey();
			String value = header.getValue();
			for (Entry<String, String> entry : templateValues.entrySet()) {
				key = key.replaceAll(entry.getKey(), entry.getValue());
				value = value.replaceAll(entry.getKey(), entry.getValue());
			}
			headersReal.put(key, value);
		}
		return headersReal;
	}

	
}
