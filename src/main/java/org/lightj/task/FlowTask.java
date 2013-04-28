package org.lightj.task;

import static akka.pattern.Patterns.ask;

import org.lightj.session.FlowEvent;
import org.lightj.session.FlowSession;
import org.lightj.session.FlowSessionFactory;
import org.lightj.session.IFlowEventListener;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepTransition;
import org.lightj.task.WorkerMessage.CallbackType;
import org.lightj.util.StringUtil;

import akka.actor.ActorRef;

@SuppressWarnings("rawtypes")
public class FlowTask extends ExecutableTask {

	/**
	 * subflows to be run
	 */
	private final FlowSession subFlow;
	
	public FlowTask(FlowSession subFlow, ExecuteOption execOption) {
		super(execOption);
		this.subFlow = subFlow;
	}

	@Override
	public String getTaskDetail() {
		return "Run subflow id=" + subFlow.getKey();
	}

	@Override
	public TaskResult execute(ActorRef executingActor) {
		try {
			long parentFlowId = context.getSessionId();
			if (subFlow.getParentId() <= 0) {
				subFlow.setParentId(parentFlowId);
				FlowSessionFactory.getInstance().update(subFlow);
			}
			subFlow.addEventListener(new SubFlowEventListener(this, executingActor));
			subFlow.runFlow();
			this.setExtTaskUuid(subFlow.getKey());
			ask(executingActor, new WorkerMessage(CallbackType.submitted, this, null), 5000);
			return null;
		} 
		catch (Throwable t) {
			return this.createErrorResult(TaskResultEnum.Failed, t.getMessage(), StringUtil.getStackTrace(t));
		}
	}


	/**
	 * inner flow event listener to call back parent flow on join
	 * @author biyu
	 *
	 */
	static class SubFlowEventListener implements IFlowEventListener {
		
		private Task task;
		private ActorRef executingActor;
		public SubFlowEventListener(FlowTask task, ActorRef executingActor) {
			this.task = task;
			this.executingActor = executingActor;
		}
		
		@Override
		public void handleStepEvent(FlowEvent event,
				FlowSession session, IFlowStep flowStep,
				StepTransition stepTransition) {
			// noop
		}

		@Override
		public void handleFlowEvent(FlowEvent event, FlowSession session) {
			if (event == FlowEvent.stop) {
				TaskResultEnum status = session.getResult().toTaskResult();
				ask(executingActor, task.createTaskResult(status, session.getStatus()), 5000);
			}
		}

		@Override
		public void handleError(Throwable t, FlowSession session) {
			// noop
		}		
	}
}
