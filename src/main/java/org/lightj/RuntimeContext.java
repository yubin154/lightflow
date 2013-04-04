package org.lightj;

import java.net.MalformedURLException;
import java.net.URL;

import org.lightj.util.StringUtil;


/**
 * runtime context of a running application
 * 
 * @author binyu
 *
 */
public class RuntimeContext {

	public static final String CONFIG_ROOT = "org.lightj.context.externalConfigRootUrl";
	public static final String PRODUCTS = "org.lightj.products";
	public static final String ENV = "org.lightj.env";
	public static final String BUILD = "org.lightj.build";
	
	public static final String LOG_CONFIG = "java.util.logging.config.file";
	public static final String LOG4J_CONFIG = "log4j.configuration";
	public static final String LOG4J_PROPS = "log4j.properties";
	
	public static void setProducts(String...products) {
		System.setProperty(PRODUCTS, StringUtil.join(products, ","));
	}
	public static boolean hasAnyProduct() {
		return !StringUtil.isNullOrEmpty(System.getProperty(PRODUCTS));
	}
	public static String[] getProducts() {
		String productStr = System.getProperty(PRODUCTS);
		return StringUtil.isNullOrEmpty(productStr) ? new String[] {"lightj"} : productStr.split(",");
	}
	public static String getMainProduct() {
		return hasAnyProduct() ? getProducts()[0] : "lightj";
	}
	public static String getEnv() {
		return System.getProperty(ENV, "UnknownEnv");
	}
	public static void setEnv(String env) {
		System.setProperty(ENV, env);
	}
	public static void setBuild(String buildId) {
		System.setProperty(BUILD, buildId);
	}
	public static String getBuild() {
		return System.getProperty(BUILD, "UnknownBuild");
	}

	/**
	 * Returns the ConfigRoot.
	 * @return URL for config root
	 */
	public static URL getConfigRoot() {
		try {
			return System.getProperty(CONFIG_ROOT)!=null ? new URL(System.getProperty(CONFIG_ROOT)) : null;
		} catch (MalformedURLException e) {
			return null;
		}
	}

	/**
	 * Sets the config root.
	 * @param configRoot The config root to set
	 */
	public static void setConfigRoot(String cr) {
		System.setProperty(CONFIG_ROOT, cr);
	}

	/**
	 * get cluster name of product-env-build
	 * @see {@link RuntimeContext#setProducts(String...)} {@link RuntimeContext#setEnv(String)} {@link RuntimeContext#setBuild(String)}
	 * @return
	 */
	public static String getClusterName() {
		return StringUtil.join(new String[] {getMainProduct(), getEnv(), getBuild()}, "-");
	}
	
	
	
}
