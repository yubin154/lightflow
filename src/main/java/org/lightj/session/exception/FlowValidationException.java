package org.lightj.session.exception;

/**
 * flow failed validation
 * @author binyu
 *
 */
public class FlowValidationException extends Error {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5422668589739196813L;

	/**
     * Constructs a new exception with <code>null</code> as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     */
    public FlowValidationException() {
    	super();
    }

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param   message   the detail message. The detail message is saved for 
     *          later retrieval by the {@link #getMessage()} method.
     */
    public FlowValidationException(String message) {
    	super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * <code>cause</code> is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param  message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @since  1.4
     */
    public FlowValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause and a detail
     * message of <tt>(cause==null ? null : cause.toString())</tt> (which
     * typically contains the class and detail message of <tt>cause</tt>).
     * This constructor is useful for exceptions that are little more than
     * wrappers for other throwables (for example, {@link
     * java.security.PrivilegedActionException}).
     *
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @since  1.4
     */
    public FlowValidationException(Throwable cause) {
        super(cause);
    }

}
