package org.lightj.session;

import org.lightj.ILabelledValue;

public enum FlowState implements ILabelledValue {
	
	Unknown("Unknown"),			// system state unknown
	Pending("Initializing"), 	// initializing
	Running("Running"), 		// running
	Callback("Running"),		// waiting for call back
	Paused("Paused"), 			// paused
	Completed("Completed"), 	// completed
	Canceled("Canceled"),		// canceled
	Crashed("Crashed"),			// crashed
	;
	
	private String label;
	FlowState(String label) {this.label = label;}
	public boolean isComplete() {
		return (this == Completed || this == Canceled || this == Crashed);
	}
	public boolean isWaiting() {
		return (!isComplete() && !isRunning());
	}
	public boolean isRunning() {
		return (this == Running || this == Callback);
	}
	public String getLabel() { return this.label; }
	public String getValue() { return this.name(); }

	public static FlowState valueOfIgnoreCase(String value) {
		for(FlowState s : FlowState.values()) {
			if(s.name().equalsIgnoreCase(value)) {
				return s;
			}
		}
		return null;
	}
}