package org.lightj.task.asynchttp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.lightj.task.asynchttp.AsyncHttpTask.HttpMethod;

/**
 * a http request template, typically immutable
 * use #:...:# as delimiter for variables
 * 
 * @author binyu
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class UrlTemplate {
	
	static final String URL_PATTERN = "^(http|https)://#:host:#.*";
	static final String VAR_PATTERN = "#:(.+?):#";
	
	static final Pattern r = Pattern.compile(VAR_PATTERN);

	
	/** url */
	protected String url;
	/** http method */
	protected HttpMethod method;
	/** http body */
	protected String body;
	/** headers */
	protected HashMap<String, String> headers = new HashMap<String, String>();
	/** url query sring GET, or parameters POST */
	protected HashMap<String, String> parameters = new HashMap<String, String>();
	
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
	public HashMap<String, String> getParameters() {
		return parameters;
	}
	public void setParameters(HashMap<String, String> parameters) {
		this.parameters = parameters;
	}
	public UrlTemplate addParameters(String k, String v) {
		this.parameters.put(k, v);
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
	@JsonIgnore
	public Set<String> getVariableNames() {
		HashSet<String> variableNames = new HashSet<String>();
  		ArrayList<String> sources = new ArrayList<String>();
  		sources.add(url);
  		if (body!=null) sources.add(body);
  		sources.addAll(headers.values());
		for (String str : sources) {
			Matcher m = r.matcher(str);
			while (m.find()) {
				variableNames.add(m.group(m.groupCount()));
			} 
		}
		return variableNames;
	}
	@JsonIgnore
	public boolean hasVariableKey(String key) {
  		ArrayList<String> sources = new ArrayList<String>();
  		sources.add(url);
  		if (body!=null) sources.add(body);
  		sources.addAll(headers.values());
		for (String str : sources) {
			if (str.matches(String.format(".*%s.*", key))) {
				return true;
			}
		}
		return false;
	}
	private void validateUrl() {
		if (!this.url.matches(URL_PATTERN)) {
			throw new IllegalArgumentException("url must be in the format of http(s)://#:host:#...");
		}
	}

}

