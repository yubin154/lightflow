package org.lightj.example.session;

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

@Configuration
public class SkeletonFlowFactory {
	
	public @Bean @Scope("prototype") static SkeletonFlow skeletonFlow() {
		return new SkeletonFlow();
	}

	@Bean(name="skeletonStartStep")
	@Scope("prototype")
	public static IFlowStep skeletonStartStep() {
		return new StepBuilder().getFlowStep();
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
	
	@Bean 
	public static IFlowStep skeletonStep1() {
		return new StepBuilder().executeTasks(new ExecutableTask() {

			@Override
			public TaskResult execute()
					throws TaskExecutionException {
				// do something useful here
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					throw new TaskExecutionException(e);
				}
				return this.hasResult(TaskResultEnum.Success, "task complete");
				
			}}).getFlowStep();
	}

}
