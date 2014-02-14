package org.lightj.task.asynchttp;

import java.util.HashMap;
import java.util.Map.Entry;

import org.lightj.task.asynchttp.AsyncHttpTask.HttpMethod;


public class UrlTemplate {
	
	private String url;
	private String host;
	private HttpMethod method;
	private String body;
	private HashMap<String, String> headers = new HashMap<String, String>();
	private HashMap<String, String> replacement = new HashMap<String, String>();
	private boolean isStatus = false;
	public UrlTemplate() {}
	public UrlTemplate(String url) {
		this.url = url;
		this.method = HttpMethod.GET;
	}
	public UrlTemplate(String url, HttpMethod method, String body) {
		this.url = url;
		this.method = method;
		this.body = body;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public boolean isStatus() {
		return isStatus;
	}
	public void setStatus(boolean isStatus) {
		this.isStatus = isStatus;
	}
	public HttpMethod getMethod() {
		return method;
	}
	public void setMethod(HttpMethod method) {
		this.method = method;
	}
	public String getBody() {
		String bodyV = body;
		for (Entry<String, String> entry : replacement.entrySet()) {
			bodyV = bodyV.replaceAll(entry.getKey(), entry.getValue());
		}
		return bodyV;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public UrlTemplate addHeader(String k, String v) {
		this.headers.put(k, v);
		return this;
	}
	public UrlTemplate addVariableReplacement(String k, String v) {
		this.replacement.put(k, v);
		return this;
	}
	public String getVariableReplacement(String k) {
		return replacement.get(k);
	}
	public HashMap<String, String> getReplacement() {
		return replacement;
	}
	public String getUrl() {
		String urlV = url;
		for (Entry<String, String> entry : replacement.entrySet()) {
			urlV = urlV.replaceAll(entry.getKey(), entry.getValue());
		}
		return urlV;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public HashMap<String, String> getHeaders() {
		return headers;
	}
	public UrlTemplate makeCopy() {
		UrlTemplate another = new UrlTemplate();
		another.body = this.body;
		another.headers = new HashMap<String, String>(this.headers);
		another.isStatus = this.isStatus;
		another.method = this.method;
		another.replacement = new HashMap<String, String>(this.replacement);
		another.url = this.url;
		return another;
	}

}

