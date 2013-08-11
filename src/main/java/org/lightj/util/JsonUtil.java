package org.lightj.util;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.type.TypeReference;

public class JsonUtil {

	static final ObjectMapper mapper = new ObjectMapper();
	
	static {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		SerializationConfig serConfig = mapper.getSerializationConfig();
		serConfig.setDateFormat(dateFormat);
		DeserializationConfig deserializationConfig = mapper.getDeserializationConfig();
		deserializationConfig.setDateFormat(dateFormat);
		mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
		mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
		mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	public static final String encode(Object value) throws JsonGenerationException, JsonMappingException, IOException {
		return mapper.writeValueAsString(value);
 	}
	
	public static final <T> T decode(String jsonStr, TypeReference<T> typeRef) throws JsonParseException, JsonMappingException, IOException {
		return "null".equalsIgnoreCase(jsonStr) ? null : mapper.<T>readValue(jsonStr, typeRef);
	}
	
	public static final <T> T decode(String jsonStr, Class<T> klazz) throws JsonParseException, JsonMappingException, IOException {
		return "null".equalsIgnoreCase(jsonStr) ? null : mapper.readValue(jsonStr, klazz);
	}

	public static final <T> T decode(Map<String, String> jsonMap, Class<T> klazz) throws JsonParseException, JsonMappingException, IOException {
		return mapper.convertValue(jsonMap, klazz);
	}
}

