package org.lightj.task;


public interface IGlobalContext {

	/** what value to use to lookup externally as a key */
	public String getPivotValue();
	
	/** lookup value from external context*/
	public <C> C getValueByName(String pivotValue, String name);
	
	/** set value for key in external context */
	public <C> void setValueForName(String pivotValue, String name, C value);
	
	/** this context has value for this name */
	public boolean hasName(String pivotValue, String name);

}
