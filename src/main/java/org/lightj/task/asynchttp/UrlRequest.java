package org.lightj.task.asynchttp;

import java.util.HashMap;
import java.util.Map.Entry;

import org.lightj.task.asynchttp.AsyncHttpTask.HttpMethod;

/**
 * url request
 * @author binyu
 *
 */
public class UrlRequest extends UrlTemplate {

	private HashMap<String, String> replacement = new HashMap<String, String>();

	/** constructor */
	public UrlRequest() { super(); }
	public UrlRequest(String url) {
		super(url);
	}
	public UrlRequest(String url, HttpMethod method, String body) {
		super(url, method, body);
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
	
	/** get real body after replacing template */
	public String getBodyReal() {
		String bodyV = body;
		for (Entry<String, String> entry : replacement.entrySet()) {
			bodyV = bodyV.replaceAll(entry.getKey(), entry.getValue());
		}
		return bodyV;
	}

	/** get real url after template replacement */
	public String getUrlReal() {
		String urlV = url;
		for (Entry<String, String> entry : replacement.entrySet()) {
			urlV = urlV.replaceAll(entry.getKey(), entry.getValue());
		}
		return urlV;
	}

	/** get real headers after template replacement */
	public HashMap<String, String> getHeadersReal() {
		HashMap<String, String> headersReal = new HashMap<String, String>();
		for (Entry<String, String> header : headers.entrySet()) {
			String key = header.getKey();
			String value = header.getValue();
			for (Entry<String, String> entry : replacement.entrySet()) {
				key = key.replaceAll(entry.getKey(), entry.getValue());
				value = value.replaceAll(entry.getKey(), entry.getValue());
			}
			headersReal.put(key, value);
		}
		return headersReal;
	}

	
}