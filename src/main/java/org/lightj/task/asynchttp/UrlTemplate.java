package org.lightj.task.asynchttp;

import java.util.HashMap;

import org.lightj.task.asynchttp.AsyncHttpTask.HttpMethod;

/**
 * a http request
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
	public void setMethod(HttpMethod method) {
		this.method = method;
	}
	public String getBody() {
		return this.body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	
	public String getUrl() {
		return this.url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	public HashMap<String, String> getHeaders() {
		return headers;
	}
	public UrlTemplate addHeader(String k, String v) {
		this.headers.put(k, v);
		return this;
	}

	public UrlRequest createRequest() {
		UrlRequest another = new UrlRequest();
		another.body = this.body;
		another.headers = new HashMap<String, String>(this.headers);
		another.method = this.method;
		another.url = this.url;
		return another;
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

