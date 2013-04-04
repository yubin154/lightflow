package org.lightj.session;


@SuppressWarnings("rawtypes")
public class FlowTypeImpl implements FlowType {

	/** flow type int, unique */
	private final String value;

	/** flow description */
	private final String desc;

	/** flow class */
	private final Class<? extends FlowSession> klass;

	/** flow context class */
	private final Class<? extends FlowContext> ctxKlass;

	/** optional grouping for better concurrency */
	private final String taskGroup;

	
	public String value() {
		return value;
	}

	public String desc() {
		return desc;
	}

	public Class<? extends FlowSession> getFlowKlass() {
		return klass;
	}

	public Class<? extends FlowContext> getCtxKlass() {
		return ctxKlass;
	}

	public String getTaskGroup() {
		return taskGroup;
	}

	protected FlowTypeImpl(String value, String desc, Class<? extends FlowSession> klass,
			Class<? extends FlowContext> ctxKlass, String taskGroup) {

		this.value = value;
		this.desc = desc;
		this.klass = klass;
		this.ctxKlass = ctxKlass;
		this.taskGroup = taskGroup;
	}
	
	public String getLabel() {
		return desc;
	}

	public String getValue() {
		return value;
	}

}
