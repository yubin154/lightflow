package org.lightj.initialization;

/**
 * initialization interface
 * @author biyu
 */
public interface Initializable {
	
	/** This should be called to initialize the initializable.
	 */
	void doInitialize()	throws InitializationException;

	/** This should be called to shutdown the initializable.
	 */
	void doShutdown();

	/** This returns the state of the Initializable.
	 */
	public InitializationStateEnum getState();

}

