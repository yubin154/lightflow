package org.lightj.initialization;

import java.util.HashMap;
import java.util.Map;

import org.lightj.util.Log4jProxy;

/**
 * initialize one particular resource or service
 * @author biyu
 */
public abstract class BaseInitializable implements Initializable {
	
	/** logger */
	static Log4jProxy logger = Log4jProxy.getInstance(BaseInitializable.class);
	
	/** time taken for init */
	private static Map<String, Long> initTime = new HashMap<String, Long>();

	private static int s_InitNumber=0;
	private static int s_ShutdownNumber=0;

	private InitializationStateEnum m_state = InitializationStateEnum.PRISTINE;
	
	/** This returns the state of the Initializable.
	 */
	public InitializationStateEnum getState() {
		return m_state;
	}

	/** This is called during initialization.  It keeps track of the state.
	 * It internally calls doInitialize().
	 */
	public final synchronized void doInitialize()
		throws InitializationException
	{
		if ( m_state == InitializationStateEnum.INITAILIZED) {
			return ;  // if we are already initied, skip
		}
		try {
			int num = s_InitNumber++;
			final long startTime = System.currentTimeMillis();
			initialize();
			final long endTime = System.currentTimeMillis();
			long m_initTime = endTime - startTime;
			logger.info(num + ": init DONE");

			m_state = InitializationStateEnum.INITAILIZED;
			initTime.put(getClass().getName(), m_initTime);
		} finally {
			if ( m_state != InitializationStateEnum.INITAILIZED ) {
				m_state = InitializationStateEnum.INITAILIZED_FAILED;
				logger.error("initializing FAILED '" + getClass().getName() + "' }");
			}
		}
	}

	/** The user should define this.  This will get called during 
	 * initialization.
	 */
	protected abstract void initialize();

	/** This is called during shutdown.  It keeps track of the state.
	 * It internally calls doShutdown().
	 */
	public final synchronized void doShutdown() {
		if ( m_state == InitializationStateEnum.SHUTDOWN ||
				m_state == InitializationStateEnum.PRISTINE) {
			return ;  // if we are already shutdown or pristine, skip
		}
		try {
			int num = s_ShutdownNumber++;
			logger.info(num + ": down...");
			shutdown();
			logger.info(num + ": down DONE");
			m_state = InitializationStateEnum.SHUTDOWN;
		} finally {
			if ( m_state != InitializationStateEnum.SHUTDOWN ) {
				m_state = InitializationStateEnum.SHUTDOWN_FAILED;
				logger.error("shutdown FAILED'" + getClass().getName() + "' }");
			}
		}
	}

	/** The user should define this.  This will get called during 
	 * shutdown.
	 */
	protected abstract void shutdown();
	
}

