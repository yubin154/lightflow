package org.lightj.util;

import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationException;
import org.lightj.initialization.InitializationProcessor;


public class TestCaseInitializer {
	
	private BaseModule[] modulesToInitialize;
	private InitializationProcessor initializer;
	
	public TestCaseInitializer(BaseModule[] modulesToInitialize) {
		this.modulesToInitialize = modulesToInitialize;
	}
	
	/**
	 * Prepare to run the test
	 * @throws InitializationException 
	 */
	public InitializationProcessor initialize() throws Exception {

		if (modulesToInitialize != null) {
			initializer = new InitializationProcessor(modulesToInitialize);
			initializer.initialize();
		}
		return initializer;
	}

	/**
	 * tear the modules down
	 * @param initializer
	 * @throws Exception
	 */
	public void shutdown() throws Exception {
		if (initializer != null) {
			initializer.shutdown();
		}
	}
	
}
