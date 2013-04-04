package org.lightj.session;



@SuppressWarnings("rawtypes")
public interface FlowType {

	/** flow type */
	public String value();
	/** flow desc */
	public String desc();
	/** flow class */
	public Class<? extends FlowSession> getFlowKlass();
	/** flow ctx class */
	public Class<? extends FlowContext> getCtxKlass();
	/** optional grouping for better concurrency see {@link QueueTaskGroup} */
	public String getTaskGroup();

}
