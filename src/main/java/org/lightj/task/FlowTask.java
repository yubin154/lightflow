package org.lightj.task;

import org.lightj.session.FlowEvent;
import org.lightj.session.FlowSession;
import org.lightj.session.FlowSessionFactory;
import org.lightj.session.IFlowEventListener;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepTransition;

/**
 * execute a flow session as a task
 * 
 * @author binyu
 *
 */
@SuppressWarnings("rawtypes")
public abstract class FlowTask extends ExecutableTask {

	private FlowSession subFlow;
	public FlowTask() {
		super();
	}
	public FlowTask(ExecuteOption execOption) {
		super(execOption);
	}

	public String toString() {
		return "Run subflow id=" + (subFlow!=null ? subFlow.getKey() : "not initialized");
	}
	
	public abstract FlowSession createSubFlow(); 

	@Override
	public TaskResult execute() {
		try {
			subFlow = createSubFlow();
			long parentFlowId = context.getSessionId();
			if (subFlow.getParentId() <= 0) {
				subFlow.setParentId(parentFlowId);
				FlowSessionFactory.getInstance().save(subFlow);
			}
			subFlow.addEventListener(new SubFlowEventListener(this));
			subFlow.runFlow();
			this.setExtTaskUuid(subFlow.getKey());
			return null;
		} 
		catch (Throwable t) {
			return this.failed(TaskResultEnum.Failed, t.getMessage(), t);
		}
	}


	/**
	 * inner flow event listener to call back parent flow on join
	 * @author biyu
	 *
	 */
	static class SubFlowEventListener implements IFlowEventListener {
		
		private FlowTask task;
		public SubFlowEventListener(FlowTask task) {
			this.task = task;
		}
		
		@Override
		public void handleStepEvent(FlowEvent event,
				FlowSession session, IFlowStep flowStep,
				StepTransition stepTransition) {
			// noop
		}

		@Override
		public void handleFlowEvent(FlowEvent event, FlowSession session, String msg) {
			if (event == FlowEvent.stop) {
				TaskResultEnum status = session.getResult().toTaskResult();
				task.reply(task.hasResult(status, session.getStatus()));
			}
		}

		@Override
		public void handleError(Throwable t, FlowSession session) {
			// noop
		}		
	}
}
