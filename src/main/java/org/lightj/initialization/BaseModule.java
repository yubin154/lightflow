package org.lightj.initialization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * a module with initialization logic and dependency list
 * @author biyu
 */
public abstract class BaseModule implements Initializable {
	
	/** logger */
	static Logger logger = LoggerFactory.getLogger(BaseModule.class);

	/**
	 * dependencies to other modules
	 */
	private List<BaseModule> m_dependentModules = new ArrayList<BaseModule>();

	/**
	 * initialization logic
	 */
	private List<Initializable> m_initializables = new ArrayList<Initializable>();

	/**
	 * initialization state
	 */
	private InitializationStateEnum m_state = InitializationStateEnum.PRISTINE;

	/**
	 * constructor, with on dependency
	 *
	 */
	public BaseModule() {
		this(new BaseModule[] {});
	}

	/**
	 * constructor, with dependencies passed in
	 * @param dependentModules
	 */
	public BaseModule(BaseModule[] dependentModules) {
		if (dependentModules != null) {
			m_dependentModules.addAll(Arrays.asList(dependentModules));
		}
	}

	/**
	 * add a dependency module
	 * @param module
	 */
	public final void addDependentModule(BaseModule module) {
		m_dependentModules.add(module);
	}

	/**
	 * get all dependency modules
	 * @return
	 */
	protected BaseModule[] getDependentModules() {
		return (BaseModule[]) m_dependentModules.toArray(new BaseModule[0]);
	}

	/**
	 * This will add the initializable to the list of objects that will
	 * get call during initialize and shutdown. It will not detect duplicates
	 * 
	 * Note: this method works only if constructor with dependent modules was used
	 */
	public final void addInitializable(Initializable initializable) {
		m_initializables.add(initializable);
	}

	/**
	 * get list of initialization logic
	 * @return
	 */
	protected Initializable[] getInitializables() {
		return (Initializable[]) m_initializables.toArray(new Initializable[0]);
	}

	/////////////////////////// initializable interface /////////////////////////
	
	/**
	 * initialization
	 */
	public final synchronized void doInitialize() throws InitializationException {
		if ( m_state == InitializationStateEnum.INITAILIZED) {
			return ;  // if we are already initied, skip
		}
		try {
			for (Initializable init : m_initializables) {
				init.doInitialize();
			}
			m_state = InitializationStateEnum.INITAILIZED;
			logger.warn("module initializing DONE '" + getClass().getName() + "' }");
		} 
		finally {
			if ( m_state != InitializationStateEnum.INITAILIZED ) {
				m_state = InitializationStateEnum.INITAILIZED_FAILED;
				logger.error("module initializing FAILED '" + getClass().getName() + "' }");
			}
		}
	}
	
	/**
	 * shutdown
	 */
	public final synchronized void doShutdown() {
		if ( m_state == InitializationStateEnum.SHUTDOWN ||
				m_state == InitializationStateEnum.PRISTINE) {
			return ;  // if we are already shutdown or pristine, skip
		}
		try {
			for (Initializable init : m_initializables) {
				init.doShutdown();
			}
			m_state = InitializationStateEnum.SHUTDOWN;
			logger.warn("module shutdown DONE'" + getClass().getName() + "' }");
		} finally {
			if ( m_state != InitializationStateEnum.SHUTDOWN ) {
				m_state = InitializationStateEnum.SHUTDOWN_FAILED;
				logger.error("module shutdown FAILED'" + getClass().getName() + "' }");
			}
		}
	}

	/** 
	 * This returns the state of the Initializable.
	 */
	public InitializationStateEnum getState() {
		return m_state;
	}

	/**
	 * change to the module is allowed only before initialiation or after shutdown
	 */
	public void validateForChange()  {
		if (m_state != InitializationStateEnum.PRISTINE &&
				m_state != InitializationStateEnum.SHUTDOWN) {
			throw new UnsupportedOperationException("No change is allowed after module initialization");
		}
	}
	
}
