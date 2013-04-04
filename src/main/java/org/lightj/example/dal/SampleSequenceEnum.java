/*
 * Created on Feb 2, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.example.dal;

import org.lightj.dal.BaseSequenceEnum;

/**
 * @author biyu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class SampleSequenceEnum extends BaseSequenceEnum {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8938731846212139331L;
	public static final SampleSequenceEnum TEST_DBFRAMEWORK = new SampleSequenceEnum("seq_test_dbframework");
	
	private SampleSequenceEnum(String name) {
		super(name);
	}

}
