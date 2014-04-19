package org.lightj.util;

import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

public class TestUtils {
	
	@SuppressWarnings("rawtypes")
	@Test
    public void testMapListPrimitiveJsonParser() throws Exception {
		String json = "{" +
				"\"commands\" : [\"AGENT_CREATE_SERVICE\", \"AGENT_DELETE_SERVICE\"], " +
				"\"cmdUserData\" : {\"AGENT_CREATE_SERVICE\" : {\"serviceName\": \"myservice\"}, \"AGENT_DELETE_SERVICE\": {\"serviceName\": \"myservice\"}}" +
				"}";
		Object result = MapListPrimitiveJsonParser.parseJson(json);
		String encodedJson = JsonUtil.encode(result);
		System.out.println(encodedJson);
		Assert.assertTrue(result instanceof Map);
		Assert.assertTrue(((Map) result).get("commands") instanceof List);
		Assert.assertTrue( ((List) ((Map) result).get("commands")).get(0) instanceof String);
		Assert.assertTrue(((Map) result).get("cmdUserData") instanceof Map);
		Assert.assertTrue( ((Map) ((Map) result).get("cmdUserData")).get("AGENT_CREATE_SERVICE") instanceof Map);
		Assert.assertEquals( ((Map) ((Map) ((Map) result).get("cmdUserData")).get("AGENT_CREATE_SERVICE")).get("serviceName"), "myservice");
		Assert.assertEquals(encodedJson, json.replaceAll(" ", ""));
    }
    

}
