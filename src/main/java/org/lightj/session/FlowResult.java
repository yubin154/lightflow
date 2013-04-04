package org.lightj.session;

import org.apache.log4j.Level;
import org.lightj.task.TaskResultEnum;

public enum FlowResult {
	Unknown,	// not started, no result, not applicable
	InProgress, // still running
	Success, 	// successful
	Fatal,		// fatal, internal error
	Failed, 	// failure
	Warning, 	// warning
	Timeout, 	// timeout
	Canceled, 	// canceled
	Crashed,	// crashed	
	;
	
	/**
	 * User friendly name
	 * Success, Failed, Timeout, InProgress, Paused, Skipped, Cancelled, Unknown
	 * @return
	 */
	public String getUserStatus() {
		switch(this) {
		case Success:
		case Warning:
			return "Success";
		case Fatal:
		case Failed:
		case Crashed:
			return "Failed";
		case Timeout:
			return "Timeout";
		case InProgress:
			return "InProgress";
		case Canceled:
			return "Canceled";
		default:
			return "Unknown";			
		}
	}
	
	/**
	 * convert to {@link TaskResultEnum}
	 * @return
	 */
	public TaskResultEnum toTaskResult() {
		switch(this) {
		case Success:
		case Warning:
			return TaskResultEnum.Success;
		case Fatal:
		case Failed:
		case Crashed:
			return TaskResultEnum.Failed;
		case Timeout:
			return TaskResultEnum.Timeout;
		case InProgress:
			return TaskResultEnum.Running;
		case Canceled:
			return TaskResultEnum.Canceled;
		default:
			return TaskResultEnum.Unknown;
		}
	}
	
	/**
	 * log level of the result status
	 * @return
	 */
	public Level logLevel() {
		switch(this) {
		case InProgress:
			return Level.DEBUG;
		case Success:
			return Level.INFO;
		case Warning:
		case Canceled:
		case Timeout:
			return Level.WARN;
		case Failed:
		case Crashed:
			return Level.ERROR;
		case Fatal:
			return Level.FATAL;
		default:
			return Level.OFF;			
		}
	}
}