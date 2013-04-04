package org.lightj.initialization;

public class InitializationException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8156571210100659861L;

	public InitializationException() {
    	super();
    }

    public InitializationException(String message) {
    	super(message);
    }

    public InitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InitializationException(Throwable cause) {
        super(cause);
    }
}
