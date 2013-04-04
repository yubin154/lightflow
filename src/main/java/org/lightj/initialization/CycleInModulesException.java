package org.lightj.initialization;


/**
 * @author biyu
 */
public class CycleInModulesException extends InvalidModuleSetException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1175536201641624531L;

	public CycleInModulesException(final String message) {
		super(message);
	}
}

