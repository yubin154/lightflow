package org.lightj.task.asynchttp;

import java.io.IOException;

import org.lightj.util.StringUtil;

import com.ning.http.client.Response;

/**
 * capture http response
 * @author binyu
 *
 */
public class SimpleHttpResponse {
	
	private int statusCode;
	private String responseBody;
	private String contentType;
	public SimpleHttpResponse() {}
	public SimpleHttpResponse(int statusCode, String contentType, String responseBody) {
		this.statusCode = statusCode;
		this.contentType = contentType;
		this.responseBody = responseBody;
	}
	public SimpleHttpResponse(Response response) {
		this(response.getStatusCode(), response.getContentType(), null);
		try {
			this.responseBody = response.getResponseBody();
		} catch (IOException e) {
			this.responseBody = StringUtil.getStackTrace(e);
		}
	}
	public int getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	public String getResponseBody() {
		return responseBody;
	}
	public void setResponseBody(String responseBody) {
		this.responseBody = responseBody;
	}
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public String toString() {
		return String.format("statusCode=%s,contentType=%s,responseBody=%s", statusCode, contentType, responseBody);
	}

}
