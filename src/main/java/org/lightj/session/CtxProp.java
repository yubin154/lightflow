package org.lightj.session;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CtxProp {
	
	CtxSaveType saveType()	default CtxSaveType.SaveOnChange;
	
	CtxDbType dbType()	default CtxDbType.VARCHAR;
	
	public static enum CtxDbType {
		VARCHAR, BLOB
	}
	
	public static enum CtxSaveType {
		SaveOnChange, AutoSave, NoSave
	}
}
