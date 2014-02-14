package org.lightj.task;

public class RuntimeTaskExecutionException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RuntimeTaskExecutionException() {
		super();
	}

	public RuntimeTaskExecutionException(String message) {
		super(message);
	}

	public RuntimeTaskExecutionException(String message, Throwable cause) {
		super(message, cause);
	}

	public RuntimeTaskExecutionException(Throwable cause) {
		super(cause);
	}

}

