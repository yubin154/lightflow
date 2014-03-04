package org.lightj.task.asynchttp;

import java.util.HashMap;

import junit.framework.TestCase;

import org.junit.Test;
import org.lightj.example.session.simplehttpflow.HttpTaskUtil.HttpTaskWrapper;
import org.lightj.task.ExecuteOption;
import org.lightj.task.MonitorOption;
import org.lightj.task.asynchttp.AsyncHttpTask.HttpMethod;
import org.lightj.util.JsonUtil;

public class TestUrlTemplate extends TestCase {

	@Test
	public void testUrlTemplateJson() throws Exception {
		UrlTemplate template = new UrlTemplate("http://www.ebay.com", HttpMethod.POST, "test");
		template.addHeader("key1", "value1");
		String urlJson = JsonUtil.encode(template);
		System.out.println(urlJson);
		UrlTemplate another = JsonUtil.decode(urlJson, UrlTemplate.class);
		assertTrue(another != null);
	}
	
	@Test
	public void testUrlRequestJson() throws Exception {
		UrlTemplate template = new UrlTemplate("http://$host", HttpMethod.GET, "test");
		template.addHeader("key1", "value1").addHeader("key2", "value2");
		UrlRequest req = new UrlRequest(template);
		req.addTemplateValue("$host", "www.ebay.com");
		String urlJson = JsonUtil.encode(req);
		System.out.println(urlJson);
		UrlRequest another = JsonUtil.decode(urlJson, UrlRequest.class);
		assertTrue(another != null);
	}
	
	@Test
	public void testHttpTaskWrapper() throws Exception {
		HttpTaskWrapper tw = new HttpTaskWrapper();
		tw.setTaskType("async");
		tw.setHttpClientType("httpClient");
		tw.setExecutionOption(new ExecuteOption());
		tw.setMonitorOption(new MonitorOption(1000, 5000));
		tw.setUrlTemplate(new UrlTemplate("https://#host"));
		HashMap<String, String> tv = new HashMap<String, String>();
		tv.put("#host", "www.yahoo.com");
		tw.setTemplateValues(tv);
		String twJson = JsonUtil.encode(tw);
		System.out.println(twJson);
		try {
			HttpTaskWrapper another = JsonUtil.decode(twJson, HttpTaskWrapper.class);
			assertTrue(another != null);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	
	}
}
