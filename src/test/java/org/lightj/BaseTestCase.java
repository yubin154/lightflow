package org.lightj;

import java.io.File;

import junit.framework.TestCase;

import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;
import org.lightj.initialization.InitializationProcessor;
import org.lightj.initialization.ShutdownException;
import org.lightj.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseTestCase extends TestCase {
	
	static Logger logger = LoggerFactory.getLogger(BaseTestCase.class);
	
	private InitializationProcessor initializer = null;

	public BaseTestCase() {}
	
	public String getConfigRoot() {
		String home =  System.getProperty("user.dir");
		String configDir = StringUtil.join(new String[] {"", "src", "main", "resources", "config", ""}, File.separator);
		return home + configDir;
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
	protected void setUp() throws Exception {

		System.setProperty(RuntimeContext.LOG_CONFIG, getConfigRoot() + "logging.properties");
		RuntimeContext.setClusterUuid("lightj", "dev", "corp", Long.toString(System.currentTimeMillis()));
		if (getDependentModules() != null) {
			logger.warn("Test case" + this.getName());
			initializer = new InitializationProcessor(getDependentModules());
			initializer.initialize();
		}

		afterInitialize(System.getProperty("user.dir"));
	}
	
	protected void tearDown() throws Exception {
		if (initializer != null) {
			initializer.shutdown();
		}
	}
	
	protected void afterInitialize(String home) throws InitializationException {
	}

	protected void afterShutdown() throws ShutdownException {
	}

}
