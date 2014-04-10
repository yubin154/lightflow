package org.lightj.task.asynchttp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.junit.Test;
import org.lightj.example.task.HttpTaskRequest;
import org.lightj.task.ExecuteOption;
import org.lightj.task.MonitorOption;
import org.lightj.task.asynchttp.AsyncHttpTask.HttpMethod;
import org.lightj.util.JsonUtil;

public class TestUrlTemplate extends TestCase {
	
	public void testHttpTaskRequestJson() throws Exception {
		String[] sites = new String[] {"slc4b01c-9dee.stratus.slc.ebay.com","slc4b01c-accc.stratus.slc.ebay.com"};
		HttpTaskRequest tw2 = new HttpTaskRequest();
		tw2.setTaskType("asyncpoll");
		tw2.setHttpClientType("httpClient");
		tw2.setExecutionOption(new ExecuteOption());
		UrlTemplate template = new UrlTemplate(UrlTemplate.encodeAllVariables("https://host", "host"), HttpMethod.GET, null);
		template.addHeader("content-type", "application/json");
		tw2.setUrlTemplate(template);
		tw2.setHosts(sites);
		
		tw2.setMonitorOption(new MonitorOption(1, 10));
		tw2.setPollTemplate(new UrlTemplate(UrlTemplate.encodeAllVariables("https://host", "host")));
		tw2.setResProcessorName("dummyPollProcessor");
		String tw2Json = JsonUtil.encode(tw2);
		System.out.println(tw2Json);
	}

	@Test
	public void testUrlTemplateJson() throws Exception {
		UrlTemplate template = new UrlTemplate(UrlTemplate.encodeAllVariables("https://host", "host"), HttpMethod.POST, "test");
		template.addHeader("key1", "value1");
		String urlJson = JsonUtil.encode(template);
		System.out.println(urlJson);
		UrlTemplate another = JsonUtil.decode(urlJson, UrlTemplate.class);
		assertTrue(another != null);
	}

	@Test
	public void testUrlRequestJson() throws Exception {
		UrlTemplate template = new UrlTemplate(UrlTemplate.encodeAllVariables("https://host", "host"), HttpMethod.GET, "test");
		template.addHeader("key1", "value1").addHeader("key2", UrlTemplate.encodeIfNeeded("var2"));
		UrlRequest req = new UrlRequest(template);
		req.setHost("www.ebay.com");
		req.addTemplateValue("var2", "value2");
		System.out.println(template.getVariableNames());
		String urlJson = JsonUtil.encode(req);
		System.out.println(urlJson);
		UrlRequest another = JsonUtil.decode(urlJson, UrlRequest.class);
		assertTrue(another != null);
	}

	@Test
	public void testHttpTaskWrapper() throws Exception {
		
		System.out.println("matched=" + "https://#host".matches("^(http|https)://#host.*"));

		String url = "http://<host>:12020/<something>?<somethingelse>=ok";
		String uuidRegex = "<(.+?)>";
		System.out.println(url.matches(uuidRegex));
		Pattern r = Pattern.compile(uuidRegex);
		Matcher m = r.matcher(url);
		while (m.find()) {
			System.out.println("Found group count: " + m.groupCount());
			System.out.println("Found value: " + m.group(m.groupCount()));
		} 
//		uuidRegex = ".*/status/(.*)\\\".*";
//		String line = "\"/status/1b6db6f6-3d0a-49d6-949b-9a069ad86a69\"";
//		System.out.println(line.matches(uuidRegex));
//		r = Pattern.compile(uuidRegex);
//		m = r.matcher(line);
//		if (m.find()) {
//			System.out.println("Found value: " + m.group(1));
//		} else {
//			System.out.println("NO MATCH");
//		}

		HttpTaskRequest tw = new HttpTaskRequest();
		tw.setTaskType("async");
		tw.setHttpClientType("httpClient");
		tw.setExecutionOption(new ExecuteOption());
		tw.setMonitorOption(new MonitorOption(1, 5));
		tw.setUrlTemplate(new UrlTemplate(UrlTemplate.encodeAllVariables("https://host", "host")));
		tw.setHosts("www.yahoo.com");
		String twJson = JsonUtil.encode(tw);
		System.out.println(twJson);
		try {
			HttpTaskRequest another = JsonUtil.decode(twJson,
					HttpTaskRequest.class);
			assertTrue(another != null);
		} catch (Throwable t) {
			t.printStackTrace();
		}

	}
}
