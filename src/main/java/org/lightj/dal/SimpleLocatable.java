package org.lightj.dal;

import java.io.InvalidObjectException;

/**
 * dummy locatable, an object with a key
 * @author biyu
 *
 */
public class SimpleLocatable implements Locatable {
	
	private String key = null;
	public SimpleLocatable() {
		this(Long.toString(System.currentTimeMillis()));
	}
	
	public SimpleLocatable(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	@SuppressWarnings("rawtypes")
	public Class<? extends Locator> getLocator() {
		return SimpleLocator.class;
	}

	/**
	 * dummy locator, just create a new locatable with the key
	 * @author biyu
	 *
	 */
	public static class SimpleLocator implements Locator<SimpleLocatable> {
		static SimpleLocator me = new SimpleLocator();
		private SimpleLocator() {}
		public static SimpleLocator getInstance() {
			return me;
		}		
		public SimpleLocatable findByKey(String key) throws InvalidObjectException {
			return new SimpleLocatable(key);
		}
		
	}

}
