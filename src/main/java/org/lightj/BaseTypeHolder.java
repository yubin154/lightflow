package org.lightj;

import java.io.Serializable;

import org.lightj.util.StringUtil;

/**
 * 
 */
public abstract class BaseTypeHolder implements Cloneable, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4972863482824268243L;
	
	//all transient attributes
	private transient String		m_name; 	
	
	/**
	 * The general contract for clones mean original.clone() != original.  
	 * However, based on the identity nature of this type, we deviate from such
	 * a general contract and return this.
	 */
	public Object clone() throws CloneNotSupportedException {
		return this ;
	}
	
	/**
	 * Base constructor to set the id and name of the constant.
	 * It should be called by other constructors from the derived classes.
	 * It internally adds this object into the EnumManager, which in turn
	 * sets the relationship between this constant with others, such as
	 * previous and next constants of the same type.
	 */
	protected BaseTypeHolder(String name) {		
		m_name = name;
	}

	/**
	 * Return the name defined in this constant.
	 * 
     * @return String - name of the constant. 
	 */
	public final String getName() {
		return m_name;
	}

	/**
	 * Returns true if the object is the same type as this one 
     * and the ids are equal. 
	 * 
     * @param other an Object reference for comparison
     *
     * @return boolean - true if they are equal, false otherwise. 
	 */ 
	public boolean equals(Object other) {
	    if (other == null) {
	    	return false;
	    }
	    if (this == other) {
	    	return true;
	    }
		if (this.getClass() == other.getClass()
			&& StringUtil.equalIgnoreCase(m_name, ((BaseTypeHolder)other).m_name))
		{
			return true;
		}
		
		return false;
	}
	
    /**
     * Generate a hash value for this constant. Conforms with equals.
     *
     * @return int - hash code of the constant. 
	 */
    public int hashCode() {
		return m_name.hashCode();
    }


    /**
     * Return a String representation of this constant.
     *
     * @return String - verbose for this constant. 
	 */			
	public String toString() {
		return m_name;
	}
	
}
