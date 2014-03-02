package org.lightj.task;

public class TaskExecutionRuntimeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TaskExecutionRuntimeException() {
		super();
	}

	public TaskExecutionRuntimeException(String message) {
		super(message);
	}

	public TaskExecutionRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public TaskExecutionRuntimeException(Throwable cause) {
		super(cause);
	}

}

