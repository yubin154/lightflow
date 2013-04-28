package org.lightj.dal;

import java.io.InvalidObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.lightj.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author biyu
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class LocatorUtil {

	// Create Log4j logger instance for logging
	private Logger logger = LoggerFactory.getLogger(LocatorUtil.class);

	private static final LocatorUtil me = new LocatorUtil();

	public static final LocatorUtil getInstance() {
		return me;
	}

	private LocatorUtil() {
		super();
	}
	
	/**
	 * find a locatable given locator class name and locatable key
	 * @param locatorClassName
	 * @param locatableKey
	 * @return
	 * @throws InvalidObjectException
	 */
	public Locatable find(String locatorClassName, String locatableKey) throws InvalidObjectException {
		Locator<? extends Locatable> locator;
		try {
			Class returnClass =	Class.forName(locatorClassName);
			Method m = returnClass.getDeclaredMethod("getInstance", Constants.NO_PARAMETER_TYPES);
			locator = (Locator<? extends Locatable>) m.invoke(Constants.NO_OBJECT, Constants.NO_PARAMETER_VALUES);
		} catch (SecurityException e) {
			logger.error(null, e);
			throw new InvalidObjectException("Security Exception Occured " + e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.error(null, e);
			throw new InvalidObjectException("Arguments are incorrect for reflection " + e.getMessage());
		} catch (ClassNotFoundException e) {
			logger.error(null, e);
			throw new InvalidObjectException("Class not found for "	+ locatorClassName + " " + e.getMessage());
		} catch (NoSuchMethodException e) {
			logger.error(null, e);
			throw new InvalidObjectException("Locator method getInstance not found " + e.getMessage());
		} catch (IllegalAccessException e) {
			logger.error(null, e);
			throw new InvalidObjectException("Illegal access to the method getInstance " + e.getMessage());
		} catch (InvocationTargetException e) {
			logger.error(null, e);
			throw new InvalidObjectException("Unable to invoke getInstance on "	+ locatorClassName + " " + e.getMessage());
		}
		Locatable locatable = locator.findByKey(locatableKey);
		return locatable;
	}

	/**
	 * generate a string presenting the locatable
	 * @param locatable
	 * @return
	 */
	public static String getLocatableString(Locatable locatable) {
		if (locatable != null && locatable.getLocator() != null) {
			return locatable.getLocator().getName() + ":" + locatable.getKey();
		}
		return null;
	}
	
	/**
	 * generate locatable string
	 * @param locatorKlass
	 * @param locatableKey
	 * @return
	 */
	public static String getLocatableString(Class locatorKlass, String locatableKey) {
		return locatorKlass.getName() + ":" + locatableKey;
	}
	
	/**
	 * reinstate a locatable from the locatable string
	 * @param locatableStr
	 * @return
	 * @throws InvalidObjectException
	 * @throws DataAccessException
	 */
	public static Locatable findByLocatableString(String locatableStr) throws InvalidObjectException, DataAccessException {
		String[] tokens = locatableStr.split(":");
		if (tokens != null && tokens.length == 2) {
			return getInstance().find(tokens[0], tokens[1]);
		}
		return null;
	}
	
	/**
	 * grab the locatable key from a locatable string 
	 * @param locatableStr
	 * @return
	 */
	public static String getLocatableKey(String locatableStr) {
		if (locatableStr != null) {
			String[] tokens = locatableStr.split(":");
			if (tokens != null && tokens.length == 2) {
				return tokens[1];
			}
		}
		return null;
	}
	
	/**
	 * grab the locator String from locatable string
	 * @param locatableStr
	 * @return
	 */
	public static String getLocatorStr(String locatableStr) {
		String[] tokens = locatableStr.split(":");
		if (tokens != null && tokens.length == 2) {
			return tokens[0];
		}
		return null;
	}
}
