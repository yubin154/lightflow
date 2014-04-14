package org.lightj.session;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * flow context property annotation 
 * @author binyu
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CtxProp {
	
	/** if this property is from user input */
	boolean isUserData() default false;
	
	/** sample data for user input data */
	String sampleUserDataValue() default "";
	
	/** when to save this property */
	CtxSaveType saveType()	default CtxSaveType.SaveOnChange;
	
	/** data type of the property */
	CtxDbType dbType()	default CtxDbType.VARCHAR;
	
	public static enum CtxDbType {
		VARCHAR, BLOB
	}
	
	public static enum CtxSaveType {
		SaveOnChange, AutoSave, NoSave
	}
}
