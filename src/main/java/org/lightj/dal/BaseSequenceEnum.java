/*
 * Created on Jan 31, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.dal;

import org.lightj.BaseTypeHolder;


/**
 * @author biyu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
@SuppressWarnings("serial")
public class BaseSequenceEnum extends BaseTypeHolder {
	
	public static final BaseSequenceEnum SEQ_FLOW_ID				= new BaseSequenceEnum("FLOW_ID_SEQ");
	public static final BaseSequenceEnum SEQ_FLOW_META_ID			= new BaseSequenceEnum("FLOW_META_ID_SEQ");
	public static final BaseSequenceEnum SEQ_FLOW_STEP_ID			= new BaseSequenceEnum("FLOW_STEP_ID_SEQ");
	
	protected BaseSequenceEnum(String name) {
		super(name);
	}

}
