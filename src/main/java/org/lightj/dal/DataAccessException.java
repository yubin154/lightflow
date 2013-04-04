/*
 * Created on Jan 31, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.dal;

/**
 * @author biyu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class DataAccessException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2287139886650002012L;
	private Exception wrapped;
	
	public DataAccessException(String msg){
		super(msg);
	}

	public DataAccessException(Exception wrappedException){
		super(wrappedException.getMessage());
		this.wrapped = wrappedException;
	}
	
	public Exception getCausedByException(){
		return this.wrapped;
	}
	
	public String getMessage() {
		return (wrapped != null) ? wrapped.getMessage() : super.getMessage();
	}
}

