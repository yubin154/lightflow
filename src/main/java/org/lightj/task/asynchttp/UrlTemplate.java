package org.lightj.task.asynchttp;

import java.util.HashMap;

import org.lightj.task.asynchttp.AsyncHttpTask.HttpMethod;

/**
 * a http request template, typically immutable
 * 
 * @author binyu
 *
 */
public class UrlTemplate {
	
	/** url */
	protected String url;
	/** http method */
	protected HttpMethod method;
	/** http body */
	protected String body;
	/** headers */
	protected HashMap<String, String> headers = new HashMap<String, String>();
	
	/** constructor */
	public UrlTemplate() {}
	public UrlTemplate(String url) {
		this(url, HttpMethod.GET, null);
	}
	public UrlTemplate(String url, HttpMethod method, String body) {
		this.url = url;
		this.method = method;
		this.body = body;
		validateUrl();		
	}
	
	public HttpMethod getMethod() {
		return method;
	}
	public String getBody() {
		return this.body;
	}
	public String getUrl() {
		return this.url;
	}
	public HashMap<String, String> getHeaders() {
		return headers;
	}
	public UrlTemplate addHeader(String k, String v) {
		this.headers.put(k, v);
		return this;
	}

	public UrlTemplate setUrl(String url) {
		this.url = url;
		validateUrl();
		return this;
	}
	public void setMethod(HttpMethod method) {
		this.method = method;
	}
	public UrlTemplate setBody(String body) {
		this.body = body;
		return this;
	}
	public UrlTemplate setHeaders(HashMap<String, String> headers) {
		this.headers = headers;
		return this;
	}
	private void validateUrl() {
		if (!this.url.matches("^(http|https)://#host.*")) {
			throw new IllegalArgumentException("url must be in the format of http(s)://#host...");
		}
	}

}

