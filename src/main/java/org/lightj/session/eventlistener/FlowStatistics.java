package org.lightj.session.eventlistener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lightj.session.FlowSession.StepPropTuple;
import org.lightj.session.FlowStepProperties;
import org.lightj.util.StringUtil;

/**
 * keep and calculate flow progress
 * 
 * @author binyu
 *
 */
public class FlowStatistics {
	
	/** progress */
	private volatile int percentComplete = 0;
	/** step to progress map */
	private Map<String, Integer> step2ProgressMap = new HashMap<String, Integer>();
	
	/** constructor */
	public FlowStatistics(List<StepPropTuple> stepProps, String currentStep) {
		int total = 0;
		for (StepPropTuple stepProp : stepProps) {
			FlowStepProperties sp = stepProp.prop;
			total += (sp!=null ? Math.max(0, sp.stepWeight()) : 1);
			step2ProgressMap.put(stepProp.name, total);
		}
		if (total == 0) total = 1; // prevent DIV0
		for (Entry<String, Integer> entry : step2ProgressMap.entrySet()) {
			int w = entry.getValue();
			int p = Double.valueOf(Math.floor(((double)w/(double)total) * 100)).intValue();
			entry.setValue(p);
		}
		if (!StringUtil.isNullOrEmpty(currentStep)) {
			updatePercentComplete(currentStep);
		}
	}
	/** update progress from current step */
	public void updatePercentComplete(String step) {
		if (step2ProgressMap.containsKey(step)) {
			this.percentComplete = step2ProgressMap.get(step);
		}
	}
	/** update progress with absolute value */
	public void setPercentComplete(int percentComplete) {
		this.percentComplete = Math.max(0, Math.min(100, percentComplete));
	}
	/** get progress */
	public int getPercentComplete() {
		return percentComplete;
	}

}
