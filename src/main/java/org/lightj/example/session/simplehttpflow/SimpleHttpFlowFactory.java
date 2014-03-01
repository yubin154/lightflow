package org.lightj.example.session.simplehttpflow;

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

@SuppressWarnings("rawtypes")
@Configuration
public class SimpleHttpFlowFactory {
	
	public @Bean @Scope("prototype") static SimpleHttpFlow SimpleHttpFlow() {
		return new SimpleHttpFlow();
	}

	@Bean 
	@Scope("prototype")
	public static IFlowStep handleErrorStep() {
		return new StepBuilder().parkInState(StepTransition.parkInState(FlowState.Completed, FlowResult.Failed, "something wrong")).getFlowStep();
	}
	
	@Bean 
	@Scope("prototype")
	public static IFlowStep runHttpTasksStep() {
		return new StepBuilder().executeAsyncTasks(new ExecutableTask() {

			@Override
			public TaskResult execute()
					throws TaskExecutionException {
				// do something useful here
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					throw new TaskExecutionException(e);
				}
				return this.createTaskResult(TaskResultEnum.Success, "task complete");
				
			}}).getFlowStep();
	}

}
