package org.lightj.initialization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * initializing modules
 * @author biyu
 */
public class InitializationProcessor {

	/** logger */
	static Logger logger = LoggerFactory.getLogger(InitializationProcessor.class);

	/**
	 * top modules to be initialized
	 */
	private final BaseModule[] m_topLevelModules;

	/**
	 * order of initialization
	 */
	private final ArrayList<BaseModule> m_initOrder;

	
	private int m_highestInitIndex = 0;

	/**
	 * initialization state
	 */
	private InitializationStateEnum m_state = InitializationStateEnum.PRISTINE;

	private String m_tag;

	/** This creates an initializationProcessor.
	 * 
	 * Calls to a module's setInitializationManager() will have not effect
	 * after this constructor is called.
	 * 
	 * This will calculate the initialization order.  If any cycles are 
	 * detected, an exception will be thrown.
	 * 
	 * Calls to the InitializationManager's setDependentModules() will have
	 * no effect after this call.
	 * @throws CycleInModulesException 
	 * @throws EmptyModuleSetException 
	 */
	public InitializationProcessor(final BaseModule[] topLevelModules) 
		throws InitializationException 
	{
		this(topLevelModules, "");
	}

	/** This creates an initializationProcessor.
	 * 
	 * Calls to a module's setInitializationManager() will have not effect
	 * after this constructor is called.
	 * 
	 * This will calculate the initialization order.  If any cycles are 
	 * detected, an exception will be thrown.
	 * 
	 * Calls to the InitializationManager's setDependentModules() will have
	 * no effect after this call.
	 * @throws CycleInModulesException 
	 * @throws EmptyModuleSetException 
	 */
	public InitializationProcessor(final BaseModule[] topLevelModules,
			final String tag) throws InitializationException 
	{
		if (topLevelModules == null || topLevelModules.length == 0) {
			throw new EmptyModuleSetException();
		}

		m_topLevelModules = topLevelModules;
		final Map<BaseModule, BaseModule[]> moduleMap = getExpandedMapOfInitManagers(m_topLevelModules);
		m_initOrder = calcInitOrder(moduleMap);
		m_tag = tag;
	}

	/** This returns the state of the BaseInitializationProcessor.
	 */
	public synchronized InitializationStateEnum getState() {
		return m_state;
	}

	/** This will initialize all modules and their dependent modules in the
	 * correct order.
	 */
	public synchronized void initialize() throws InitializationException {

		String initTag = (m_tag.equals("")) ? getClass().getName() : m_tag;

		try {
			printModuleInitOrder();

			logger.info("initializing '" + initTag + "' {");
			final Iterator<BaseModule> iter = m_initOrder.listIterator();
			int initIndex = 0;
			while (iter.hasNext()) {
				BaseModule module = (BaseModule) iter.next();

				initIndex++;
				if (initIndex > m_highestInitIndex) {
					m_highestInitIndex = initIndex;
				}
				module.doInitialize();
			}
			m_state = InitializationStateEnum.INITAILIZED;

		} finally {
			if (m_state != InitializationStateEnum.INITAILIZED) {
				m_state = InitializationStateEnum.INITAILIZED_FAILED;
				logger.info("initializing FAILED '" + initTag + "' }");
			}
		}
	}

	/**
	 * print initialization order
	 *
	 */
	private void printModuleInitOrder() {
		Iterator<BaseModule> iter = m_initOrder.listIterator();
		int i = 0;
		logger.info("module init order:");
		while (iter.hasNext()) {
			BaseModule module = iter.next();
			logger.info(i + ":" + module.getClass().getName());
			i++;
		}
	}

	/** This will call shutdown in reverse order of initialization.  Any
	 * Exceptions will be caught and shutdown() will continue to be called
	 * on all elements that have been initialized.
	 */
	public synchronized void shutdown() {
		try {
			final ListIterator<BaseModule> iter = m_initOrder.listIterator(m_highestInitIndex);
			while (iter.hasPrevious()) {
				BaseModule module = (BaseModule) iter.previous();
				module.doShutdown();
				m_highestInitIndex--;
			}
			m_state = InitializationStateEnum.SHUTDOWN;
		} finally {
			if (m_state != InitializationStateEnum.SHUTDOWN) {
				m_state = InitializationStateEnum.SHUTDOWN_FAILED;
			}
		}
	}

	/**
	 * calculate initialization order
	 * @param moduleMap
	 * @return
	 * @throws CycleInModulesException
	 */
	private static ArrayList<BaseModule> calcInitOrder(final Map<BaseModule, BaseModule[]> moduleMap) throws CycleInModulesException {
		final Map<BaseModule, BaseModule[]> uncalculatedManagers = new HashMap<BaseModule, BaseModule[]>(moduleMap);
		final ArrayList<BaseModule> initOrderList = new ArrayList<BaseModule>(moduleMap.size());
		final Map<BaseModule, BaseModule[]> calculatedHash = new HashMap<BaseModule, BaseModule[]>(moduleMap.size());

		while (uncalculatedManagers.size() > 0) {
			final Iterator<BaseModule> keyIter = uncalculatedManagers.keySet().iterator();
			final Vector<BaseModule> newlyCalculated = new Vector<BaseModule>();
			while (keyIter.hasNext()) {
				final BaseModule module = (BaseModule) keyIter.next();
				final BaseModule[] dependents = (BaseModule[]) uncalculatedManagers
						.get(module);

				final boolean allDependentsInHash = areAllInMap(dependents,	calculatedHash);
				if (allDependentsInHash) {
					newlyCalculated.add(module);
				}
			}
			if (newlyCalculated.size() == 0) {
				StringBuffer buf = new StringBuffer();
				buf.append("cycle in modules:  ");
				final Iterator<BaseModule> iter = uncalculatedManagers.keySet().iterator();
				while (iter.hasNext()) {
					buf.append(iter.next());
					if (iter.hasNext()) {
						buf.append(", ");
					}
				}
				throw new CycleInModulesException(buf.toString());
			}
			final Iterator<BaseModule> newCalcIter = newlyCalculated.iterator();
			while (newCalcIter.hasNext()) {
				final BaseModule module = (BaseModule) newCalcIter.next();
				initOrderList.add(module);
				calculatedHash.put(module, null);
				uncalculatedManagers.remove(module);
			}
		}

		return initOrderList;
	}

	/** returns true if all the dependent modules are in the map
	 */
	private static boolean areAllInMap(final BaseModule[] dependents, final Map<BaseModule, BaseModule[]> map) {
		for (int i = 0; i < dependents.length; i++) {
			if (!map.containsKey(dependents[i])) {
				return false;
			}
		}
		return true;
	}

	/** 
	 * This takes an array of modules and returns a map of all modules
	 * mapped to their dependent init managers.
	 */
	private Map<BaseModule, BaseModule[]> getExpandedMapOfInitManagers(final BaseModule[] topLevelModules) 
	{
		if (topLevelModules == null || topLevelModules.length == 0) {
			throw new RuntimeException("assertion failed");
		}

		final Map<BaseModule, BaseModule[]> moduleMap = new HashMap<BaseModule, BaseModule[]>(50);

		for (int i = 0; i < topLevelModules.length; i++) {
			addModuleAndDependentsToMap(topLevelModules[i], moduleMap);
		}

		return moduleMap;
	}

	/** 
	 * This will recursively add a module and its dependents to the module
	 * map.
	 */
	private void addModuleAndDependentsToMap(final BaseModule module, final Map<BaseModule, BaseModule[]> moduleMap) {
		if (module == null) {
			throw new NullPointerException();
		}
		if (moduleMap.containsKey(module)) {
			// we already have this module in the map, so skip
			return;
		}

		final BaseModule[] dependentModules = module.getDependentModules();
		if (dependentModules == null) {
			// a null depent array will turn into a zero length
			// array to make processing more consistent.
			moduleMap.put(module, new BaseModule[0]);
		} else {
			moduleMap.put(module, dependentModules);
			for (int i = 0; i < dependentModules.length; i++) {
				final BaseModule depenentModule = dependentModules[i];
				if (depenentModule == null) {
					throw new NullPointerException("parent:  '"
							+ module.getClass().getName() + ", "
							+ "check to make sure that the module is not "
							+ "refering to itself");
				}
				addModuleAndDependentsToMap(depenentModule, moduleMap);
			}
		}
	}
}
