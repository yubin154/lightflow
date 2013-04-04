package org.lightj;

import java.io.File;
import java.net.MalformedURLException;

import junit.framework.TestCase;

import org.apache.log4j.xml.DOMConfigurator;
import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;
import org.lightj.initialization.InitializationProcessor;
import org.lightj.initialization.ShutdownException;
import org.lightj.util.Log4jProxy;
import org.lightj.util.StringUtil;

public abstract class BaseTestCase extends TestCase {
	
	static Log4jProxy logger = Log4jProxy.getInstance(BaseTestCase.class);
	
	private InitializationProcessor initializer = null;

	/**
	 * Helper method to initialize the test environment
	 */
	protected String getHome() {
		String userDir = System.getProperty("user.dir");
		return userDir;
	}
	
	/**
	 * helper method to initialize the test environment
	 * @return
	 */
	protected String[] getProducts() {
		return new String[] {"lightj"};
	}
	
	/**
	 * config root path relative to home
	 * @return
	 */
	protected String getConfigRoot() {
		return StringUtil.join(new String[] {"", "src", "main", "resources", "config", ""}, File.separator);
	}
	
	public BaseTestCase() {
	}
	
	/**
	 * dependent modules
	 * @return
	 */
	protected abstract BaseModule[] getDependentModules();
	
	/**
	 * Prepare to run the test suite.
	 * @throws InitializationException 
	 */
	protected void setUp() throws InitializationException {

		String home = getHome();

		String configRoot = home + getConfigRoot();
		try {
			RuntimeContext.setConfigRoot(new File(configRoot).toURI().toURL().toString());
		} catch (MalformedURLException e) {
			throw new InitializationException(e);
		}
		RuntimeContext.setProducts(getProducts());
		RuntimeContext.setEnv("dev");
		RuntimeContext.setBuild(Long.toString(System.currentTimeMillis()));

		
		// logging
		System.setProperty("java.util.logging.config.file", configRoot + "logging.properties");
		DOMConfigurator.configure(configRoot + "log4j.xml");

		if (getDependentModules() != null) {
			logger.warn("Test case" + this.getName());
			initializer = new InitializationProcessor(getDependentModules());
			initializer.initialize();
		}

		afterInitialize(home);
	}
	
	protected void tearDown() throws ShutdownException {
		if (initializer != null) {
			initializer.shutdown();
		}
	}
	
	protected void afterInitialize(String home) throws InitializationException {
	}

	protected void afterShutdown() throws ShutdownException {
	}

}
