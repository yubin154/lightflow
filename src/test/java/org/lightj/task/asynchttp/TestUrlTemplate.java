package org.lightj.task.asynchttp;

import junit.framework.TestCase;

import org.junit.Test;
import org.lightj.task.asynchttp.UrlRequest;
import org.lightj.task.asynchttp.UrlTemplate;
import org.lightj.util.JsonUtil;

public class TestUrlTemplate extends TestCase {

	@Test
	public void testUrlTemplateJson() throws Exception {
		UrlTemplate template = new UrlTemplate("http://www.ebay.com");
		template.setBody("test");
		template.addHeader("key1", "value1");
		String urlJson = JsonUtil.encode(template);
		System.out.println(urlJson);
		UrlTemplate another = JsonUtil.decode(urlJson, UrlTemplate.class);
		System.out.println(another);
	}
	
	@Test
	public void testUrlRequestJson() throws Exception {
		UrlRequest req = new UrlRequest("http://$host");
		req.addHeader("key1", "value1").addHeader("key2", "value2");
		req.addTemplateValue("$host", "www.ebay.com");
		req.setBody("test");
		String urlJson = JsonUtil.encode(req);
		System.out.println(urlJson);
		UrlRequest another = JsonUtil.decode(urlJson, UrlRequest.class);
	}
}
