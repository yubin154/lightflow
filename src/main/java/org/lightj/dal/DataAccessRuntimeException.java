package org.lightj.dal;

public class DataAccessRuntimeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2609166479553065481L;
	private Exception wrapped;
	
	public DataAccessRuntimeException(String msg){
		super(msg);
	}

	public DataAccessRuntimeException(Exception wrappedException){
		super(wrappedException.getMessage());
		this.wrapped = wrappedException;
	}
	
	public Exception getCausedByException(){
		return this.wrapped;
	}
	
}
