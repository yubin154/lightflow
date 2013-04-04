package org.lightj.session;

import java.io.InvalidObjectException;

public class NoSuchFlowException extends InvalidObjectException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5896578374323868697L;

	public NoSuchFlowException(String message) {
		super(message);
	}

}
