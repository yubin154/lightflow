package org.lightj.util;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * a v3logger wrapper that has log4j interface, 
 * this is used to easily migrate log4j style logging to v3logger
 * 
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
public class Log4jProxy {
	
	/**
	 * v3 logger
	 */
	private Category v3logger;
	
	/**
	 * constructor
	 * @param v3logger
	 */
	private Log4jProxy(Category v3logger) {
		this.v3logger = v3logger;
	}

	/**
	 * debug 
	 * @param message
	 */
	public void debug(java.lang.Object message) {
		v3logger.debug(message != null ? message.toString() : "null");
	}

	/**
	 * debug with exception
	 * @param message
	 * @param t
	 */
	public void debug(java.lang.Object message, java.lang.Throwable t) {
		v3logger.log(Level.DEBUG, message != null ? message.toString() : "null", t);
	}

	/**
	 * error
	 * @param message
	 */
	public void error(java.lang.Object message) {
		v3logger.log(Level.ERROR, message != null ? message.toString() : "null");
	}

	/**
	 * error with exception
	 * @param message
	 * @param t
	 */
	public void error(java.lang.Object message, java.lang.Throwable t) {
		v3logger.log(Level.ERROR, message != null ? message.toString() : "null", t);
	}

	/**
	 * fatal 
	 * @param message
	 */
	public void fatal(java.lang.Object message) {
		v3logger.log(Level.FATAL, message != null ? message.toString() : "null");
	}

	/**
	 * fatal with exception
	 * @param message
	 * @param t
	 */
	public void fatal(java.lang.Object message, java.lang.Throwable t) {
		v3logger.log(Level.FATAL, message != null ? message.toString() : "null", t);
	}

	/**
	 * info
	 * @param message
	 */
	public void info(java.lang.Object message) {
		v3logger.log(Level.INFO, message != null ? message.toString() : "null");
	}

	/**
	 * info with exception
	 * @param message
	 * @param t
	 */
	public void info(java.lang.Object message, java.lang.Throwable t) {
		v3logger.log(Level.INFO, message != null ? message.toString() : "null", t);
	}

	/**
	 * warn
	 * @param message
	 */
	public void warn(java.lang.Object message) {
		v3logger.log(Level.WARN, message != null ? message.toString() : "null");
	}

	/**
	 * warn with exception
	 * @param message
	 * @param t
	 */
	public void warn(java.lang.Object message, java.lang.Throwable t) {
		v3logger.log(Level.WARN, message != null ? message.toString() : "null", t);
	}
	
	/**
	 * debug enable
	 * @return
	 */
	public boolean isDebugEnabled() {
		return v3logger.isDebugEnabled();
	}
	
	/**
	 * info enable
	 * @return
	 */
	public boolean isInfoEnabled() {
		return v3logger.isInfoEnabled();
	}
	
	/**
	 * create an instance
	 * @param clazz
	 * @return
	 */
	public static Log4jProxy getInstance(java.lang.Class clazz) {
		return new Log4jProxy(Logger.getLogger(clazz));
	}

	/**
	 * create an instance
	 * @param name
	 * @return
	 */
	public static Log4jProxy getInstance(java.lang.String name) {
		return new Log4jProxy(Logger.getLogger(name));
	}

	/**
	 * create an instance
	 * @param clazz
	 * @return
	 */
	public static Log4jProxy getLogger(java.lang.Class clazz) {
		return new Log4jProxy(Logger.getLogger(clazz));
	}

	/**
	 * create an instance
	 * @param name
	 * @return
	 */
	public static Log4jProxy getLogger(java.lang.String name) {
		return new Log4jProxy(Logger.getLogger(name));
	}

}
