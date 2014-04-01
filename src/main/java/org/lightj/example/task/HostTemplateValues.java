package org.lightj.example.task;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * template values of a host
 * 
 * @author biyu
 *
 */
public class HostTemplateValues {
	
	/**
	 * internal holder
	 */
	private final List<Map<String, String>> templateValues = new LinkedList<Map<String,String>>();
	
	public List<Map<String, String>> getTemplateValues() {
		return templateValues;
	}
	
	/** add a new entry of map */
	public HostTemplateValues addNewTemplateValue(Map<String, String> value) {
		this.templateValues.add(value);
		return this;
	}
	/** add a new entry of list of key value pair as map */
	public HostTemplateValues addNewTemplateValueAsMap(String...kvp) {
		HashMap<String, String> tval = new HashMap<String, String>();
		for (int i = 0; i < kvp.length; i+=2) {
			tval.put(kvp[i], kvp[i+1]);
		}
		this.templateValues.add(tval);
		return this;
	}
	/** add a new entry of one key value pair as map */
	public HostTemplateValues addNewTemplateValue(String key, String value) {
		return this.addNewTemplateValueAsMap(key, value);
	}
	/** add to exist map of one key value pair */
	public HostTemplateValues addToCurrentTemplate(String key, String value) {
		templateValues.get(templateValues.size()-1).put(key, value);
		return this;
	}
	public HostTemplateValues addToCurrentTemplate(Map<String, String> values) {
		templateValues.get(templateValues.size()-1).putAll(values);
		return this;
	}
	/** add everything from an external list of maps */
	public HostTemplateValues addAllTemplateValues(List<Map<String, String>> templateValues) {
		this.templateValues.addAll(templateValues);
		return this;
	}	
	
	/** is there multiple entries of template */
	public boolean hasMultiple() {
		return templateValues.size() > 1;
	}
	public boolean isEmpty() {
		return templateValues.isEmpty();
	}

}
