package org.lightj.session.exception;

/**
 * same flow exist exception
 * @author binyu
 *
 */
public class FlowExistException extends FlowSaveException 
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5264633154841764554L;
	
	private String requester;
	private String existSessionId;
	
	public String getExistSessionId() {
		return existSessionId;
	}

	public void setExistSessionId(String existSessionId) {
		this.existSessionId = existSessionId;
	}

	public String getRequester() {
		return requester;
	}

	public void setRequester(String requester) {
		this.requester = requester;
	}

	/**
	 * constructor 
	 */
	public FlowExistException() {
		super();
	}

	/**
	 * @param message
	 */
	public FlowExistException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public FlowExistException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param cause
	 */
	public FlowExistException(Throwable cause) {
		super(cause);
	}

}
