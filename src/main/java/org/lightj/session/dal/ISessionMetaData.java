package org.lightj.session.dal;

import java.io.Serializable;

import org.lightj.dal.IData;

/**
 * session meta data interface
 * @author biyu
 *
 */
public interface ISessionMetaData extends IData {

	/**
	 * blob value
	 * @return
	 */
	public Serializable getBlobValue();
	
	/**
	 * set blob value
	 * @param blobValue
	 */
	public void setBlobValue(Serializable blobValue);
	
	/**
	 * metadata name
	 * @return
	 */
	public String getName();
	
	/**
	 * set metadata name
	 * @param name
	 */
	public void setName(String name);
	
	/**
	 * session id
	 * @return
	 */
	public long getFlowId();
	
	/**
	 * set session id
	 * @param sessionId
	 */
	public void setFlowId(long sessionId);
	
	/**
	 * get string value
	 * @return
	 */
	public String getStrValue();
	
	/**
	 * set string value
	 * @param strValue
	 */
	public void setStrValue(String strValue);
	
	/**
	 * metadata id
	 * @return
	 */
	public long getFlowMetaId();
	
	/**
	 * set metadata id
	 * @param sessionMetaId
	 */
	public void setFlowMetaId(long sessionMetaId);
	
	/**
	 * dirty flag
	 * @return
	 */
	public boolean isDirty();
	
	/**
	 * set dirty flag
	 * @param isDirty
	 */
	public void setDirty(boolean isDirty);
	
}
