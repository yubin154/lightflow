package org.lightj.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;


/**
 * Utility functions for serializing/deserializing json object/string.
 */
public final class MapListPrimitiveJsonParser
{
    private static final Logger log = LoggerFactory.getLogger(MapListPrimitiveJsonParser.class);
    private static ObjectMapper jsonObjectMapper = null;

    private MapListPrimitiveJsonParser() {}

    // TBD:
    private static ObjectMapper getJsonObjectMapper()
    {
        if(jsonObjectMapper == null) {
            jsonObjectMapper = new ObjectMapper();
            // jsonObjectMapper.setSerializationInclusion(Inclusion.NON_EMPTY);
            // jsonObjectMapper.setSerializationInclusion(Inclusion.NON_NULL);
            // jsonObjectMapper.setDateFormat(DateFormat.getInstance());
            // ...
        }
        return jsonObjectMapper;
    }


    // temporary
    private static int GLOBAL_NODE_COUNTER_FOR_DEBUGTRACING = 0;

    /**
     * Parse the json string into a structure comprising only three types: a map, a list, and a scalar value.
     * 1) Map of {String -> Object}. The value type can be a map, a list, or a scalar value.
     * 2) List of {Object}. Ditto.
     * 3) Any other objects, primitive type, Object, a collection other than list and map, are treated as scalar,
     *    and they are treated as leaf nodes. (Even if an object has an internal structure, we do not have the type information.)
     *    Note that a leaf node object cannot be instantiated as an object (it's only a string). 
     * 
     * @param jsonStr Input JSON string representing a map, a list, or an object/primitive type.
     * @return The object deserialized from the jsonStr.
     */
    public static Object parseJson(String jsonStr)
    {
        if(jsonStr == null) {
            log.warn("Input jsonStr is null.");
            return null;
        }

        GLOBAL_NODE_COUNTER_FOR_DEBUGTRACING = 0;
        Object jsonObj = null;
        try {
            JsonFactory factory = new JsonFactory();
            ObjectMapper om = getJsonObjectMapper();  // ???? 
            factory.setCodec(om);                     // Do we need this?
            JsonParser parser = factory.createJsonParser(jsonStr);

            JsonNode topNode = parser.readValueAsTree();
            if(topNode != null) {
                // ++GLOBAL_NODE_COUNTER_FOR_DEBUGTRACING;
                if(topNode.isObject()) {
                    jsonObj = parseJsonMap(topNode);
                } else if(topNode.isArray()) {
                    jsonObj = parseJsonList(topNode);
                } else {
                    // Leaf node
                    ++GLOBAL_NODE_COUNTER_FOR_DEBUGTRACING;
                    String value = topNode.asText();   // toString() vs. asText() ????
                    log.debug("jsonMap: counter = " + GLOBAL_NODE_COUNTER_FOR_DEBUGTRACING + "; TopNode value" + value);
                    jsonObj = value;
                }
            } else {
                // ???
                log.info("Failed to parse jsonStr = " + jsonStr);
            }
        } catch (JsonParseException e) {
            log.warn("Failed to parse jsonStr = " + jsonStr, e);
        } catch (JsonProcessingException e) {
            log.warn("Failed to process jsonStr = " + jsonStr, e);
        } catch (IOException e) {
            log.warn("Exception while processing jsonStr = " + jsonStr, e);
        } catch (Exception e) {
            log.warn("Unknownn exception while processing jsonStr = " + jsonStr, e);
        }
        return jsonObj;
    }

    // Parse a json map
    private static Map<String, Object> parseJsonMap(JsonNode parentNode)
    {
        if(parentNode == null || !parentNode.isObject()) {
            // This should not happen.
            log.warn("Invalid argument: parentNode is not a map.");
        }

        Map<String, Object> jsonObject = new LinkedHashMap<String, Object>();
        try {
            Iterator<String> fieldNames = parentNode.fieldNames();
            while(fieldNames.hasNext()) {
                // ++GLOBAL_NODE_COUNTER_FOR_DEBUGTRACING;
                String name = fieldNames.next();
                JsonNode node = parentNode.get(name);
                if(node == null) {
                    // Can this happen?
                    log.info("Empty/null node found: name = " + name);
                    continue;
                }
                if(node.isArray()) {
                    List<Object> childList = parseJsonList(node);
                    jsonObject.put(name, childList);
                } else if(node.isObject()) {
                    Map<String,Object> childMap = parseJsonMap(node);
                    jsonObject.put(name, childMap);
                } else {
                    // Leaf node
                    ++GLOBAL_NODE_COUNTER_FOR_DEBUGTRACING;
                    String value = node.asText();
                    log.debug("jsonMap: counter = " + GLOBAL_NODE_COUNTER_FOR_DEBUGTRACING + "; name = " + name + "; value" + value);
                    jsonObject.put(name, value);
                }
            }
        } catch (Exception e) {
            log.warn("Exception while processing parentNode = " + parentNode, e);
        }
        return jsonObject;
    }

    // Parses a json list.
    private static List<Object> parseJsonList(JsonNode parentNode)
    {
        if(parentNode == null || !parentNode.isArray()) {
            // This should not happen.
            log.warn("Invalid argument: parentNode is not a list.");
        }

        List<Object> jsonArray = new ArrayList<Object>();
        try {
            for(Iterator<JsonNode> elements = parentNode.iterator(); elements.hasNext(); ) {
                // ++GLOBAL_NODE_COUNTER_FOR_DEBUGTRACING;
                JsonNode node = elements.next(); 
                if(node == null) {
                    // Can this happen?
                    log.info("Empty/null node found.");
                    continue;
                }
                if(node.isArray()) {
                    List<Object> childList = parseJsonList(node);
                    jsonArray.add(childList);
                } else if(node.isObject()) {
                    Map<String,Object> childMap = parseJsonMap(node);
                    jsonArray.add(childMap);
                } else {
                    // Leaf node
                    ++GLOBAL_NODE_COUNTER_FOR_DEBUGTRACING;
                    String value = node.asText();
                    log.debug("jsonList: counter = " + GLOBAL_NODE_COUNTER_FOR_DEBUGTRACING + "; element: value" + value);
                    jsonArray.add(value);
                }
            }
        } catch (Exception e) {
            log.warn("Exception while processing parentNode = " + parentNode, e);
        }
        return jsonArray;
    }



    /**
     * Returns a JSON string for the given object.
     * The input can be a map of {String -> Object}, a list of {Object}, or an Object/primitive type.
     * It recursively traverses each element as long as they are a list or a map.
     * All other elements (primitive, object, a collection type other than list and map) are treated as leaf nodes.
     * Objects are converted to a string using toString().
     * (Note: Even if an object has an internal structure, we do not have the type information.)
     * 
     * @param jsonObj An object that is to be converted to JSON string.
     * @return The json string representation of jsonObj.
     */
    @SuppressWarnings("unchecked")
	public static String buildJson(Object jsonObj)
    {
        String jsonStr = null;
        JsonNodeFactory factory = JsonNodeFactory.instance;   // ?????
        JsonNode topNode = null;
        if(jsonObj instanceof Map<?,?>) {
            Map<String,Object> map = (Map<String,Object>) jsonObj;
            topNode = buildJsonObject(map, factory);
        } else if(jsonObj instanceof List<?>) {
            List<Object> list = (List<Object>) jsonObj;
            topNode = buildJsonArray(list, factory);
        } else {
            if(jsonObj instanceof Boolean) {
                Boolean b = (Boolean) jsonObj;
                topNode = BooleanNode.valueOf(b);
            } else if(jsonObj instanceof Character) {
                // Note: char is treated as String (not as int).
                String str = Character.toString((Character) jsonObj);
                topNode = new TextNode(str);
            } else if(jsonObj instanceof Byte) {
                Byte b = (Byte) jsonObj;
                topNode = IntNode.valueOf(b);
            } else if(jsonObj instanceof Short) {
                Short b = (Short) jsonObj;
                topNode = IntNode.valueOf(b);
            } else if(jsonObj instanceof Integer) {
                Integer b = (Integer) jsonObj;
                topNode = IntNode.valueOf(b);
            } else if(jsonObj instanceof Long) {
                Long b = (Long) jsonObj;
                topNode = LongNode.valueOf(b);
            } else if(jsonObj instanceof Float) {
                Float b = (Float) jsonObj;
                topNode = DoubleNode.valueOf(b);
            } else if(jsonObj instanceof Double) {
                Double b = (Double) jsonObj;
                topNode = DoubleNode.valueOf(b);
            } else if(jsonObj instanceof String) {
                String b = (String) jsonObj;
                topNode = new TextNode(b);
            } else {
                String value = jsonObj.toString();
                if(value != null) {
                    topNode = new TextNode(value);
                } else {
                    // ?????
                    log.debug("TopNode value is null.");
                    // topNode = null;   // ???
                }
            }
        }
        if(topNode != null) {
            jsonStr = topNode.toString();
        } else {
            log.info("Failed to generate a JSON string for the given jsonObj = " + jsonObj);
        }
        log.debug("buildJson(): jsonStr = " + jsonStr);
        return jsonStr;
    }

    // Creates a JsonNode for the given map. 
    private static ObjectNode buildJsonObject(Map<String,Object> map, JsonNodeFactory factory)
    {
        if(map == null) {
            log.info("Argument map is null.");
            return null;
        }
        if(factory == null) {
            log.warn("Argument factory is null");
            return null;  // ????
        }
        
        ObjectNode jsonObj = new ObjectNode(factory);
        for(String key : map.keySet()) {
            Object o = map.get(key);
            
            if(o instanceof Map<?,?>) {
                @SuppressWarnings("unchecked")
                Map<String,Object> m = (Map<String,Object>) o;
                ObjectNode jo = buildJsonObject(m, factory);
                if(jo != null) {
                    jsonObj.put(key, jo);
                } else {
                    // ????
                    log.debug("Value object is null for key = " + key);
                    jsonObj.put(key, jo);
                }
            } else if(o instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Object> l = (List<Object>) o;
                ArrayNode ja = buildJsonArray(l, factory);
                if(ja != null) {
                    jsonObj.put(key, ja);
                } else {
                    // ????
                    log.debug("Value array is null for key = " + key);
                    jsonObj.put(key, ja);
                }
            } else {
                // Should be a "primitive type" or an object.
                // Or, everything else (including collection which is not a list or a map...)
                // We always convert them to text (except for boxed values of primitive types).
                if(o instanceof Boolean) {
                    Boolean b = (Boolean) o;
                    jsonObj.put(key, b);
                } else if(o instanceof Character) {
                    // Note: char is treated as String (not as int).
                    String str = Character.toString((Character) o);
                    jsonObj.put(key, str);
                } else if(o instanceof Byte) {
                    Byte b = (Byte) o;
                    jsonObj.put(key, b);
                } else if(o instanceof Short) {
                    Short b = (Short) o;
                    jsonObj.put(key, b);
                } else if(o instanceof Integer) {
                    Integer b = (Integer) o;
                    jsonObj.put(key, b);
                } else if(o instanceof Long) {
                    Long b = (Long) o;
                    jsonObj.put(key, b);
                } else if(o instanceof Float) {
                    Float b = (Float) o;
                    jsonObj.put(key, b);
                } else if(o instanceof Double) {
                    Double b = (Double) o;
                    jsonObj.put(key, b);
                } else if(o instanceof String) {
                    String b = (String) o;
                    jsonObj.put(key, b);
                } else {
                    String value = o.toString();
                    if(value != null) {
                        jsonObj.put(key, value);
                    } else {
                        // ?????
                        log.debug("Value is null for key = " + key);
                        jsonObj.put(key, value);
                    }
                }
            }
        }

        log.debug("jsonObj = " + jsonObj);
        return jsonObj;
    }

    // Creates a JsonNode for the given list. 
    private static ArrayNode buildJsonArray(List<Object> list, JsonNodeFactory factory)
    {
        if(list == null) {
            log.info("Argument map is null.");
            return null;
        }
        if(factory == null) {
            log.warn("Argument factory is null");
            return null;  // ????
        }

        ArrayNode jsonArr = new ArrayNode(factory);
        for(Object o : list) {
            if(o instanceof Map<?,?>) {
                @SuppressWarnings("unchecked")
                Map<String,Object> m = (Map<String,Object>) o;
                ObjectNode jo = buildJsonObject(m, factory);
                if(jo != null) {
                    jsonArr.add(jo);
                } else {
                    // ????
                    log.debug("Object element is null.");
                    jsonArr.add(jo);
                }
            } else if(o instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Object> l = (List<Object>) o;
                ArrayNode ja = buildJsonArray(l, factory);
                if(ja != null) {
                    jsonArr.add(ja);
                } else {
                    // ????
                    log.debug("Array element is null.");
                    jsonArr.add(ja);
                }
            } else {
                // Should be a "primitive type" or an object.
                // Or, everything else (including collection which is not a list or a map...)
                // We always convert them to text (except for boxed values of primitive types).
                if(o instanceof Boolean) {
                    Boolean b = (Boolean) o;
                    jsonArr.add(b);
                } else if(o instanceof Character) {
                    // Note: char is treated as String (not as int).
                    String str = Character.toString((Character) o);
                    jsonArr.add(str);
                } else if(o instanceof Byte) {
                    Byte b = (Byte) o;
                    jsonArr.add(b);
                } else if(o instanceof Short) {
                    Short b = (Short) o;
                    jsonArr.add(b);
                } else if(o instanceof Integer) {
                    Integer b = (Integer) o;
                    jsonArr.add(b);
                } else if(o instanceof Long) {
                    Long b = (Long) o;
                    jsonArr.add(b);
                } else if(o instanceof Float) {
                    Float b = (Float) o;
                    jsonArr.add(b);
                } else if(o instanceof Double) {
                    Double b = (Double) o;
                    jsonArr.add(b);
                } else if(o instanceof String) {
                    String b = (String) o;
                    jsonArr.add(b);
                } else {
                    String value = o.toString();
                    if(value != null) {
                        jsonArr.add(value);
                    } else {
                        // ?????
                        log.debug("Element value is null.");
                        jsonArr.add(value);
                    }
                }
            }
        }

        log.debug("jsonArr = " + jsonArr);
        return jsonArr;
    }
    

}
