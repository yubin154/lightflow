package org.lightj.session.step;

import java.util.Collection;

import org.lightj.session.FlowContext;
import org.lightj.session.FlowExecutionException;
import org.lightj.task.FlowTask;

/**
 * a flow step to run sub flows
 * 
 * @author biyu
 * 
 */
public class FlowStepExecution<T extends FlowContext> extends SimpleStepExecution<T> {
	
	private final Collection<FlowTask> tasks;
	
	/**
	 * constructor
	 */
	public FlowStepExecution(StepTransition tran, Collection<FlowTask> tasks) {
		super(tran);
		this.tasks = tasks;
	}

	/**
	 * submit the task to be run by task runner
	 */
	@Override
	public StepTransition execute() throws FlowExecutionException {
		long parentFlowId = this.sessionContext.getSessionId();
		for (FlowTask task : tasks) {
			task.setParentFlowId(parentFlowId);
		}
		return super.execute();
	}

}
