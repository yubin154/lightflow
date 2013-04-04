package org.lightj.dal;


/**
 * @author biyu
 *
 */
public class FinderException extends DataAccessException {


	/**
	 * 
	 */
	private static final long serialVersionUID = 3117280732773002904L;
	/**
	 * @param wrappedException
	 */
	public FinderException(Exception wrappedException) {
		super(wrappedException.getMessage());	

	}
	/**
	 * @param message
	 * @param cause
	 */
	public FinderException(String message) {
		super(message);
	}

}
