package org.lightj.task.asynchttp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lightj.task.asynchttp.AsyncHttpTask.HttpMethod;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * a http request template, typically immutable
 * use <> as wrapper delimiters for variables
 * this class abstracts a RESTful http request, with url, method, parameter, body, headers 
 * 
 * @author binyu
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class UrlTemplate {
	
	static final String URL_PATTERN = "^(http|https)://<.+?>.*";
	static final String VAR_PATTERN = "<(.+?)>";	
	static final Pattern r = Pattern.compile(VAR_PATTERN);

	
	/** url */
	protected String url;
	/** http method */
	protected HttpMethod method;
	/** http body */
	protected HashMap<String, String> body = new HashMap<String, String>();
	/** headers */
	protected HashMap<String, String> headers = new HashMap<String, String>();
	/** url query sring GET, or parameters POST */
	protected HashMap<String, String> parameters = new HashMap<String, String>();
	
	/** constructor */
	public UrlTemplate() {}
	public UrlTemplate(String url) {
		this(url, HttpMethod.GET);
	}
	public UrlTemplate(String url, HttpMethod method) {
		this.url = url;
		this.method = method;
		validateUrl();		
	}
	
	public HttpMethod getMethod() {
		return method;
	}
	public HashMap<String, String> getBody() {
		return this.body;
	}
	public UrlTemplate addBodyParam(String k, String v) {
		this.body.put(k, v);
		return this;
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
	public UrlTemplate setBody(HashMap<String, String> body) {
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
  		sources.addAll(body.keySet());
  		sources.addAll(body.values());
  		sources.addAll(headers.keySet());
  		sources.addAll(headers.values());
  		sources.addAll(parameters.keySet());
  		sources.addAll(parameters.values());
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
  		sources.addAll(body.keySet());
  		sources.addAll(body.values());
  		sources.addAll(headers.keySet());
  		sources.addAll(headers.values());
  		sources.addAll(parameters.keySet());
  		sources.addAll(parameters.values());
		String vName = key.matches(UrlTemplate.VAR_PATTERN) ? key : encodeIfNeeded(key);
		for (String str : sources) {
			if (str.indexOf(vName) >= 0) {
				return true;
			}
		}
		return false;
	}
	private void validateUrl() {
		if (!this.url.matches(URL_PATTERN)) {
			throw new IllegalArgumentException("url must be in the format of http(s)://<host>...");
		}
	}
	
	public UrlTemplate createNew() {
		UrlTemplate newUrl = new UrlTemplate();
		newUrl.body = this.body;
		newUrl.headers.putAll(this.headers);
		newUrl.method = this.method;
		newUrl.parameters.putAll(this.parameters);
		newUrl.url = this.url;
		return newUrl;
	}
	
	public static String encodeIfNeeded(String variableName) {
		return variableName.matches(UrlTemplate.VAR_PATTERN) ? variableName : String.format("<%s>", variableName);
	}
	
	public static String encodeAllVariables(String source, String...variables) {
		for (String variable : variables) {
			source = source.replace(variable, encodeIfNeeded(variable));
		}
		return source;
	}
	
	public static boolean containVariable(String source) {
		return source.matches(UrlTemplate.VAR_PATTERN);
	}

}

