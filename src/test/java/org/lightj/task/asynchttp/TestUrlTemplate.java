package org.lightj.task.asynchttp;

import junit.framework.TestCase;

import org.junit.Test;
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
		System.out.println(another);
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
	}
}
