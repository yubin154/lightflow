package org.lightj.task;

import org.lightj.session.FlowContext;

/**
 * noop task, just return success (default) or predefined result
 * @author binyu
 *
 * @param <T>
 */
public class NoopTask<T extends FlowContext> extends ExecutableTask<T> 
{

	private TaskResult result;
	
	public NoopTask() {super();}
	public NoopTask(ExecuteOption option) { super(option); }
	
	@Override
	public TaskResult execute() {
		return result==null ? this.createTaskResult(TaskResultEnum.Success, null) : result;
	}

	public TaskResult getResult() {
		return result;
	}
	public void setResult(TaskResult result) {
		this.result = result;
	}
	
	public String toString() {
		return "noop exeutable task";
	}
}
