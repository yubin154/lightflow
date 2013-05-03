package org.lightj.example.session;

import org.lightj.example.session.SkeletonFlow.SkeletonSteps;
import org.lightj.session.FlowResult;
import org.lightj.session.FlowState;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.StepBuilder;
import org.lightj.session.step.StepTransition;
import org.lightj.task.ExecutableTask;
import org.lightj.task.TaskExecutionException;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import akka.actor.ActorRef;

@Configuration
public class SkeletonFlowFactory {
	
	public @Bean @Scope("prototype") static SkeletonFlow skeletonFlow() {
		return new SkeletonFlow();
	}

	@Bean(name="skeletonStartStep")
	@Scope("prototype")
	public static IFlowStep skeletonStartStep() {
		return new StepBuilder().runTo(SkeletonSteps.step1).getFlowStep();
	}
	
	@Bean 
	@Scope("prototype")
	public static IFlowStep skeletonStopStep() {
		return new StepBuilder().parkInState(StepTransition.parkInState(FlowState.Completed, FlowResult.Success, null)).getFlowStep();
	}

	@Bean 
	@Scope("prototype")
	public static IFlowStep skeletonErrorStep() {
		return new StepBuilder().parkInState(StepTransition.parkInState(FlowState.Completed, FlowResult.Failed, "something wrong")).getFlowStep();
	}
	
	@SuppressWarnings("rawtypes")
	@Bean 
	public static IFlowStep skeletonStep1() {
		return new StepBuilder().executeAsyncTasks(new ExecutableTask() {

			@Override
			public TaskResult execute(ActorRef exeuctingActor)
					throws TaskExecutionException {
				// do something useful here
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					throw new TaskExecutionException(e);
				}
				return this.createTaskResult(TaskResultEnum.Success, "task complete");
				
			}}).onSuccess(SkeletonSteps.stop).getFlowStep();
	}

}
