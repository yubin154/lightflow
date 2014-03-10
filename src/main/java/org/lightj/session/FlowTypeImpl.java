package org.lightj.session;

/**
 * flow type imple
 * 
 * @author binyu
 *
 */
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

	protected FlowTypeImpl(String value, String desc, Class<? extends FlowSession> klass,
			Class<? extends FlowContext> ctxKlass) {

		this.value = value;
		this.desc = desc;
		this.klass = klass;
		this.ctxKlass = ctxKlass;
	}
	
	public String getLabel() {
		return desc;
	}

	public String getValue() {
		return value;
	}

}
