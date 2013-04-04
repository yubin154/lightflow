package org.lightj.session;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FlowDefinition {

	/** flow type */
	public String typeId();
	
	/** flow desc */
	public String desc()	default "";
	
	/** optional grouping for better concurrency see {@link QueueTaskGroup} */
	public String group()	default "UNKNOWN";
}
