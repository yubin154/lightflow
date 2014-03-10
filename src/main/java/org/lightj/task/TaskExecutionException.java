package org.lightj.task;

/**
 * Task exeuction exception
 * @author binyu
 *
 */
public class TaskExecutionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2637918426823385362L;

	public TaskExecutionException() {
		super();
	}

	public TaskExecutionException(String message) {
		super(message);
	}

	public TaskExecutionException(String message, Throwable cause) {
		super(message, cause);
	}

	public TaskExecutionException(Throwable cause) {
		super(cause);
	}
}
