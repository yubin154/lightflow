package org.lightj.example.session;

import java.io.IOException;
import java.util.concurrent.Executors;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.lightj.example.dal.LocalDatabaseEnum;
import org.lightj.initialization.BaseModule;
import org.lightj.initialization.InitializationProcessor;
import org.lightj.session.FlowModule;
import org.lightj.session.FlowProperties;
import org.lightj.session.FlowSession;
import org.lightj.session.FlowSessionFactory;
import org.lightj.session.FlowStepProperties;
import org.lightj.session.exception.FlowSaveException;
import org.lightj.session.step.IFlowStep;
import org.lightj.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@FlowProperties(typeId="Skeleton", desc="Bare minimal of a flow", clustered=false, interruptible=false, timeoutInSec=0)
public class SkeletonFlow extends FlowSession<SkeletonFlowContext> {

	//////////////// step implementation /////////////////
	@Autowired(required=true)
	private IFlowStep skeletonStartStep;
	@Autowired(required=true)
	private IFlowStep skeletonStep1;
	@Autowired(required=true)
	private IFlowStep skeletonStopStep;
	@Autowired(required=true)
	private IFlowStep skeletonErrorStep;
	
	// method with the same name as in flow step enum, framework will use reflection to run each step
	@FlowStepProperties(stepWeight=2, logging=false, onSuccess="stop", onElse="handleError", onException="handleError", isFirstStep=true, stepIdx=1)
	public IFlowStep start() {
		return skeletonStartStep;
	}
	@FlowStepProperties(stepWeight=1, stepIdx=3)
	public IFlowStep stop() {
		return skeletonStopStep;
	}
	@FlowStepProperties(stepWeight=0, isErrorStep=true, stepIdx=100)
	public IFlowStep handleError() {
		return skeletonErrorStep;
	}
	@FlowStepProperties(stepWeight=5, stepIdx=2)
	public IFlowStep step1() {
		return skeletonStep1;
	}
	
	public static void main(String[] args) {
		// initialize flow framework
		AnnotationConfigApplicationContext flowCtx = new AnnotationConfigApplicationContext("org.lightj.example");
		
		InitializationProcessor initializer = new InitializationProcessor(
			new BaseModule[] {
				new FlowModule().setDb(LocalDatabaseEnum.TESTMONGO)
								.enableCluster()
								.setSpringContext(flowCtx)
								.setExectuorService(Executors.newFixedThreadPool(5))
								.getModule()
		});
		initializer.initialize();
		
		// create an instance of skeleton flow, fill in the flesh for each steps
		SkeletonFlow flow = FlowSessionFactory.getInstance().createSession(SkeletonFlow.class);
		
		// persist the flow
		try {
			flow.save();
		} catch (FlowSaveException e) {
			e.printStackTrace();
		}
		
		// kick off flow
		flow.runFlow();
		
		// checking flow state and print progress
		while (!flow.getState().isComplete()) {
			System.out.println(flow.getFlowInfo().getProgress());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// print complete flow info and flow execution logs
		try {
			System.out.println(JsonUtil.encode(flow.getFlowInfo()));
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
}
