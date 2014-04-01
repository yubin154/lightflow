package org.lightj.task;



public interface IGlobalContext {

	/** pivot key, use to look up for pivot value from local context */
	public String getPivotKey();
	
	/** lookup value from external context*/
	public <C> C getValueByName(String pivotValue, String name);
	
	/** set value for key in external context */
	public void setValueForName(String pivotValue, String name, Object value);
	
	/** this context has value for this name */
	public boolean hasName(String pivotValue, String name);

}
