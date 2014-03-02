package org.lightj.task.asynchttp;

import java.util.HashMap;

import org.lightj.task.asynchttp.AsyncHttpTask.HttpMethod;

/**
 * a http request template, typically immutable
 * @author binyu
 *
 */
public class UrlTemplate {
	
	protected String url;
	protected HttpMethod method;
	protected String body;
	protected HashMap<String, String> headers = new HashMap<String, String>();
	
	/** constructor */
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

	public void setUrl(String url) {
		this.url = url;
	}
	public void setMethod(HttpMethod method) {
		this.method = method;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public void setHeaders(HashMap<String, String> headers) {
		this.headers = headers;
	}

	public UrlRequest createRequest(String...nvp) {
		if (nvp.length%2 != 0) {
			throw new IllegalArgumentException("Replacement nvp has to be in pairs");
		}
		UrlRequest another = this.createRequest();
		for (int i = 0; i < nvp.length; i+=2) {
			another.addTemplateValue(nvp[i], nvp[i+1]);
		}
		return another;
	}

}

