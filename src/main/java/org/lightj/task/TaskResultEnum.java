package org.lightj.task;

import org.lightj.util.StringUtil;

/**
 * task result status enum
 * @author biyu
 *
 */
public enum TaskResultEnum {
	
	Unknown(0), Running(1), Success(2), Failed(3), Timeout(3), Canceled(3)
	;
	
	private int severity;
	public int getSeverity() {
		return severity;
	}
	private TaskResultEnum(int severity) {
		this.severity = severity;
	}
	public boolean isComplete() {
		return !(this == Running || this == Unknown);
	}
	public boolean isSuccess() {
		return this == Success;
	}
	public boolean isTimeout() {
		return this == Timeout;
	}
	public boolean isCanceled() {
		return this == Canceled;
	}
	public boolean isFailed() {
		return this == Failed;
	}
	public boolean isAnyError() {
		return (this == Failed || this == Timeout || this == Canceled);
	}
	public static TaskResultEnum valueOfString(String v) {
		for (TaskResultEnum r : values()) {
			if (StringUtil.equalIgnoreCase(v, r.name())) {
				return r;
			}
		}
		return Unknown;
	}

}
