package org.lightj;

import org.lightj.util.StringUtil;


/**
 * runtime context of a running application
 * 
 * @author binyu
 *
 */
public class RuntimeContext {

	public static final String CONFIG_ROOT = "org.lightj.context.externalConfigRootUrl";
	public static final String PRODUCT = "org.lightj.product";
	public static final String ENV = "org.lightj.env";
	public static final String BUILD = "org.lightj.build";
	public static final String DC = "org.lightj.dc";
	
	public static final String LOG_CONFIG = "java.util.logging.config.file";
	public static final String LOG4J_CONFIG = "log4j.configuration";
	public static final String LOG4J_PROPS = "log4j.properties";
	
	public static void setProduct(String product) {
		System.setProperty(PRODUCT, product);
	}
	public static String getProduct() {
		return System.getProperty(PRODUCT, "UnknownProduct");
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
	public static void setDc(String dc) {
		System.setProperty(DC, dc);
	}
	public static String getDc() {
		return System.getProperty(DC, "UnknownDC");
	}
	public static void setClusterUuid(String product, String env, String dc, String buildId) {
		setProduct(product);
		setEnv(env);
		setDc(dc);
		setBuild(buildId);
	}

	/**
	 * get cluster name of product-env-build
	 * @see {@link RuntimeContext#setProduct(String...)} {@link RuntimeContext#setEnv(String)} {@link RuntimeContext#setBuild(String)}
	 * @return
	 */
	public static String getClusterName() {
		return StringUtil.join(new String[] {getProduct(), getEnv(), getDc(), getBuild()}, "-");
	}
	
}
