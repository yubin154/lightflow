package org.lightj.example.session.simplehttpflow;

import java.util.Map;

import org.lightj.example.task.HttpTaskBuilder;
import org.lightj.session.FlowResult;
import org.lightj.session.FlowState;
import org.lightj.session.exception.FlowExecutionException;
import org.lightj.session.step.IFlowStep;
import org.lightj.session.step.SimpleStepExecution;
import org.lightj.session.step.StepBuilder;
import org.lightj.session.step.StepTransition;
import org.lightj.session.step.TaskFactoryStepExecution.IFlowContextTaskFactory;
import org.lightj.session.step.TaskFactoryStepExecution.TaskInFlow;
import org.lightj.task.ExecutableTask;
import org.lightj.task.ITaskEventHandler;
import org.lightj.task.SimpleTaskEventHandler;
import org.lightj.task.Task;
import org.lightj.task.TaskResult;
import org.lightj.task.TaskResultEnum;
import org.lightj.task.asynchttp.SimpleHttpResponse;
import org.lightj.util.StringUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

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
					ExecutableTask task = HttpTaskBuilder.buildTask(sessionContext.getCurrentRequest());
					sessionContext.setCurrentTask(task);
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
		
		return new StepBuilder().executeTasks(new IFlowContextTaskFactory<SimpleHttpFlowContext>(){

			@Override
			public TaskInFlow<SimpleHttpFlowContext> createTaskInFlow(
					SimpleHttpFlowContext context, int sequence) 
			{
				// task
				ExecutableTask task = context.getCurrentTask();
				
				// handler
				ITaskEventHandler<SimpleHttpFlowContext> myHandler = new SimpleTaskEventHandler<SimpleHttpFlowContext>() {
					@Override
					public void executeOnResult(SimpleHttpFlowContext ctx, Task task,
							TaskResult result) {
						System.out.println(StringUtil.trimToLength(result.<SimpleHttpResponse>getRawResult().toString(), 100));
					}
					@Override
					public TaskResultEnum executeOnCompleted(SimpleHttpFlowContext ctx,
							Map<String, TaskResult> results) {
						ctx.incTaskIndex();
						return super.executeOnCompleted(ctx, results);
					}
					
				};
				return new TaskInFlow<SimpleHttpFlowContext>(null, myHandler, task);
			}
			
		}).onSuccess("buildHttpTasks").getFlowStep();
	}

}
