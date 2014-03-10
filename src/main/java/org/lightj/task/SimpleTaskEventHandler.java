package org.lightj.task;

import java.util.Map;

import org.lightj.session.FlowContext;
import org.lightj.session.step.StepTransition;

/**
 * simple handler, does nothing
 * @author binyu
 *
 * @param <T>
 */
@SuppressWarnings("rawtypes")
public class SimpleTaskEventHandler<T extends FlowContext> implements ITaskEventHandler<T> {

	@Override
	public void executeOnCreated(T ctx, Task task) {
	}

	@Override
	public void executeOnSubmitted(T ctx, Task task) {
	}

	@Override
	public void executeOnResult(T ctx, Task task, TaskResult result) {
	}

	@Override
	public StepTransition executeOnCompleted(T ctx,
			Map<String, TaskResult> results) {
		return null;
	}

}
