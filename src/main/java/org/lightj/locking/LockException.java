package org.lightj.locking;

public class LockException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7273335488833339954L;

	public LockException() {
		super();
	}

	public LockException(String message) {
		super(message);
	}

	public LockException(String message, Throwable cause) {
		super(message, cause);
	}

	public LockException(Throwable cause) {
		super(cause);
	}
}
