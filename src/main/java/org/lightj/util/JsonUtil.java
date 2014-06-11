package org.lightj.util;

import java.io.IOException;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonUtil {

	static final ObjectMapper mapper = customMapper("yyyy-MM-dd HH:mm:ss.SSSZZZ");
	
	public static final ObjectMapper customMapper(String dateFormat) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.setDateFormat(new SimpleDateFormat(dateFormat));
		return mapper;
	}

	public static final String encode(Object value) throws JsonGenerationException, JsonMappingException, IOException {
		return mapper.writeValueAsString(value);
 	}
	
	public static final String encodePretty(Object value) throws JsonGenerationException, JsonMappingException, IOException {
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
 	}

	public static final <T> T decode(String jsonStr, TypeReference<T> typeRef) throws JsonParseException, JsonMappingException, IOException {
		return "null".equalsIgnoreCase(jsonStr) ? null : mapper.<T>readValue(jsonStr, typeRef);
	}
	
	public static final <T> T decode(String jsonStr, Class<T> klazz) throws JsonParseException, JsonMappingException, IOException {
		return "null".equalsIgnoreCase(jsonStr) ? null : mapper.readValue(jsonStr, klazz);
	}

}

