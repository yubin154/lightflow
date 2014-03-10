package org.lightj.session.exception;

/**
 * flow update exception
 * 
 * @author binyu
 *
 */
public class StateChangeException extends FlowExecutionException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7297823194924282789L;
	private boolean overridable;

	public StateChangeException() {
    	super();
    }

    public StateChangeException(String message) {
    	super(message);
    }

    public StateChangeException(String message, Throwable cause) {
        super(message, cause);
    }

    public StateChangeException(Throwable cause) {
        super(cause);
    }
    
    public boolean isOverridable() {
    	return overridable;
    }
    public void setOverridable(boolean overridable) {
    	this.overridable = overridable;
    }

}
