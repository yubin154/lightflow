package org.lightj.task;

/**
 * task execution runtime exception
 * @author binyu
 *
 */
public class TaskExecutionRuntimeException extends RuntimeException {


	/**
	 * 
	 */
	private static final long serialVersionUID = -7690906370768818332L;

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

