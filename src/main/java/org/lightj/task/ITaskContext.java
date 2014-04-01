package org.lightj.task;


public interface ITaskContext {
	
	/**
	 * get value by name
	 * @param name
	 * @return
	 */
	public <C> C getValueByName(String name);
	
	/**
	 * set value for name
	 * @param name
	 * @return
	 */
	public void setValueForName(String name, Object value);

}
