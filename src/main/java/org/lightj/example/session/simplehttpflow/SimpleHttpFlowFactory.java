package org.lightj.example.session.simplehttpflow;

import org.lightj.session.FlowResult;
import org.lightj.session.FlowState;
import org.lightj.session.exception.FlowExecutionException;
import org.lightj.session.step.IAroundExecution;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.SimpleStepExecution;
import org.lightj.session.step.StepBuilder;
import org.lightj.session.step.StepTransition;
import org.lightj.task.ExecutableTask;
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
	public static IFlowStep buildHttpTasksStep() {
		return new StepBuilder().execute(new SimpleStepExecution<SimpleHttpFlowContext>() {
			
			@Override
			public StepTransition execute() throws FlowExecutionException {
				if (sessionContext.getCurrentRequest() != null) {
					ExecutableTask task = HttpTaskUtil.buildTask(sessionContext.getCurrentRequest());
					sessionContext.getCurrentTasks().clear();
					sessionContext.addCurrentTask(task);
					System.out.println("there");
					return StepTransition.runToStep("runHttpTasks");
				}
				else {
					return StepTransition.parkInState(FlowState.Completed, FlowResult.Success, "all done");
				}
			}

		}).getFlowStep();
		
	}
	
	@Bean 
	@Scope("prototype")
	public static IFlowStep runHttpTasksStep() {
		return new StepBuilder().executeTasksFromContext("currentTasks", null, new IAroundExecution<SimpleHttpFlowContext>(){

			@Override
			public void preExecute(SimpleHttpFlowContext ctx)
					throws FlowExecutionException {
				System.out.println("here");
			}

			@Override
			public void postExecute(SimpleHttpFlowContext ctx)
					throws FlowExecutionException {
				ctx.incTaskIndex();
			}
			
		}).onSuccess("buildHttpTasks").getFlowStep();
	}

}
