package org.lightj.session.exception;

/**
 * exception while persist flow
 * 
 * @author biyu
 * 
 */
public class FlowSaveException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4361407765859258460L;

	/**
	 * 
	 */
	public FlowSaveException() {
		super();
	}

	/**
	 * @param message
	 */
	public FlowSaveException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public FlowSaveException(
		String message,
		Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param cause
	 */
	public FlowSaveException(Throwable cause) {
		super(cause);
	}

}
