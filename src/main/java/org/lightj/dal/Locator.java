/*
 * Created on Jan 30, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.dal;

import java.io.InvalidObjectException;

/**
 * @author biyu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface Locator<T extends Locatable> {
	
	/**
	 * find a locatable by key
	 * @param key
	 * @return
	 * @throws InvalidObjectException
	 */
	public T findByKey(String key) throws InvalidObjectException;
	
}
