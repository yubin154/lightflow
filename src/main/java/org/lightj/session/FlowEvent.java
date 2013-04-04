package org.lightj.session;

import org.lightj.ILabelledValue;

public enum FlowEvent implements ILabelledValue {
	/** session start */
	start("Session Start"),
	/** session paused */
	pause("Session Paused"),
	/** session resumed */
	resume("Session Resumed"),
	/** session recovered */
	recover("Session Recover"),
	/** session is not enabled for recover */
	recoverNot("Session Not Recover"),
	/** session recover failure */
	recoverFailure("Session Recover Failure"),
	/** session complete */
	stop("Session Stop"),
	/** build a step */
	stepBuild("Build Step"),
	/** enter in a step */
	stepEntry("Execute Step"),
	/** within a step */
	stepOngoing("Continue Step"),
	/** leaving a step */
	stepExit("Complete Step"),
	/** log an event */
	log("Log"),
	;

	private String label;
	FlowEvent(String label) {
		this.label = label;
	}
	public String getLabel() {
		return label;
	}
	public String getValue() {
		return name();
	}
}
