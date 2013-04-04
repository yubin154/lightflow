package org.lightj.session.step;

import java.util.Date;

public class StepStatistics {
	
	private Date startTime;
	private Date endTime;
	
	public StepStatistics() {
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

}
