package org.lightj.dal;

/**
 * @author biyu
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface Locatable {

	/** key */
	public String getKey();
	
	/**
	 * load by key
	 * @param key
	 */
	@SuppressWarnings("rawtypes")
	public Class<? extends Locator> getLocator();

}
