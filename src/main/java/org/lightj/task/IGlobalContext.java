package org.lightj.task;


/**
 * a global context that holds lookup values for across flow, task, and request
 * 
 * @author biyu
 *
 */
public interface IGlobalContext {

	/** 
	 * pivot key, use to look up for pivot value from local context
	 * for example, the master key for lookup values can be hostname
	 * client of the GlobalContext will get the name "hostname" and 
	 * use it to find actual value of hostname from its own context,
	 * and use the value for subsequent lookup 
	 */
	public String getPivotKey();
	
	/** 
	 * lookup value from external context
	 * e.g. pivotValue = "abc.test.com", name = "oauthkey"
	 */
	public <C> C getValueByName(String pivotValue, String name);
	
	/** 
	 * update value in global context so other clients can benefit 
	 * e.g. when one workflow detect the oauthkey expired, it will generate a new
	 * one and update the new value into the global context 
	 */
	public void setValueForName(String pivotValue, String name, Object value);
	
	/** 
	 * this context has value for this name, check existance of the entry/value in the context 
	 */
	public boolean hasName(String pivotValue, String name);

}
